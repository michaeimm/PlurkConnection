package tw.shounenwind.plurkconnection.callbacks

import okhttp3.Response
import tw.shounenwind.plurkconnection.responses.ApiResponseNull

class ApiNullCallback : BasePlurkCallback<ApiResponseNull>() {

    @Throws(Exception::class)
    override fun onSuccess(parsedResponse: ApiResponseNull) {

    }

    @Throws(Exception::class)
    override fun runResult(response: Response) {
        super.runResult(response)
        val body = response.body()
        body?.close()
        onSuccess(ApiResponseNull(response.code()))
    }
}
