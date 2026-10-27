package com.openbuds.app.widget

import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import com.openbuds.app.ui.MainActivity

/** Root widget surface: background, launch action, and responsive layout dispatch. */
@Composable
internal fun WidgetContent(state: WidgetSnapshot) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center,
    ) {
        when (WidgetFormat.forSize(LocalSize.current)) {
            WidgetFormat.TINY -> TinyLayout(state)
            WidgetFormat.ROW -> RowLayout(state)
            WidgetFormat.SQUARE -> SquareLayout(state)
            WidgetFormat.WIDE -> WideLayout(state)
        }
    }
}
