package tw.shounenwind.plurkconnection

import java.util.*

class Param {
    val key: String
    val value: String

    constructor(key: String, value: String) {
        this.key = key
        this.value = value
    }

    constructor(key: String, value: Long) {
        this.key = key
        this.value = value.toString() + ""
    }

    constructor(key: String, value: Int) {
        this.key = key
        this.value = value.toString() + ""
    }

    constructor(key: String, value: Boolean) {
        this.key = key
        this.value = if (value) "true" else "false"
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(arrayOf(key, value))
    }

    override fun toString(): String {
        return "$key, $value"
    }

    override fun equals(other: Any?): Boolean {
        return other is Param && hashCode() == other.hashCode()
    }
}