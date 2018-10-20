package tw.shounenwind.plurkconnection.callbacks

import okhttp3.Response
import okhttp3.ResponseBody
import tw.shounenwind.plurkconnection.responses.ApiResponseStream

abstract class ApiStreamCallback : BasePlurkCallback<ApiResponseStream>() {


    @Throws(Exception::class)
    override fun runResult(response: Response) {
        super.runResult(response)
        var body: ResponseBody? = null
        val result: ApiResponseStream
        try {
            body = response.body()
            result = ApiResponseStream(response.code(), body!!.charStream())
        } finally {
            body?.close()
        }
        onSuccess(result)
    }
}
