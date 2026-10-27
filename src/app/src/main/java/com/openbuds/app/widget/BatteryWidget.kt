package com.openbuds.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.openbuds.app.OpenBudsApp
import kotlinx.coroutines.launch

/** Home screen battery widget: responsive Glance surface backed by persisted device state. */
class BatteryWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Responsive(WidgetSizes.ALL)
    override val previewSizeMode = SizeMode.Responsive(WidgetSizes.ALL)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme { WidgetContent(WidgetSnapshot.from(currentState<Preferences>())) }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent { GlanceTheme { WidgetContent(WidgetSnapshot.PREVIEW) } }
    }
}

class BatteryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BatteryWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val app = context.applicationContext as? OpenBudsApp ?: return
        val manager = app.container.bluetoothConnectionManager
        app.container.appScope.launch {
            if (manager.activeDriver.value == null) manager.autoConnect()
            manager.activeDriver.value?.refreshAll()
        }
    }
}
