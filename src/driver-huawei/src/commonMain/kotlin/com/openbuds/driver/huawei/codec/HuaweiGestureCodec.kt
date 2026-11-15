package com.openbuds.driver.huawei.codec

import com.openbuds.core.model.GestureAction
import com.openbuds.core.model.GestureType

/**
 * Wire encoding for touch gestures (MBB service 43, commands 26/27, 30/31, 106/107).
 *
 * The firmware reuses code `0` for both voice assistant and volume control, so the mapping is
 * only unambiguous once the [GestureType] is known. [actionToWire] therefore returns `null`
 * for combinations the firmware cannot express instead of silently sending `0`.
 */
object HuaweiGestureCodec {

    const val CODE_NONE = 255

    private const val CODE_VOICE_ASSISTANT = 0
    private const val CODE_PLAY_PAUSE = 1
    private const val CODE_NEXT_TRACK = 2
    private const val CODE_PREVIOUS_TRACK = 7
    private const val CODE_CYCLE_ANC = 10
    private const val CODE_VOLUME = 0

    fun actionToWire(action: GestureAction, type: GestureType): Int? = when (type) {
        GestureType.DOUBLE_TAP_LEFT, GestureType.DOUBLE_TAP_RIGHT -> when (action) {
            GestureAction.PLAY_PAUSE -> CODE_PLAY_PAUSE
            GestureAction.NEXT_TRACK -> CODE_NEXT_TRACK
            GestureAction.PREVIOUS_TRACK -> CODE_PREVIOUS_TRACK
            GestureAction.VOICE_ASSISTANT -> CODE_VOICE_ASSISTANT
            GestureAction.NONE -> CODE_NONE
            GestureAction.CYCLE_ANC, GestureAction.ADJUST_VOLUME -> null
        }

        GestureType.TRIPLE_TAP_LEFT, GestureType.TRIPLE_TAP_RIGHT -> when (action) {
            GestureAction.NEXT_TRACK -> CODE_NEXT_TRACK
            GestureAction.PREVIOUS_TRACK -> CODE_PREVIOUS_TRACK
            GestureAction.NONE -> CODE_NONE
            else -> null
        }

        GestureType.HOLD_BOTH -> when (action) {
            GestureAction.CYCLE_ANC -> CODE_CYCLE_ANC
            GestureAction.NONE -> CODE_NONE
            else -> null
        }

        GestureType.SWIPE -> when (action) {
            GestureAction.ADJUST_VOLUME -> CODE_VOLUME
            GestureAction.NONE -> CODE_NONE
            else -> null
        }
    }

    /** Decodes a tap gesture field. Code `0` is the voice assistant for tap gestures. */
    fun actionFromWire(code: Int): GestureAction = when (code) {
        CODE_VOICE_ASSISTANT -> GestureAction.VOICE_ASSISTANT
        CODE_PLAY_PAUSE -> GestureAction.PLAY_PAUSE
        CODE_NEXT_TRACK -> GestureAction.NEXT_TRACK
        CODE_PREVIOUS_TRACK -> GestureAction.PREVIOUS_TRACK
        CODE_CYCLE_ANC -> GestureAction.CYCLE_ANC
        else -> GestureAction.NONE
    }

    /** Press-and-hold reports the same action codes as taps. */
    fun holdFromWire(code: Int): GestureAction = actionFromWire(code)

    fun swipeToWire(action: GestureAction): Int =
        if (action == GestureAction.ADJUST_VOLUME) CODE_VOLUME else CODE_NONE

    fun swipeFromWire(code: Int): GestureAction =
        if (code == CODE_VOLUME) GestureAction.ADJUST_VOLUME else GestureAction.NONE
}
