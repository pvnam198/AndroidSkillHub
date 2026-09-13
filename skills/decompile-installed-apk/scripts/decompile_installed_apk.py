#!/usr/bin/env python3
"""Lấy và dịch ngược toàn bộ split APK của một package Android đã cài."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import shlex
import shutil
import subprocess
import sys
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath
from typing import Sequence


PACKAGE_ID_RE = re.compile(
    r"^[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+$"
)
INSTALL_HELP = {
    "adb": "Cài Android SDK Platform Tools: https://developer.android.com/tools/releases/platform-tools",
    "jadx": "Cài JADX: https://github.com/skylot/jadx/releases",
    "apktool": "Cài Apktool: https://apktool.org/docs/install/",
}


class WorkflowError(RuntimeError):
    """Lỗi dự kiến của quy trình, kèm thông báo có thể xử lý."""


@dataclass(frozen=True)
class Tooling:
    adb: str
    jadx: str
    apktool: str | None


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Lấy toàn bộ split APK của package đã cài và dịch ngược chúng.",
        add_help=False,
    )
    parser._positionals.title = "tham số bắt buộc"
    parser._optionals.title = "tùy chọn"
    parser.add_argument("-h", "--help", action="help", help="Hiển thị trợ giúp này rồi thoát.")
    parser.add_argument("package_id", help="Package ID Android, ví dụ com.example.app")
    parser.add_argument(
        "--smali",
        action="store_true",
        help="Đồng thời dịch từng APK sang smali bằng Apktool (bỏ qua tài nguyên).",
    )
    parser.add_argument("--serial", help="Serial thiết bị ADB; bắt buộc khi có nhiều thiết bị khả dụng.")
    parser.add_argument(
        "--output-root",
        type=Path,
        default=Path("apk-decompile-runs"),
        help="Thư mục gốc cho các phiên chạy có dấu thời gian (mặc định: ./apk-decompile-runs).",
    )
    return parser.parse_args(argv)


def validate_package_id(package_id: str) -> None:
    if not PACKAGE_ID_RE.fullmatch(package_id):
        raise WorkflowError(
            "Package ID Android không hợp lệ. Cần ít nhất hai phần ngăn cách bằng dấu chấm; "
            "mỗi phần phải bắt đầu bằng chữ cái và chỉ chứa chữ cái, chữ số hoặc dấu gạch dưới."
        )


def discover_tools(want_smali: bool) -> Tooling:
    required = ["adb", "jadx"] + (["apktool"] if want_smali else [])
    resolved: dict[str, str] = {}
    missing: list[str] = []
    for command in required:
        path = shutil.which(command)
        if path:
            resolved[command] = path
        else:
            missing.append(command)
    if missing:
        guidance = "\n".join(f"- {name}: {INSTALL_HELP[name]}" for name in missing)
        raise WorkflowError(f"Thiếu lệnh bắt buộc: {', '.join(missing)}\n{guidance}")
    return Tooling(
        adb=resolved["adb"],
        jadx=resolved["jadx"],
        apktool=resolved.get("apktool"),
    )


def run_command(args: Sequence[str]) -> subprocess.CompletedProcess[str]:
    try:
        return subprocess.run(
            list(args),
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=False,
        )
    except OSError as exc:
        raise WorkflowError(f"Không thể chạy {args[0]}: {exc}") from exc


def tool_version(command: str, args: Sequence[str]) -> str:
    result = run_command([command, *args])
    combined = [line.strip() for line in (result.stdout + result.stderr).splitlines() if line.strip()]
    if result.returncode != 0:
        raise WorkflowError(
            f"Công cụ phụ thuộc đã tồn tại nhưng không thể chạy: {command} ({result.returncode})"
            + (f"\n{combined[-1]}" if combined else "")
        )
    return combined[0] if combined else "không xác định"


def collect_tool_versions(tools: Tooling) -> dict[str, str | None]:
    return {
        "adb": tool_version(tools.adb, ["version"]),
        "jadx": tool_version(tools.jadx, ["--version"]),
        "apktool": tool_version(tools.apktool, ["--version"]) if tools.apktool else None,
    }


def parse_adb_devices(output: str) -> dict[str, str]:
    devices: dict[str, str] = {}
    for raw_line in output.splitlines():
        line = raw_line.strip()
        if not line or line.startswith("List of devices attached") or line.startswith("*"):
            continue
        fields = line.split()
        if len(fields) >= 2:
            devices[fields[0]] = fields[1]
    return devices


def select_device(adb: str, requested_serial: str | None) -> str:
    result = run_command([adb, "devices", "-l"])
    if result.returncode != 0:
        detail = (result.stderr or result.stdout).strip()
        raise WorkflowError(f"Không thể liệt kê thiết bị ADB: {detail or 'lỗi ADB không xác định'}")
    devices = parse_adb_devices(result.stdout)
    if requested_serial:
        state = devices.get(requested_serial)
        if state is None:
            raise WorkflowError(f"Không tìm thấy thiết bị ADB '{requested_serial}'.")
        if state != "device":
            raise WorkflowError(f"Thiết bị ADB '{requested_serial}' đang ở trạng thái {state}, chưa sẵn sàng.")
        return requested_serial

    usable = sorted(serial for serial, state in devices.items() if state == "device")
    if len(usable) == 1:
        return usable[0]
    if len(usable) > 1:
        joined = ", ".join(usable)
        raise WorkflowError(f"Tìm thấy nhiều thiết bị ADB khả dụng: {joined}. Hãy chạy lại với --serial.")
    if devices:
        states = ", ".join(f"{serial} ({state})" for serial, state in sorted(devices.items()))
        raise WorkflowError(f"Không có thiết bị ADB trực tuyến đã được cấp quyền. Đã phát hiện: {states}")
    raise WorkflowError("Không tìm thấy thiết bị ADB. Hãy kết nối thiết bị và bật Gỡ lỗi USB.")


def package_paths(adb: str, serial: str, package_id: str) -> list[str]:
    result = run_command([adb, "-s", serial, "shell", "pm", "path", package_id])
    if result.returncode != 0:
        detail = (result.stderr or result.stdout).strip()
        raise WorkflowError(f"Không thể truy vấn package '{package_id}': {detail or 'lỗi ADB không xác định'}")
    paths = [
        line.strip()[len("package:") :]
        for line in result.stdout.splitlines()
        if line.strip().startswith("package:") and line.strip()[len("package:") :]
    ]
    if not paths:
        raise WorkflowError(f"Package '{package_id}' chưa được cài hoặc không thể truy cập đường dẫn APK.")
    indexed = list(enumerate(dict.fromkeys(paths)))
    indexed.sort(key=lambda item: (PurePosixPath(item[1]).name != "base.apk", item[0]))
    return [path for _, path in indexed]


def create_run_dir(output_root: Path, package_id: str) -> Path:
    timestamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    base = output_root.expanduser().resolve() / package_id / timestamp
    candidate = base
    suffix = 1
    while candidate.exists():
        candidate = base.with_name(f"{base.name}-{suffix:02d}")
        suffix += 1
    candidate.mkdir(parents=True)
    for child in ("apks", "logs"):
        (candidate / child).mkdir()
    return candidate


def safe_apk_name(index: int, remote_path: str) -> str:
    basename = PurePosixPath(remote_path).name or "package.apk"
    basename = re.sub(r"[^A-Za-z0-9._-]", "_", basename)
    if not basename.lower().endswith(".apk"):
        basename += ".apk"
    return f"{index:03d}-{basename}"


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def write_log(path: Path, args: Sequence[str], result: subprocess.CompletedProcess[str]) -> None:
    text = (
        f"$ {shlex.join(str(arg) for arg in args)}\n"
        f"exit_code: {result.returncode}\n\n"
        f"[stdout]\n{result.stdout}\n"
        f"[stderr]\n{result.stderr}\n"
    )
    path.write_text(text, encoding="utf-8")


def stage_result(result: subprocess.CompletedProcess[str], log: Path) -> dict[str, object]:
    return {
        "status": "success" if result.returncode == 0 else "failed",
        "exit_code": result.returncode,
        "log": str(log),
    }


def write_reports(run_dir: Path, report: dict[str, object]) -> None:
    report_path = run_dir / "report.json"
    report_path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    apk_files = report.get("apk_files", [])
    stages = report.get("stages", {})
    status_labels = {
        "failed": "thất bại",
        "not_requested": "không được yêu cầu",
        "partial": "hoàn thành một phần",
        "pending": "đang chờ",
        "running": "đang chạy",
        "skipped": "đã bỏ qua",
        "success": "thành công",
        "unknown": "không xác định",
    }
    stage_labels = {"pull": "Lấy APK", "jadx": "JADX", "smali": "Smali"}
    report_status = str(report["status"])
    lines = [
        f"# Báo cáo dịch ngược APK: {report['package_id']}",
        "",
        f"- Trạng thái: **{status_labels.get(report_status, report_status)}**",
        f"- Thời điểm tạo: `{report['created_at']}`",
        f"- Thiết bị: `{report['device_serial']}`",
        f"- Số tệp APK đã lấy: **{len(apk_files) if isinstance(apk_files, list) else 0}**",
        f"- Thư mục phiên chạy: `{run_dir}`",
        "",
        "## Các giai đoạn",
        "",
    ]
    if isinstance(stages, dict):
        for name, details in stages.items():
            status = details.get("status", "unknown") if isinstance(details, dict) else "unknown"
            lines.append(
                f"- {stage_labels.get(name, name)}: **{status_labels.get(str(status), status)}**"
            )
    errors = report.get("errors", [])
    if isinstance(errors, list) and errors:
        lines.extend(["", "## Lỗi", ""])
        lines.extend(f"- {error}" for error in errors)
    warnings = report.get("warnings", [])
    if isinstance(warnings, list) and warnings:
        lines.extend(["", "## Cảnh báo", ""])
        lines.extend(f"- {warning}" for warning in warnings)
    lines.extend(
        [
            "",
            "> Mã Java dịch ngược chỉ là bản xấp xỉ từ bytecode và có thể chứa phương thức chưa đầy đủ hoặc không chính xác.",
            "",
        ]
    )
    (run_dir / "REPORT.md").write_text("\n".join(lines), encoding="utf-8")


def execute(
    args: argparse.Namespace,
    tools: Tooling,
    versions: dict[str, str | None],
    serial: str,
    remote_paths: list[str],
) -> tuple[Path, bool]:
    run_dir = create_run_dir(args.output_root, args.package_id)
    created_at = datetime.now(timezone.utc).isoformat()
    report: dict[str, object] = {
        "schema_version": 1,
        "package_id": args.package_id,
        "created_at": created_at,
        "status": "running",
        "device_serial": serial,
        "tools": versions,
        "apk_files": [],
        "stages": {
            "pull": {"status": "running"},
            "jadx": {"status": "pending"},
            "smali": {"status": "pending" if args.smali else "not_requested"},
        },
        "errors": [],
        "warnings": [],
    }
    write_reports(run_dir, report)

    local_apks: list[Path] = []
    pull_failed = False
    for index, remote_path in enumerate(remote_paths):
        local_path = run_dir / "apks" / safe_apk_name(index, remote_path)
        command = [tools.adb, "-s", serial, "pull", remote_path, str(local_path)]
        result = run_command(command)
        log_path = run_dir / "logs" / f"pull-{index:03d}.log"
        write_log(log_path, command, result)
        entry: dict[str, object] = {
            "remote_path": remote_path,
            "local_path": str(local_path),
            "pull_exit_code": result.returncode,
            "log": str(log_path),
        }
        if result.returncode == 0 and local_path.is_file():
            entry.update({"size": local_path.stat().st_size, "sha256": sha256_file(local_path)})
            local_apks.append(local_path)
        else:
            pull_failed = True
            error = f"Không thể lấy {remote_path}; xem log tại {log_path}"
            report["errors"].append(error)  # type: ignore[union-attr]
        report["apk_files"].append(entry)  # type: ignore[union-attr]

    stages = report["stages"]
    assert isinstance(stages, dict)
    stages["pull"] = {"status": "failed" if pull_failed else "success"}
    if pull_failed:
        stages["jadx"] = {"status": "skipped"}
        if args.smali:
            stages["smali"] = {"status": "skipped"}
        report["status"] = "failed"
        write_reports(run_dir, report)
        return run_dir, False

    jadx_dir = run_dir / "jadx"
    jadx_command = [tools.jadx, "-d", str(jadx_dir), *(str(path) for path in local_apks)]
    jadx_result = run_command(jadx_command)
    jadx_log = run_dir / "logs" / "jadx.log"
    write_log(jadx_log, jadx_command, jadx_result)
    stages["jadx"] = stage_result(jadx_result, jadx_log)
    if jadx_result.returncode != 0:
        report["errors"].append(f"JADX thất bại; xem log tại {jadx_log}")  # type: ignore[union-attr]
    elif jadx_result.stderr.strip():
        report["warnings"].append(f"JADX có thông báo chẩn đoán; xem log tại {jadx_log}")  # type: ignore[union-attr]

    smali_failed = False
    if args.smali:
        smali_dir = run_dir / "smali"
        smali_dir.mkdir()
        apktool_results: list[dict[str, object]] = []
        assert tools.apktool is not None
        for index, apk_path in enumerate(local_apks):
            output_dir = smali_dir / apk_path.stem
            command = [tools.apktool, "d", "--no-res", "-o", str(output_dir), str(apk_path)]
            result = run_command(command)
            log_path = run_dir / "logs" / f"apktool-{index:03d}.log"
            write_log(log_path, command, result)
            item = stage_result(result, log_path)
            item["apk"] = str(apk_path)
            item["output"] = str(output_dir)
            apktool_results.append(item)
            if result.returncode != 0:
                smali_failed = True
                report["errors"].append(f"Apktool thất bại với {apk_path.name}; xem log tại {log_path}")  # type: ignore[union-attr]
            elif result.stderr.strip():
                report["warnings"].append(  # type: ignore[union-attr]
                    f"Apktool có thông báo chẩn đoán cho {apk_path.name}; xem log tại {log_path}"
                )
        stages["smali"] = {
            "status": "failed" if smali_failed else "success",
            "apk_results": apktool_results,
        }

    succeeded = jadx_result.returncode == 0 and not smali_failed
    report["status"] = "success" if succeeded else "partial"
    write_reports(run_dir, report)
    return run_dir, succeeded


def main(argv: Sequence[str] | None = None) -> int:
    args = parse_args(argv)
    try:
        validate_package_id(args.package_id)
        tools = discover_tools(args.smali)
        versions = collect_tool_versions(tools)
        serial = select_device(tools.adb, args.serial)
        remote_paths = package_paths(tools.adb, serial, args.package_id)
        run_dir, succeeded = execute(args, tools, versions, serial, remote_paths)
    except WorkflowError as exc:
        print(f"lỗi: {exc}", file=sys.stderr)
        return 2

    print(run_dir)
    print(run_dir / "REPORT.md")
    return 0 if succeeded else 1


if __name__ == "__main__":
    raise SystemExit(main())
