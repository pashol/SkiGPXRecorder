package com.skigpxrecorder.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skigpxrecorder.R
import com.skigpxrecorder.ui.theme.RedError
import kotlinx.coroutines.delay

/**
 * Button that requires holding for 4 seconds to confirm stop action
 */
@Composable
fun HoldToStopButton(
    onStopConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    holdDurationMs: Long = 4000L
) {
    var isHolding by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "holdProgress"
    )

    LaunchedEffect(isHolding) {
        if (isHolding) {
            val startTime = System.currentTimeMillis()
            while (isHolding) {
                val elapsed = System.currentTimeMillis() - startTime
                progress = (elapsed.toFloat() / holdDurationMs).coerceIn(0f, 1f)

                if (progress >= 1f) {
                    onStopConfirmed()
                    isHolding = false
                    progress = 0f
                    break
                }

                delay(16) // ~60 fps update
            }
        } else {
            progress = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isHolding = true
                            tryAwaitRelease()
                            isHolding = false
                        }
                    )
                },
            shape = ButtonDefaults.shape,
            color = RedError,
            contentColor = MaterialTheme.colorScheme.onError
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = if (isHolding) {
                        stringResource(R.string.hold_to_stop)
                    } else {
                        stringResource(R.string.stop_recording)
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        if (isHolding && animatedProgress > 0f) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                color = MaterialTheme.colorScheme.onError,
            )
        }
    }
}
