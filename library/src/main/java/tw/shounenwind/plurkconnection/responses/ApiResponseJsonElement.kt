package tw.shounenwind.plurkconnection.responses

import com.google.gson.JsonElement
import com.google.gson.JsonParser

import org.apache.commons.text.StringEscapeUtils

class ApiResponseJsonElement : IResponse<JsonElement> {
    override val content: JsonElement
    override val statusCode: Int
    val e: Throwable?

    constructor(statusCode: Int, content: JsonElement) {
        this.statusCode = statusCode
        this.content = content
        this.e = null
    }

    constructor(e: Throwable) {
        this.statusCode = -1
        this.content = JsonParser().parse("{error_text: \"" + StringEscapeUtils.escapeJson(e.toString()) + "\"}")
        this.e = e
    }

    override fun toString(): String {
        return try {
            "$statusCode, $content"
        } catch (e1: Exception) {
            "$statusCode, $e1"
        }

    }
}
