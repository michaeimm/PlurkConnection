package tw.shounenwind.plurkconnection.callbacks

import okhttp3.Response
import tw.shounenwind.plurkconnection.BuildablePlurkConnection
import tw.shounenwind.plurkconnection.PlurkConnectionException
import tw.shounenwind.plurkconnection.Tasks
import tw.shounenwind.plurkconnection.responses.ApiResponseString
import tw.shounenwind.plurkconnection.responses.IResponse
import java.net.HttpURLConnection

abstract class BasePlurkCallback<T : IResponse<*>> {

    private val tasks: Tasks? = null

    @Throws(Exception::class)
    protected abstract fun onSuccess(parsedResponse: T)

    open fun onRetry(e: Throwable, retryTimes: Long, totalTimes: Long, errorAction: BuildablePlurkConnection.ErrorAction) {

    }

    open fun onError(e: Throwable) {

    }

    @Throws(Exception::class)
    open fun runResult(response: Response) {
        if (response.code() != HttpURLConnection.HTTP_OK) {
            val exception = PlurkConnectionException(
                    ApiResponseString(response.code(), response.body()!!.string())
            )
            response.close()
            throw exception
        }
    }

}
