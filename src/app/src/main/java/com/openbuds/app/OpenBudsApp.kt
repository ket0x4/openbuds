package com.openbuds.app

import android.app.Application
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.openbuds.app.di.AppContainer
import com.openbuds.app.widget.BatteryWidgetReceiver
import kotlinx.coroutines.launch

class OpenBudsApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        publishWidgetPreviews()
    }

    /** Publishes the Glance-generated widget picker preview (API 35+, rate-limited by the system). */
    private fun publishWidgetPreviews() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        container.appScope.launch {
            runCatching { GlanceAppWidgetManager(this@OpenBudsApp).setWidgetPreviews(BatteryWidgetReceiver::class) }
        }
    }
}
