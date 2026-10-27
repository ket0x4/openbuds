package com.openbuds.core.protocol

data class MbbCommand(
    val serviceId: Int,
    val commandId: Int,
    val args: Map<Int, ByteArray> = emptyMap()
) {
    fun isMatching(service: Int, command: Int): Boolean {
        return serviceId == service && commandId == command
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MbbCommand) return false
        if (serviceId != other.serviceId || commandId != other.commandId) return false
        if (args.size != other.args.size) return false
        return args.all { (key, value) ->
            val otherVal = other.args[key] ?: return false
            value.contentEquals(otherVal)
        }
    }

    override fun hashCode(): Int {
        var result = serviceId
        result = 31 * result + commandId
        for ((key, value) in args) {
            result = 31 * result + key
            result = 31 * result + value.contentHashCode()
        }
        return result
    }

    override fun toString(): String {
        val argsStr = args.entries.joinToString(", ") { (k, v) ->
            "$k=[${v.joinToString(",") { (it.toInt() and 0xFF).toString() }}]"
        }
        return "MbbCommand(service=$serviceId, command=$commandId, args={$argsStr})"
    }
}
