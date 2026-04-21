package uk.ac.gre.behaviouralauth.capture

import android.text.Editable
import android.text.TextWatcher

/**
 * Small helper that turns text-length changes into behavioural keystroke events.
 *
 * Two entry points are provided so the same event model can be used from both
 * classic Android Views and Jetpack Compose text fields.
 */
class KeystrokeCaptureSource {
    fun createTextWatcher(onEvent: (KeystrokeEvent) -> Unit): TextWatcher {
        return object : TextWatcher {
            private var previousLength: Int = 0

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                previousLength = s?.length ?: 0
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) = Unit

            override fun afterTextChanged(s: Editable?) {
                val newLength = s?.length ?: 0
                emitLengthChange(
                    previousLength = previousLength,
                    newLength = newLength,
                    onEvent = onEvent
                )
                previousLength = newLength
            }
        }
    }

    /**
     * Compose TextField uses onValueChange rather than TextWatcher, so we expose
     * the same conversion logic in plain function form.
     */
    fun handleComposeTextChange(
        previousText: String,
        newText: String,
        onEvent: (KeystrokeEvent) -> Unit
    ) {
        emitLengthChange(
            previousLength = previousText.length,
            newLength = newText.length,
            onEvent = onEvent
        )
    }

    private fun emitLengthChange(
        previousLength: Int,
        newLength: Int,
        onEvent: (KeystrokeEvent) -> Unit
    ) {
        if (previousLength == newLength) {
            return
        }

        onEvent(
            KeystrokeEvent(
                timestampMs = System.currentTimeMillis(),
                previousLength = previousLength,
                newLength = newLength,
                wasDeletion = newLength < previousLength
            )
        )
    }
}
