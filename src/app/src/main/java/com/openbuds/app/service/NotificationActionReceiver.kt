package com.openbuds.app.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.openbuds.app.OpenBudsApp
import com.openbuds.app.status.HeadphonesCommands
import com.openbuds.core.model.AncMode
import kotlinx.coroutines.launch

/** Handles taps on the status notification's ANC and Reconnect controls. */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val ACTION_SET_ANC = "com.openbuds.app.action.SET_ANC"
        private const val ACTION_RECONNECT = "com.openbuds.app.action.RECONNECT"
        private const val EXTRA_MODE = "mode"

        fun setAncIntent(context: Context, mode: AncMode): PendingIntent = broadcast(
            context,
            requestCode = mode.ordinal + 1,
            intent = Intent(context, NotificationActionReceiver::class.java)
                .setAction(ACTION_SET_ANC)
                .putExtra(EXTRA_MODE, mode.name),
        )

        fun reconnectIntent(context: Context): PendingIntent = broadcast(
            context,
            requestCode = 100,
            intent = Intent(context, NotificationActionReceiver::class.java).setAction(ACTION_RECONNECT),
        )

        private fun broadcast(context: Context, requestCode: Int, intent: Intent) = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? OpenBudsApp ?: return
        val pending = goAsync()
        app.container.appScope.launch {
            try {
                when (intent.action) {
                    ACTION_SET_ANC -> intent.getStringExtra(EXTRA_MODE)
                        ?.let { runCatching { AncMode.valueOf(it) }.getOrNull() }
                        ?.let { HeadphonesCommands.setAncMode(context, it) }
                    ACTION_RECONNECT -> HeadphonesCommands.reconnect(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
