package com.openbuds.app.status

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.LruCache
import androidx.core.graphics.createBitmap

/**
 * Draws battery rings as bitmaps (Glance / RemoteViews can't draw arcs).
 *
 * Mask variants are white-on-transparent so the widget can tint them with dynamic
 * [androidx.glance.GlanceTheme] colors; colored variants are used by the notification.
 * All bitmaps share one fixed resolution to keep RemoteViews parcel size small.
 */
object RingRenderer {
    private const val SIZE_PX = 144
    private const val SPLIT_GAP_DEG = 14f

    enum class Side { LEFT, RIGHT }

    private val cache = LruCache<String, Bitmap>(32)

    private fun cached(key: String, draw: (Canvas, Paint, RectF) -> Unit): Bitmap =
        cache.get(key) ?: run {
            val bmp = createBitmap(SIZE_PX, SIZE_PX)
            val canvas = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
            draw(canvas, paint, RectF())
            cache.put(key, bmp)
            bmp
        }

    private fun RectF.inset(stroke: Float): RectF = apply {
        val half = stroke / 2f + 1f
        set(half, half, SIZE_PX - half, SIZE_PX - half)
    }

    private fun strokePx(ratio: Float) = SIZE_PX * ratio

    /** Full-circle track mask. */
    fun trackMask(strokeRatio: Float = 0.11f): Bitmap = cached("track:$strokeRatio") { c, p, r ->
        p.strokeWidth = strokePx(strokeRatio)
        p.color = WHITE
        c.drawArc(r.inset(p.strokeWidth), 0f, 360f, false, p)
    }

    /** Progress arc mask, clockwise from 12 o'clock. */
    fun arcMask(percent: Int, strokeRatio: Float = 0.11f): Bitmap {
        val pct = percent.coerceIn(0, 100)
        return cached("arc:$pct:$strokeRatio") { c, p, r ->
            if (pct == 0) return@cached
            p.strokeWidth = strokePx(strokeRatio)
            p.color = WHITE
            c.drawArc(r.inset(p.strokeWidth), -90f, 360f * pct / 100f, false, p)
        }
    }

    /** Half-ring track mask for the 1x1 split ring. */
    fun splitTrackMask(side: Side, strokeRatio: Float = 0.10f): Bitmap =
        cached("strack:$side:$strokeRatio") { c, p, r ->
            p.strokeWidth = strokePx(strokeRatio)
            p.color = WHITE
            val (start, sweep) = halfSweep(side, 1f)
            c.drawArc(r.inset(p.strokeWidth), start, sweep, false, p)
        }

    /** Half-ring fill that grows from the bottom upward on its side. */
    fun splitArcMask(side: Side, percent: Int, strokeRatio: Float = 0.10f): Bitmap {
        val pct = percent.coerceIn(0, 100)
        return cached("sarc:$side:$pct:$strokeRatio") { c, p, r ->
            if (pct == 0) return@cached
            p.strokeWidth = strokePx(strokeRatio)
            p.color = WHITE
            val (start, sweep) = halfSweep(side, pct / 100f)
            c.drawArc(r.inset(p.strokeWidth), start, sweep, false, p)
        }
    }

    private fun halfSweep(side: Side, fraction: Float): Pair<Float, Float> {
        val span = 180f - 2 * SPLIT_GAP_DEG
        return when (side) {
            // 90° = 6 o'clock; LEFT sweeps clockwise through 9 o'clock, RIGHT counter-clockwise through 3.
            Side.LEFT -> (90f + SPLIT_GAP_DEG) to span * fraction
            Side.RIGHT -> (90f - SPLIT_GAP_DEG) to -span * fraction
        }
    }

    /** Pre-colored ring (track + arc) for notification RemoteViews. */
    fun coloredRing(percent: Int?, trackColor: Int, arcColor: Int, strokeRatio: Float = 0.12f): Bitmap {
        val pct = percent?.coerceIn(0, 100) ?: 0
        return cached("col:$pct:$trackColor:$arcColor:$strokeRatio") { c, p, r ->
            p.strokeWidth = strokePx(strokeRatio)
            val rect = r.inset(p.strokeWidth)
            p.color = trackColor
            c.drawArc(rect, 0f, 360f, false, p)
            if (pct > 0) {
                p.color = arcColor
                c.drawArc(rect, -90f, 360f * pct / 100f, false, p)
            }
        }
    }

    private const val WHITE = -0x1 // 0xFFFFFFFF
}
