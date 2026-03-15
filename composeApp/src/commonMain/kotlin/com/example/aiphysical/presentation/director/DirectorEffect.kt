package com.example.aiphysical.presentation.director

sealed class DirectorEffect {
    /** Copy text to clipboard and show a snackbar message */
    data class CopyToClipboard(val text: String, val message: String) : DirectorEffect()
    /** Open a URL via UriHandler */
    data class OpenUrl(val url: String) : DirectorEffect()
    /** Show a plain snackbar */
    data class ShowSnackbar(val message: String) : DirectorEffect()
    /** Trigger haptic feedback */
    object TriggerHaptic : DirectorEffect()
}

