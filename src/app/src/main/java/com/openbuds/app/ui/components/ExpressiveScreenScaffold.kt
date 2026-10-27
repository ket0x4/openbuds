package com.openbuds.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Vertical clearance reserved for the floating navigation dock. */
val NavigationDockClearance: Dp = 104.dp

/**
 * Standard scrollable screen container.
 *
 * Provides the shared canvas colour, vertical scroll, status bar inset handling and the
 * bottom clearance that keeps content from sliding under the floating dock. Screens only
 * supply their own content padding and arrangement.
 *
 * [contentPadding] is applied *inside* the dock clearance, so callers should pass their
 * visual padding only and let the scaffold add the space for the dock.
 */
@Composable
fun ExpressiveScreenScaffold(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    applyStatusBarPadding: Boolean = true,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(14.dp),
    dockClearance: Dp = NavigationDockClearance,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .then(if (applyStatusBarPadding) Modifier.statusBarsPadding() else Modifier)
            .padding(contentPadding)
            .padding(bottom = dockClearance),
        verticalArrangement = verticalArrangement,
        content = content
    )
}
