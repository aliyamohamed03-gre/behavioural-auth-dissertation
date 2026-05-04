package uk.ac.gre.behaviouralauth.capture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.sqrt

private const val MIN_SWIPE_DISTANCE_PX = 5f


fun Modifier.captureSwipes(onSwipe: (SwipeEvent) -> Unit): Modifier {
    return pointerInput(onSwipe) {
        awaitEachGesture {
            //starts tracking when the user first touches the screen.
            val down = awaitFirstDown(requireUnconsumed = false)
            val startPosition = down.position
            val startTimestamp = down.uptimeMillis

            var endPosition = startPosition
            var endTimestamp = startTimestamp
            var movedEnough = false

            while (true) {
                val event = awaitPointerEvent()
                val trackedPointer = event.changes.firstOrNull { it.id == down.id } ?: break

                endPosition = trackedPointer.position
                endTimestamp = trackedPointer.uptimeMillis

                //Marks the gesture as a swipe only after it passes the minimum distance.
                if (distanceBetween(startPosition, endPosition) > MIN_SWIPE_DISTANCE_PX) {
                    movedEnough = true
                }

                if (!trackedPointer.pressed) {
                    //Emits the swipe once the finger is lifted.
                    if (movedEnough) {
                        onSwipe(
                            SwipeEvent(
                                startTimestampMs = startTimestamp,
                                endTimestampMs = endTimestamp,
                                startX = startPosition.x,
                                startY = startPosition.y,
                                endX = endPosition.x,
                                endY = endPosition.y
                            )
                        )
                    }
                    break
                }
            }
        }
    }
}

private fun distanceBetween(start: Offset, end: Offset): Float {
    //Calculates straight-line movement between two screen positions.
    val dx = end.x - start.x
    val dy = end.y - start.y
    return sqrt(dx * dx + dy * dy)
}