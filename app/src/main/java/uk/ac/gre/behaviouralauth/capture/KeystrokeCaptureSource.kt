package uk.ac.gre.behaviouralauth.capture

import android.text.Editable
import android.text.TextWatcher


class KeystrokeCaptureSource {
    fun createTextWatcher(onEvent: (KeystrokeEvent) -> Unit): TextWatcher {
        return object : TextWatcher {
            //Stores the text length before Android applies the latest edit.
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

                //Turns the text length change into a keystroke event.
                emitLengthChange(
                    previousLength = previousLength,
                    newLength = newLength,
                    onEvent = onEvent
                )

                previousLength = newLength
            }
        }
    }


    fun handleComposeTextChange(
        previousText: String,
        newText: String,
        onEvent: (KeystrokeEvent) -> Unit
    ) {
        //Provides the same capture logic for Jetpack Compose text fields.
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
        //Ignores edits that do not change the text length.
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