package com.openbuds.app.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/** Responsive breakpoints: 1x1 → 2x1 → 2x2 → 4x2. */
internal object WidgetSizes {
    val TINY = DpSize(50.dp, 50.dp)
    val ROW = DpSize(110.dp, 50.dp)
    val SQUARE = DpSize(110.dp, 110.dp)
    val WIDE = DpSize(220.dp, 110.dp)
    val ALL = setOf(TINY, ROW, SQUARE, WIDE)
}

/** Layout family rendered for the available widget size. Larger formats are checked first. */
internal enum class WidgetFormat {
    TINY,
    ROW,
    SQUARE,
    WIDE;

    companion object {
        fun forSize(size: DpSize): WidgetFormat = when {
            size.width >= WidgetSizes.WIDE.width && size.height >= WidgetSizes.WIDE.height -> WIDE
            size.width >= WidgetSizes.SQUARE.width && size.height >= WidgetSizes.SQUARE.height -> SQUARE
            size.width >= WidgetSizes.ROW.width -> ROW
            else -> TINY
        }
    }
}
