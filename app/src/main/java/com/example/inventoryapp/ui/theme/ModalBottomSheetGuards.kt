package com.example.inventoryapp.ui.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Remembers a [SheetState] that prevents accidental dismissal when the user swipes down slightly
 * or swipes down and then back up ("swipe down and up" bounce), while preserving:
 * - Smooth intentional swipe-down dismissal from the drag handle / header (`>= minDismissDragDp`)
 * - Immediate backdrop scrim tap and system back / close button dismissal
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberSafeModalBottomSheetState(
    skipPartiallyExpanded: Boolean = true,
    minDismissDragDp: Dp = 96.dp,
    upwardReversalToleranceDp: Dp = 12.dp,
    extraConfirmValueChange: (SheetValue) -> Boolean = { true }
): SheetState {
    val density = LocalDensity.current
    val minDismissDragPx = with(density) { minDismissDragDp.toPx() }
    val upwardReversalPx = with(density) { upwardReversalToleranceDp.toPx() }
    val tapTolerancePx = with(density) { 4.dp.toPx() }
    val imeBottomPx = WindowInsets.ime.getBottom(density)

    var stateRef by remember { mutableStateOf<SheetState?>(null) }
    var expandedBaselineOffset by remember { mutableStateOf<Float?>(null) }
    var maxOffsetReached by remember { mutableStateOf<Float?>(null) }

    val state = rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded,
        confirmValueChange = { targetValue ->
            if (!extraConfirmValueChange(targetValue)) {
                return@rememberModalBottomSheetState false
            }
            if (targetValue == SheetValue.Hidden) {
                val sheet = stateRef
                val currentOffset = sheet?.let { runCatching { it.requireOffset() }.getOrNull() }
                val baseline = expandedBaselineOffset
                if (currentOffset != null && baseline != null) {
                    val draggedDownPx = currentOffset - baseline
                    // Scrim tap / Back press / Close button happen while resting at the expanded baseline
                    if (draggedDownPx <= tapTolerancePx) {
                        return@rememberModalBottomSheetState true
                    }
                    // Reject short accidental drags or gestures where the user swiped back upward
                    val peak = maxOffsetReached ?: currentOffset
                    val reversedUpPx = peak - currentOffset
                    if (draggedDownPx < minDismissDragPx || reversedUpPx > upwardReversalPx) {
                        return@rememberModalBottomSheetState false
                    }
                }
            }
            true
        }
    )

    SideEffect {
        stateRef = state
    }

    // Re-sync expanded baseline when the software keyboard animates in or out
    LaunchedEffect(imeBottomPx) {
        delay(220)
        if (state.currentValue == SheetValue.Expanded) {
            runCatching { state.requireOffset() }.getOrNull()?.let { settledOffset ->
                expandedBaselineOffset = settledOffset
                maxOffsetReached = settledOffset
            }
        }
    }

    LaunchedEffect(state) {
        snapshotFlow {
            runCatching { state.requireOffset() }.getOrNull()
        }.collect { offset ->
            if (offset != null) {
                if (state.currentValue == SheetValue.Expanded &&
                    (expandedBaselineOffset == null || offset < expandedBaselineOffset!!)
                ) {
                    expandedBaselineOffset = offset
                }
                val baseline = expandedBaselineOffset
                if (baseline == null || offset <= baseline + tapTolerancePx) {
                    maxOffsetReached = offset
                } else {
                    maxOffsetReached = maxOf(maxOffsetReached ?: offset, offset)
                }
            }
        }
    }

    return state
}

/**
 * Consumes leftover downward overscroll and downward fling velocity from scrollable children
 * inside a [androidx.compose.material3.ModalBottomSheet] so scrolling a list or form back to the
 * top never leaks into the sheet's swipe-to-dismiss gesture.
 *
 * Users can still smoothly swipe down from the drag handle / header bar or tap the scrim to close.
 */
@Composable
fun Modifier.consumeBottomSheetNestedDismiss(): Modifier {
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return if (available.y > 0f) Offset(0f, available.y) else Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return if (available.y > 0f) Velocity(0f, available.y) else Velocity.Zero
            }
        }
    }
    return this.nestedScroll(connection)
}
