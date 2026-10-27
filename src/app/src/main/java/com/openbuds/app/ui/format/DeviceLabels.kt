package com.openbuds.app.ui.format

import com.openbuds.core.model.EqPreset

/**
 * Short, glanceable labels used in compact summary surfaces. Longer descriptive copy for
 * full settings screens lives with those screens.
 */
val EqPreset.shortLabel: String
    get() = when (this) {
        EqPreset.DEFAULT -> "Default"
        EqPreset.BASS_BOOST -> "Bass"
        EqPreset.TREBLE_BOOST -> "Treble"
        EqPreset.VOICES -> "Voices"
    }
