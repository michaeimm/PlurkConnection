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
            "$statusCode, " +
                    if (content == null)
                        ""
                    else {
                        val text = content.readText()
                        content.close()
                        text
                    }
        } catch (e1: Exception) {
            "$statusCode, $e1"
        }

    }
}
