package com.openbuds.driver.huawei.codec

import com.openbuds.core.model.EqPreset

/** Wire encoding for the equalizer preset (MBB service 43, commands 73 and 74). */
object HuaweiEqCodec {

    fun presetToWire(preset: EqPreset): Int = when (preset) {
        EqPreset.DEFAULT -> 1
        EqPreset.BASS_BOOST -> 2
        EqPreset.TREBLE_BOOST -> 3
        EqPreset.VOICES -> 9
    }

    fun presetFromWire(code: Int): EqPreset = when (code) {
        2 -> EqPreset.BASS_BOOST
        3 -> EqPreset.TREBLE_BOOST
        9 -> EqPreset.VOICES
        else -> EqPreset.DEFAULT
    }
}
