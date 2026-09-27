package com.example.alphabetlauncher.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.exp

@Composable
fun AlphabetBar(
    modifier: Modifier = Modifier,
    availableLetters: Set<Char>,
    onLetterSelected: (Char) -> Unit,
    onInteractionStart: () -> Unit,
    onInteractionEnd: () -> Unit
) {
    val alphabet = ('A'..'Z').toList()
    var barHeight by remember { mutableFloatStateOf(0f) }
    var columnTop by remember { mutableFloatStateOf(0f) }
    var columnHeight by remember { mutableFloatStateOf(1f) }
    
    var dragY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // Animate the bend factor separately from the live drag to ensure 60fps
    val bendAmount by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bend_amount"
    )

    fun updateSelection(y: Float) {
        dragY = y
        val relativeY = y - columnTop
        // 28 total items: 1 star + 26 letters + 1 dot
        val rawIndex = (relativeY / columnHeight * 28).toInt()
        val index = (rawIndex - 1).coerceIn(0, alphabet.size - 1)
        if (selectedIndex != index) {
            selectedIndex = index
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onLetterSelected(alphabet[index])
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 32.dp, horizontal = 16.dp)
            .onGloballyPositioned { coordinates ->
                barHeight = coordinates.size.height.toFloat()
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    isDragging = true
                    onInteractionStart()
                    updateSelection(down.position.y)
                    down.consume()

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull()
                        if (change != null) {
                            updateSelection(change.position.y)
                            change.consume()
                        }
                    } while (event.changes.any { it.pressed })

                    isDragging = false
                    selectedIndex = -1
                    onInteractionEnd()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .onGloballyPositioned { coords ->
                    columnTop = coords.positionInParent().y
                    columnHeight = coords.size.height.toFloat().coerceAtLeast(1f)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            val symbols = listOf("☆") + alphabet.map { it.toString() } + listOf("●")
            symbols.forEachIndexed { index, symbol ->
                Box(
                    modifier = Modifier
                        .offset {
                            // Defers state reading to the layout phase to eliminate recomposition stutter
                            val currentDragY = dragY
                            val itemY = columnTop + (index + 0.5f) * (columnHeight / symbols.size)
                            val distance = abs(itemY - currentDragY)
                            
                            val maxOffsetPx = density.run { 80.dp.toPx() }
                            val spread = density.run { 100.dp.toPx() }
                            val targetOffset = -maxOffsetPx * exp(-(distance * distance) / (2 * spread * spread))
                            
                            IntOffset((targetOffset * bendAmount).toInt(), 0)
                        }
                ) {
                    val isLetter = index in 1..26
                    val isSelected = isLetter && selectedIndex == (index - 1)
                    val isAvailable = !isLetter || availableLetters.contains(symbol.first())
                    
                    Text(
                        text = symbol,
                        fontSize = if (isLetter) 14.sp else 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (isAvailable) 1f else 0.3f)
                    )
                }
            }
        }
        
        // Letter Bubble
        if (isDragging && selectedIndex in alphabet.indices) {
            Box(
                modifier = Modifier
                    .offset { 
                        IntOffset(
                            density.run { -110.dp.toPx() }.toInt(),
                            (dragY - barHeight / 2).toInt() // centered around dragY
                        ) 
                    }
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = alphabet[selectedIndex].toString(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}