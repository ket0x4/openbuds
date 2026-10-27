package com.openbuds.app.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.openbuds.app.R
import com.openbuds.app.status.BatteryTone
import com.openbuds.app.status.DeviceStatus
import com.openbuds.app.status.RingRenderer
import com.openbuds.app.status.StatusFormat
import com.openbuds.app.ui.MainActivity
import com.openbuds.core.model.AncMode
import com.openbuds.core.model.BatteryStatus

/**
 * Builds the ongoing status notification.
 *
 * - Android 16+: standard template + [NotificationCompat.ProgressStyle] so it qualifies as a
 *   Live Update (custom views are not allowed there). Promotion is only requested while
 *   charging or while ANC/Awareness is active.
 * - Android 12–15: [NotificationCompat.DecoratedCustomViewStyle] with battery bars / rings.
 * - Disconnected: plain template with a Reconnect action.
 */
class StatusNotificationBuilder(private val context: Context) {

    companion object {
        const val DEVICE_NAME = "FreeBuds 5i"
        private val ANC_ORDER = listOf(AncMode.NOISE_CANCELLATION, AncMode.TRANSPARENCY, AncMode.OFF)
    }

    // Resolved on access so day/night switches are picked up on the next update.
    private val palette get() = Palette.resolve(context)

    fun build(channelId: String, status: DeviceStatus): Notification {
        val battery = status.battery
        val base = NotificationCompat.Builder(context, channelId)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setColor(palette.accent)

        if (!status.isConnected || battery == null) {
            return base
                .setSmallIcon(R.drawable.ic_stat_buds_off)
                .setContentTitle(DEVICE_NAME)
                .setContentText(if (status.isConnecting) "Connecting…" else "Disconnected")
                .addAction(R.drawable.ic_refresh, "Reconnect", NotificationActionReceiver.reconnectIntent(context))
                .build()
        }

        val mode = status.ancMode ?: AncMode.OFF
        base.setSmallIcon(R.drawable.ic_stat_buds_level, StatusFormat.iconLevel(battery))
            .setContentTitle(DEVICE_NAME)
            .setContentText(StatusFormat.batteryLine(battery))
            .setSubText(StatusFormat.ancLabel(mode))

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            buildLiveUpdate(base, battery, mode)
        } else {
            buildCustom(base, battery, mode)
        }
    }

    // region Android 16+ Live Update

    private fun buildLiveUpdate(
        builder: NotificationCompat.Builder,
        battery: BatteryStatus,
        mode: AncMode,
    ): Notification {
        // Three 100-unit groups (L / R / Case), each split into a "filled" and "remaining" segment.
        val segments = buildList {
            listOf(
                battery.leftPercent to battery.isLeftCharging,
                battery.rightPercent to battery.isRightCharging,
                battery.casePercent to battery.isCaseCharging,
            ).forEach { (pct, charging) ->
                val filled = pct?.coerceIn(0, 100) ?: 0
                if (filled > 0) add(NotificationCompat.ProgressStyle.Segment(filled).setColor(palette.toneColor(StatusFormat.tone(pct, charging))))
                if (filled < 100) add(NotificationCompat.ProgressStyle.Segment(100 - filled).setColor(palette.track))
            }
        }
        val style = NotificationCompat.ProgressStyle()
            .setProgressSegments(segments)
            .setStyledByProgress(false)
            .setProgress(segments.sumOf { it.length })
            .setProgressStartIcon(IconCompat.createWithResource(context, R.drawable.ic_earbud_left))
            .setProgressEndIcon(IconCompat.createWithResource(context, R.drawable.ic_earbuds_case))

        builder.setStyle(style)
        ancActions(mode).forEach(builder::addAction)

        if (StatusFormat.shouldPromote(battery, mode)) {
            builder.setRequestPromotedOngoing(true)
            StatusFormat.chipText(battery).takeIf { it.isNotEmpty() }?.let(builder::setShortCriticalText)
        }
        return builder.build()
    }

    private fun ancActions(current: AncMode): List<NotificationCompat.Action> =
        ANC_ORDER.filter { it != current }.map { mode ->
            NotificationCompat.Action(
                ancIcon(mode),
                when (mode) {
                    AncMode.NOISE_CANCELLATION -> "Noise cancelling"
                    AncMode.TRANSPARENCY -> "Awareness"
                    AncMode.OFF -> "Turn off ANC"
                },
                NotificationActionReceiver.setAncIntent(context, mode),
            )
        }

    // endregion

    // region Android 12–15 custom layout

    private fun buildCustom(
        builder: NotificationCompat.Builder,
        battery: BatteryStatus,
        mode: AncMode,
    ): Notification {
        val items = listOf(
            Item(battery.leftPercent, battery.isLeftCharging, R.id.left_pct, R.id.left_glyph, R.id.left_bar, R.id.left_ring, R.id.left_bolt),
            Item(battery.rightPercent, battery.isRightCharging, R.id.right_pct, R.id.right_glyph, R.id.right_bar, R.id.right_ring, R.id.right_bolt),
            Item(battery.casePercent, battery.isCaseCharging, R.id.case_pct, R.id.case_glyph, R.id.case_bar, R.id.case_ring, R.id.case_bolt),
        )

        val collapsed = RemoteViews(context.packageName, R.layout.notification_collapsed).apply {
            items.forEach { item ->
                val color = palette.toneColor(item.tone)
                setTextViewText(item.pct, StatusFormat.percent(item.percent) + if (item.charging) " ⚡" else "")
                setTextColor(item.pct, if (item.tone == BatteryTone.LOW) palette.error else palette.onSurface)
                setInt(item.glyph, "setColorFilter", palette.onSurfaceVariant)
                setProgressBar(item.bar, 100, item.percent ?: 0, false)
                setColorStateList(item.bar, "setProgressTintList", ColorStateList.valueOf(color))
                setColorStateList(item.bar, "setProgressBackgroundTintList", ColorStateList.valueOf(palette.track))
            }
        }

        val expanded = RemoteViews(context.packageName, R.layout.notification_expanded).apply {
            setTextViewText(R.id.title, StatusFormat.ancLabel(mode))
            items.forEach { item ->
                setImageViewBitmap(
                    item.ring,
                    RingRenderer.coloredRing(item.percent, palette.track, palette.toneColor(item.tone)),
                )
                setInt(item.glyph, "setColorFilter", palette.onSurface)
                setTextViewText(item.pct, StatusFormat.percent(item.percent))
                setTextColor(item.pct, if (item.tone == BatteryTone.LOW) palette.error else palette.onSurface)
                setViewVisibility(item.bolt, if (item.charging) View.VISIBLE else View.GONE)
                setColorStateList(item.bolt, "setBackgroundTintList", ColorStateList.valueOf(palette.accent))
                setInt(item.bolt, "setColorFilter", palette.onAccent)
            }
            listOf(
                AncButton(AncMode.NOISE_CANCELLATION, R.id.anc_nc_btn, R.id.anc_nc_icon, R.id.anc_nc_label),
                AncButton(AncMode.TRANSPARENCY, R.id.anc_aw_btn, R.id.anc_aw_icon, R.id.anc_aw_label),
                AncButton(AncMode.OFF, R.id.anc_off_btn, R.id.anc_off_icon, R.id.anc_off_label),
            ).forEach { (m, btn, icon, label) ->
                val active = m == mode
                val bg = if (active) palette.accent else palette.container
                val fg = if (active) palette.onAccent else palette.onContainer
                setColorStateList(btn, "setBackgroundTintList", ColorStateList.valueOf(bg))
                setInt(icon, "setColorFilter", fg)
                setTextColor(label, fg)
                setOnClickPendingIntent(btn, NotificationActionReceiver.setAncIntent(context, m))
                setContentDescription(btn, StatusFormat.ancLabel(m))
            }
        }

        return builder
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsed)
            .setCustomBigContentView(expanded)
            .build()
    }

    /** One battery slot and the view ids it binds to (ids are shared across both layouts). */
    private data class Item(
        val percent: Int?,
        val charging: Boolean,
        val pct: Int,
        val glyph: Int,
        val bar: Int,
        val ring: Int,
        val bolt: Int,
    ) {
        val tone get() = StatusFormat.tone(percent, charging)
    }

    private data class AncButton(val mode: AncMode, val btn: Int, val icon: Int, val label: Int)

    // endregion

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    @DrawableRes
    private fun ancIcon(mode: AncMode): Int = when (mode) {
        AncMode.NOISE_CANCELLATION -> R.drawable.ic_noise_cancel
        AncMode.TRANSPARENCY -> R.drawable.ic_noise_awareness
        AncMode.OFF -> R.drawable.ic_noise_off
    }

    /** Material You colors resolved for the current day/night mode. */
    private data class Palette(
        val accent: Int,
        val onAccent: Int,
        val secondary: Int,
        val container: Int,
        val onContainer: Int,
        val track: Int,
        val error: Int,
        val onSurface: Int,
        val onSurfaceVariant: Int,
    ) {
        fun toneColor(tone: BatteryTone): Int = when (tone) {
            BatteryTone.CHARGING -> accent
            BatteryTone.LOW -> error
            BatteryTone.NORMAL -> secondary
            BatteryTone.UNKNOWN -> track
        }

        companion object {
            fun resolve(context: Context): Palette {
                val night = (context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                fun c(id: Int) = context.getColor(id)
                return if (night) Palette(
                    accent = c(android.R.color.system_accent1_200),
                    onAccent = c(android.R.color.system_accent1_800),
                    secondary = c(android.R.color.system_accent2_200),
                    container = c(android.R.color.system_accent2_700),
                    onContainer = c(android.R.color.system_accent2_100),
                    track = c(android.R.color.system_accent2_800),
                    error = 0xFFF2B8B5.toInt(),
                    onSurface = c(android.R.color.system_neutral1_100),
                    onSurfaceVariant = c(android.R.color.system_neutral2_200),
                ) else Palette(
                    accent = c(android.R.color.system_accent1_600),
                    onAccent = c(android.R.color.system_accent1_0),
                    secondary = c(android.R.color.system_accent2_600),
                    container = c(android.R.color.system_accent2_100),
                    onContainer = c(android.R.color.system_accent2_900),
                    track = c(android.R.color.system_accent2_100),
                    error = 0xFFB3261E.toInt(),
                    onSurface = c(android.R.color.system_neutral1_900),
                    onSurfaceVariant = c(android.R.color.system_neutral2_700),
                )
            }
        }
    }
}
