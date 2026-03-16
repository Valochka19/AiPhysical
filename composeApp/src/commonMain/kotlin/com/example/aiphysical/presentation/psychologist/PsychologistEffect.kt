package com.example.aiphysical.presentation.psychologist

sealed class PsychologistEffect {
    data class ShowSnackbar(val message: String) : PsychologistEffect()
    object TriggerHaptic : PsychologistEffect()
    data class OpenUrl(val url: String) : PsychologistEffect()
}
