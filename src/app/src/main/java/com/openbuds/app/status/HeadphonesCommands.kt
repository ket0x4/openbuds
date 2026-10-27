package com.openbuds.app.status

import android.content.Context
import com.openbuds.app.OpenBudsApp
import com.openbuds.core.driver.HeadphonesDriver
import com.openbuds.core.model.AncMode
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Commands that can be fired from outside the app UI (widget, notification actions). */
object HeadphonesCommands {
    private const val CONNECT_TIMEOUT_MS = 8_000L

    private fun manager(context: Context) =
        (context.applicationContext as? OpenBudsApp)?.container?.bluetoothConnectionManager

    /** Returns the active driver, auto-connecting first if needed. */
    suspend fun awaitDriver(context: Context): HeadphonesDriver? {
        val manager = manager(context) ?: return null
        manager.activeDriver.value?.let { return it }
        manager.autoConnect()
        return withTimeoutOrNull(CONNECT_TIMEOUT_MS) { manager.activeDriver.filterNotNull().first() }
    }

    suspend fun setAncMode(context: Context, mode: AncMode): Boolean {
        val driver = awaitDriver(context) ?: return false
        return runCatching { driver.setAncMode(mode) }.isSuccess
    }

    fun nextAncMode(current: AncMode?): AncMode = when (current) {
        AncMode.NOISE_CANCELLATION -> AncMode.TRANSPARENCY
        AncMode.TRANSPARENCY -> AncMode.OFF
        AncMode.OFF, null -> AncMode.NOISE_CANCELLATION
    }

    suspend fun cycleAnc(context: Context): AncMode? {
        val manager = manager(context) ?: return null
        val currentMode = manager.activeDriver.value?.ancMode?.value ?: AncMode.OFF
        val next = nextAncMode(currentMode)
        val success = setAncMode(context, next)
        return if (success) next else null
    }

    suspend fun reconnect(context: Context) {
        val driver = awaitDriver(context) ?: return
        runCatching { driver.refreshAll() }
    }
}
