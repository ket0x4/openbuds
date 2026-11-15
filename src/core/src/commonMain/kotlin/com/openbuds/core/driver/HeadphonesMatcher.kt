package com.openbuds.core.driver

/**
 * Device-family recognition, so the app layer never hardcodes product names.
 *
 * Driver modules ship one implementation each; the app resolves a device to a driver through
 * its [HeadphonesMatcher].
 */
interface HeadphonesMatcher {
    /** Short label for notifications and widgets, e.g. "FreeBuds 5i". */
    val displayName: String

    /** Full model name reported by the device, e.g. "HUAWEI FreeBuds 5i". */
    val modelName: String

    /** True when this driver speaks to [deviceName]. */
    fun matches(deviceName: String?): Boolean

    /** True when [deviceName] is the flagship model this driver is tuned for. */
    fun preferred(deviceName: String?): Boolean = matches(deviceName)
}
