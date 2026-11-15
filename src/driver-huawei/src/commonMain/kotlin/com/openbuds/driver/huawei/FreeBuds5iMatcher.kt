package com.openbuds.driver.huawei

import com.openbuds.core.driver.HeadphonesMatcher

/** Recognises HUAWEI FreeBuds devices; [preferred] identifies the 5i model. */
object FreeBuds5iMatcher : HeadphonesMatcher {

    override val displayName: String = "FreeBuds 5i"
    override val modelName: String = "HUAWEI FreeBuds 5i"

    private const val FAMILY = "FreeBuds"
    private const val MODEL = "5i"

    override fun matches(deviceName: String?): Boolean =
        deviceName?.contains(FAMILY, ignoreCase = true) == true

    override fun preferred(deviceName: String?): Boolean =
        deviceName?.contains(MODEL, ignoreCase = true) == true
}
