package com.example.presentation.clipboard_list

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeRevealBox(
    modifier: Modifier = Modifier,
    maxRevealWidth: Dp = 200.dp,
    backgroundContent: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val maxRevealPx = with(LocalDensity.current) { maxRevealWidth.toPx() }
    val offsetX = remember { Animatable(0f) }

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    scope.launch {
                        if (offsetX.value > maxRevealPx / 2) {
                            offsetX.animateTo(maxRevealPx, tween(200))
                        } else if (offsetX.value < -maxRevealPx / 2) {
                            offsetX.animateTo(-maxRevealPx, tween(200))
                        } else {
                            offsetX.animateTo(0f, tween(200))
                        }
                    }
                },
                onDragCancel = {
                    scope.launch {
                        offsetX.animateTo(0f, tween(200))
                    }
                },
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    scope.launch {
                        val newOffset = (offsetX.value + dragAmount).coerceIn(-maxRevealPx, maxRevealPx)
                        offsetX.snapTo(newOffset)
                    }
                }
            )
        }
    ) {
        Box(modifier = Modifier.matchParentSize(), content = backgroundContent)
        Box(
            modifier = Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) },
            content = content
        )
    }
}
