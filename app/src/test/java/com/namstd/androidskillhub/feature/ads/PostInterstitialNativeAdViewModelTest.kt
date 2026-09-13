package com.namstd.androidskillhub.feature.ads

import com.namstd.androidskillhub.feature.ads.PostInterstitialNativeAdViewModel.UiState
import org.junit.Assert.assertEquals
import org.junit.Test

class PostInterstitialNativeAdViewModelTest {

    @Test
    fun startsInProgressAtZeroPercent() {
        val viewModel = PostInterstitialNativeAdViewModel()

        assertEquals(UiState.Progress(0), viewModel.state.value)
    }

    @Test
    fun progressTickUpdatesPercent() {
        val viewModel = PostInterstitialNativeAdViewModel()

        viewModel.onProgressTick(42)

        assertEquals(UiState.Progress(42), viewModel.state.value)
    }

    @Test
    fun progressFinishedMovesToAwaitingNext() {
        val viewModel = PostInterstitialNativeAdViewModel()

        viewModel.onProgressTick(80)
        viewModel.onProgressFinished()

        assertEquals(UiState.AwaitingNext, viewModel.state.value)
    }

    @Test
    fun progressTickAfterAwaitingNextIsNoOp() {
        val viewModel = PostInterstitialNativeAdViewModel()
        viewModel.onProgressFinished()

        viewModel.onProgressTick(10)

        assertEquals(UiState.AwaitingNext, viewModel.state.value)
    }

    @Test
    fun countdownStartedMovesToCountdown() {
        val viewModel = PostInterstitialNativeAdViewModel()
        viewModel.onProgressFinished()

        viewModel.onCountdownStarted(5)

        assertEquals(UiState.Countdown(5), viewModel.state.value)
    }

    @Test
    fun countdownTickUpdatesSecondsLeft() {
        val viewModel = PostInterstitialNativeAdViewModel()
        viewModel.onCountdownStarted(5)

        viewModel.onCountdownTick(3)

        assertEquals(UiState.Countdown(3), viewModel.state.value)
    }

    @Test
    fun countdownTickBeforeCountdownStartedIsNoOp() {
        val viewModel = PostInterstitialNativeAdViewModel()

        viewModel.onCountdownTick(3)

        assertEquals(UiState.Progress(0), viewModel.state.value)
    }

    @Test
    fun countdownFinishedMovesToAwaitingClose() {
        val viewModel = PostInterstitialNativeAdViewModel()
        viewModel.onCountdownStarted(5)

        viewModel.onCountdownFinished()

        assertEquals(UiState.AwaitingClose, viewModel.state.value)
    }

    @Test
    fun countdownFinishedBeforeCountdownStartedIsNoOp() {
        val viewModel = PostInterstitialNativeAdViewModel()

        viewModel.onCountdownFinished()

        assertEquals(UiState.Progress(0), viewModel.state.value)
    }

    @Test
    fun countdownTickAfterAwaitingCloseIsNoOp() {
        val viewModel = PostInterstitialNativeAdViewModel()
        viewModel.onCountdownStarted(5)
        viewModel.onCountdownFinished()

        viewModel.onCountdownTick(1)

        assertEquals(UiState.AwaitingClose, viewModel.state.value)
    }
}
