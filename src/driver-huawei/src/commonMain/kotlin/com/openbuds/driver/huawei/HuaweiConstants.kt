package com.openbuds.driver.huawei

object HuaweiConstants {
    const val SERVICE_BATTERY = 1
    const val COMMAND_BATTERY_QUERY = 8
    const val COMMAND_BATTERY_EVENT = 39

    const val SERVICE_DEVICE_INFO = 1
    const val COMMAND_DEVICE_INFO = 1
    const val COMMAND_DEVICE_INFO_EXT = 7

    const val SERVICE_SETTINGS = 43
    const val COMMAND_IN_EAR = 3
    const val COMMAND_ANC_SET = 4
    const val COMMAND_ANC_LEVEL_SET = 5
    const val COMMAND_AUTO_PAUSE_SET = 16
    const val COMMAND_AUTO_PAUSE_GET = 17
    const val COMMAND_HOLD_SPLIT_SET = 22
    const val COMMAND_HOLD_SPLIT_GET = 23
    const val COMMAND_HOLD_MODES_SET = 24
    const val COMMAND_HOLD_MODES_GET = 25
    const val COMMAND_DOUBLE_TAP_SET = 26
    const val COMMAND_DOUBLE_TAP_GET = 27
    const val COMMAND_HOLD_SET = 28
    const val COMMAND_HOLD_GET = 29
    const val COMMAND_SWIPE_SET = 30
    const val COMMAND_SWIPE_GET = 31
    const val COMMAND_FIT_TEST = 38
    const val COMMAND_ANC_STATUS = 42

    // Dual Connect (Multi-device) - Service 43 (0x2b)
    const val COMMAND_DUAL_CONNECT_ENABLED_SET = 46 // 0x2e
    const val COMMAND_DUAL_CONNECT_ENABLED_GET = 47 // 0x2f
    const val COMMAND_DUAL_CONNECT_ENUMERATE = 49   // 0x31
    const val COMMAND_DUAL_CONNECT_PREFERRED_SET = 50 // 0x32
    const val COMMAND_DUAL_CONNECT_EXECUTE = 51     // 0x33
    const val COMMAND_DUAL_CONNECT_CHANGE_EVENT = 54 // 0x36

    const val DUAL_CONNECT_ACTION_CONNECT = 1
    const val DUAL_CONNECT_ACTION_DISCONNECT = 2
    const val DUAL_CONNECT_ACTION_UNPAIR = 3
    const val DUAL_CONNECT_ACTION_ENABLE_AUTO = 4
    const val DUAL_CONNECT_ACTION_DISABLE_AUTO = 5

    const val COMMAND_EQ_SET = 73
    const val COMMAND_EQ_GET = 74
    const val COMMAND_TRIPLE_TAP_SET = 106
    const val COMMAND_TRIPLE_TAP_GET = 107
    const val COMMAND_LOW_LATENCY = 108
    const val COMMAND_SOUND_QUALITY_SET = 162
    const val COMMAND_SOUND_QUALITY_GET = 163
}
