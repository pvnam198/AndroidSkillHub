package com.namstd.androidskillhub.feature.ads

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PostInterstitialNativeAdViewModel : ViewModel() {

    sealed interface UiState {
        data class Progress(val percent: Int) : UiState
        data object AwaitingNext : UiState
        data class Countdown(val secondsLeft: Int) : UiState
        data object AwaitingClose : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Progress(percent = 0))
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onProgressTick(percent: Int) {
        if (_state.value is UiState.Progress) _state.value = UiState.Progress(percent)
    }

    fun onProgressFinished() {
        if (_state.value is UiState.Progress) _state.value = UiState.AwaitingNext
    }

    fun onCountdownStarted(secondsLeft: Int) {
        _state.value = UiState.Countdown(secondsLeft)
    }

    fun onCountdownTick(secondsLeft: Int) {
        if (_state.value is UiState.Countdown) _state.value = UiState.Countdown(secondsLeft)
    }

    fun onCountdownFinished() {
        if (_state.value is UiState.Countdown) _state.value = UiState.AwaitingClose
    }
}
