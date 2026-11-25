package com.gymable.ui.tutorial

import androidx.lifecycle.ViewModel
import com.gymable.data.repository.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

sealed interface TutorialState {
    object Hidden : TutorialState
    data class Step(val index: Int) : TutorialState
    object Final : TutorialState
}

private const val TOTAL_STEPS = 6

@HiltViewModel
class TutorialViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {

    private val _state = MutableStateFlow<TutorialState>(TutorialState.Hidden)
    val state: StateFlow<TutorialState> = _state

    init {
        if (!onboardingRepository.isComplete) {
            _state.value = TutorialState.Step(0)
        }
    }

    fun advance() {
        when (val current = _state.value) {
            is TutorialState.Step -> {
                if (current.index < TOTAL_STEPS - 1) {
                    _state.value = TutorialState.Step(current.index + 1)
                } else {
                    _state.value = TutorialState.Final
                }
            }
            TutorialState.Final -> {
                onboardingRepository.markComplete()
                _state.value = TutorialState.Hidden
            }
            TutorialState.Hidden -> Unit
        }
    }

    fun skip() {
        onboardingRepository.markComplete()
        _state.value = TutorialState.Hidden
    }
}
