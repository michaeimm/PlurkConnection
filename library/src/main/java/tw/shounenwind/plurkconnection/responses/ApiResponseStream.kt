package tw.shounenwind.plurkconnection.responses

import java.io.Reader

class ApiResponseStream : IResponse<Reader?> {
    override val content: Reader?
    override val statusCode: Int
    val e: Throwable?

    constructor(statusCode: Int, content: Reader) {
        this.statusCode = statusCode
        this.content = content
        this.e = null
    }

    constructor(e: Throwable) {
        this.statusCode = -1
        this.content = null
        this.e = e
    }

    override fun toString(): String {
        return try {
            val stringBuilder = StringBuilder("$statusCode, ")
            if (content != null) {
                stringBuilder.append(content.readText())
            }
            stringBuilder.toString()
        } catch (e1: Exception) {
            "$statusCode, $e1"
        } finally {
            content?.close()
        }

    }
}
