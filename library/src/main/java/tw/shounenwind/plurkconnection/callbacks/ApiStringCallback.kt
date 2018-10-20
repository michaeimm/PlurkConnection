package tw.shounenwind.plurkconnection.callbacks

import okhttp3.Response
import okhttp3.ResponseBody
import tw.shounenwind.plurkconnection.responses.ApiResponseString

abstract class ApiStringCallback : BasePlurkCallback<ApiResponseString>() {


    @Throws(Exception::class)
    override fun runResult(response: Response) {
        super.runResult(response)
        var body: ResponseBody? = null
        val result: ApiResponseString
        try {
            body = response.body()
            result = ApiResponseString(response.code(), body!!.string())
        } finally {
            body?.close()
        }
        onSuccess(result)
    }
}
