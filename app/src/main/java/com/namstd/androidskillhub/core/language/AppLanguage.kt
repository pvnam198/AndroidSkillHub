package com.namstd.androidskillhub.core.language

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    VIETNAMESE("vi", "Tiếng Việt"),
    ;

    companion object {
        val default = ENGLISH

        fun fromCode(code: String): AppLanguage? = entries.find { it.code == code }
    }
}
