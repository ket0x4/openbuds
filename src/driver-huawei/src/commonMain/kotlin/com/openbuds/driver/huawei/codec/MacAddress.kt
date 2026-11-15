package com.openbuds.driver.huawei.codec

/** MAC address helpers for the Huawei MBB dual-connect payloads. */
object MacAddress {

    fun normalize(mac: String): String = mac.replace(":", "").lowercase()

    /** Returns an empty array when [mac] is not a 12-digit hex address. */
    fun toBytes(mac: String): ByteArray {
        val clean = normalize(mac)
        if (clean.length != 12) return byteArrayOf()
        return ByteArray(6) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    fun fromBytes(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }

    fun format(mac: String): String {
        val clean = normalize(mac)
        return if (clean.length == 12) {
            clean.uppercase().chunked(2).joinToString(":")
        } else {
            mac.uppercase()
        }
    }
}
