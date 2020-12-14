package tw.shounenwind.plurkconnection

import android.net.TrafficStats
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Response
import org.json.JSONObject
import tw.shounenwind.plurkconnection.interfaces.IResponseAdapter
import tw.shounenwind.plurkconnection.interfaces.OnErrorAction
import tw.shounenwind.plurkconnection.interfaces.OnRetryAction
import tw.shounenwind.plurkconnection.interfaces.RetryChecker
import java.io.File

open class ApiCaller : PlurkConnection {

    constructor(APP_KEY: String, APP_SECRET: String, token: String, token_secret: String) : super(APP_KEY, APP_SECRET, token, token_secret)

    constructor(APP_KEY: String, APP_SECRET: String) : super(APP_KEY, APP_SECRET)

    fun builder(): Builder {
        return Builder()
    }

    inline fun build(body: Builder.() -> Unit) = Builder().apply {
        body()
    }

    inner class Builder {
        private var params: Array<Param>? = null
        private var retryTimes = 1
        private var onRetryAction: OnRetryAction? = null
        private var retryDispatcher: CoroutineDispatcher = Dispatchers.Unconfined
        private var onErrorAction: OnErrorAction? = null
        private var errorDispatcher: CoroutineDispatcher = Dispatchers.Unconfined
        private var retryChecker: RetryChecker? = null
        private var target: String? = null

        fun setParams(params: Array<Param>) {
            this.params = params
        }

        fun setParam(param: Param) {
            this.params = arrayOf(param)
        }

        fun setRetryTimes(times: Int) {
            retryTimes = times
        }

        fun setOnRetryAction(action: OnRetryAction) {
            onRetryAction = action
        }

        fun setOnRetryAction(dispatcher: CoroutineDispatcher, action: OnRetryAction) {
            retryDispatcher = dispatcher
            onRetryAction = action
        }

        fun setRetryChecker(action: RetryChecker) {
            retryChecker = action
        }

        fun setOnErrorAction(action: OnErrorAction) = apply {
            onErrorAction = action
        }

        fun setOnErrorAction(dispatcher: CoroutineDispatcher, action: OnErrorAction) = apply {
            errorDispatcher = dispatcher
            onErrorAction = action
        }

        fun setTarget(target: String) {
            this.target = target
        }

        suspend fun <T> getResult(adapter: IResponseAdapter<T>) = withContext(Dispatchers.IO) {
            retryExecutor {
                getApiResponse().use { apiResult ->
                    adapter.convert(apiResult)
                }
            }
        }

        suspend fun upload(imageFile: File, fileName: String) = withContext(Dispatchers.IO) {
            retryExecutor {
                startConnect(target!!, imageFile, fileName).use { apiResult ->
                    if (!apiResult.isSuccessful) {
                        throw PlurkConnectionException(
                                target!!,
                                apiResult.code.toString() + ": " + apiResult.body?.string()
                        )
                    }
                    val body = apiResult.body
                    JSONObject(body!!.string())
                }
            }
        }

        private suspend fun <T> retryExecutor(block: () -> T): T? = withContext(Dispatchers.Default) {
            var currentRunningTime = 1
            var result: T? = null
            while (retryTimes >= currentRunningTime) {
                try {
                    result = block()
                    break
                } catch (e: Exception) {
                    if (retryTimes >= currentRunningTime + 1) {
                        if (retryChecker?.onCheck(e) != false) {
                            withContext(retryDispatcher) {
                                onRetryAction?.onRetry(e, currentRunningTime, retryTimes)
                            }
                        }
                        delay(250)
                        currentRunningTime++
                    } else {
                        withContext(errorDispatcher) {
                            onErrorAction?.onError(PlurkConnectionException(target ?: "", e))
                        }
                        break
                    }
                }
            }
            result
        }

        private fun getApiResponse(): Response {
            TrafficStats.setThreadStatsTag(arrayOf(target!!, params ?: "").contentHashCode())
            val apiResult = startConnect(target!!, params ?: arrayOf())
            TrafficStats.clearThreadStatsTag()
            if (!apiResult.isSuccessful) {
                val errors = buildString {
                    params?.forEach {
                        if (length == 0) {
                            append('?')
                        } else {
                            append('&')
                        }
                        append("${it.key}=${it.value}")
                    }
                }
                throw PlurkConnectionException(
                        errors,
                        apiResult.code.toString() + ": " + apiResult.body?.string()
                )
            }
            return apiResult
        }
    }


}