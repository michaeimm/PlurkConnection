package tw.shounenwind.plurkconnection.callbacks

import com.google.gson.JsonParser
import okhttp3.Response
import okhttp3.ResponseBody
import tw.shounenwind.plurkconnection.responses.ApiResponseJsonElement

abstract class ApiJsonElementCallback : BasePlurkCallback<ApiResponseJsonElement>() {

    @Throws(Exception::class)
    override fun runResult(response: Response) {
        super.runResult(response)
        var body: ResponseBody? = null
        val result: ApiResponseJsonElement
        try {
            body = response.body()
            val jsonElement = JsonParser().parse(body!!.charStream())
            result = ApiResponseJsonElement(response.code(), jsonElement)
        } catch (e: Exception) {
            throw e
        } finally {
            body?.close()
        }
        onSuccess(result)
    }
}
