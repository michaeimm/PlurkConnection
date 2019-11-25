package tw.shounenwind.plurkconnection

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Response
import okhttp3.ResponseBody
import tw.shounenwind.plurkconnection.callbacks.OnErrorAction
import tw.shounenwind.plurkconnection.callbacks.OnRetryAction
import tw.shounenwind.plurkconnection.callbacks.RetryChecker
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
        private var retryDispatcher: CoroutineDispatcher = Dispatchers.Default
        private var onErrorAction: OnErrorAction? = null
        private var errorDispatcher: CoroutineDispatcher = Dispatchers.Default
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

        inline fun setOnRetryAction(crossinline body: (
                e: Throwable,
                retryTimes: Int,
                totalTimes: Int
        ) -> Unit) = apply {
            setOnRetryAction(object : OnRetryAction {
                override fun onRetry(e: Throwable, retryTimes: Int, totalTimes: Int) {
                    body(e, retryTimes, totalTimes)
                }
            })
        }

        inline fun setOnRetryAction(
                dispatcher: CoroutineDispatcher,
                crossinline body: (
                        e: Throwable,
                        retryTimes: Int,
                        totalTimes: Int) -> Unit
        ) = apply {
            setOnRetryAction(dispatcher, object : OnRetryAction {
                override fun onRetry(e: Throwable, retryTimes: Int, totalTimes: Int) {
                    body(e, retryTimes, totalTimes)
                }
            })
        }

        fun setRetryChecker(action: RetryChecker) {
            retryChecker = action
        }

        inline fun setRetryChecker(crossinline body: (e: Throwable) -> Boolean) = apply {
            setRetryChecker(object : RetryChecker {
                override fun onCheck(e: Throwable): Boolean {
                    return body(e)
                }
            })
        }

        fun setOnErrorAction(action: OnErrorAction) = apply {
            onErrorAction = action
        }

        fun setOnErrorAction(dispatcher: CoroutineDispatcher, action: OnErrorAction) = apply {
            errorDispatcher = dispatcher
            onErrorAction = action
        }

        inline fun setOnErrorAction(crossinline body: (e: Throwable) -> Unit) = apply {
            setOnErrorAction(object : OnErrorAction {
                override fun onError(e: Throwable) {
                    body(e)
                }
            })
        }

        inline fun setOnErrorAction(
                dispatcher: CoroutineDispatcher,
                crossinline body: (e: Throwable) -> Unit
        ) = apply {
            setOnErrorAction(dispatcher, object : OnErrorAction {
                override fun onError(e: Throwable) {
                    body(e)
                }
            })
        }

        fun setTarget(target: String) {
            this.target = target
        }

        fun setRetryDispatcher(dispatcher: CoroutineDispatcher) {
            retryDispatcher = dispatcher
        }

        fun setErrorDispatcher(dispatcher: CoroutineDispatcher) {
            errorDispatcher = dispatcher
        }

        suspend fun <T> asGsonObject(gson: Gson, type: Class<T>) = withContext(Dispatchers.IO) {
            retryExecutor {
                var body: ResponseBody? = null
                try {
                    val apiResult = callApiResult()
                    body = apiResult.body
                    gson.fromJson<T>(body!!.charStream(), type)
                } catch (e: Exception) {
                    throw e
                } finally {
                    body?.close()
                }
            }
        }

        suspend fun asString() = withContext(Dispatchers.IO) {
            retryExecutor {
                var body: ResponseBody? = null
                try {
                    val apiResult = callApiResult()
                    body = apiResult.body
                    body?.string()
                } catch (e: Exception) {
                    throw e
                } finally {
                    body?.close()
                }
            }
        }

        suspend fun asJsonElement() = withContext(Dispatchers.IO) {
            retryExecutor {
                var body: ResponseBody? = null
                try {
                    val apiResult = callApiResult()
                    body = apiResult.body
                    JsonParser.parseReader(body!!.charStream())
                } catch (e: Exception) {
                    throw e
                } finally {
                    body?.close()
                }
            }
        }

        suspend fun asNull() = withContext(Dispatchers.IO) {
            retryExecutor {
                var body: ResponseBody? = null
                try {
                    val apiResult = callApiResult()
                    body = apiResult.body
                    null
                } catch (e: Exception) {
                    throw e
                } finally {
                    body?.close()
                }
            }
        }

        suspend inline fun asJsonArray(): JsonArray? {
            return asJsonElement()?.asJsonArray
        }

        suspend inline fun asJsonObject(): JsonObject? {
            return asJsonElement()?.asJsonObject
        }

        suspend fun asSuccess() = withContext(Dispatchers.IO) {
            var result = false
            retryExecutor {
                var body: ResponseBody? = null
                try {
                    val apiResult = callApiResult()
                    body = apiResult.body!!
                    result = true
                    null
                } catch (e: Exception) {
                    throw e
                } finally {
                    body?.close()
                }
            }
            result
        }

        suspend fun upload(imageFile: File, fileName: String) = withContext(Dispatchers.IO) {
            retryExecutor {
                var body: ResponseBody? = null
                try {
                    val apiResult = startConnect(target!!, imageFile, fileName)
                    if (!apiResult.isSuccessful) {
                        throw PlurkConnectionException(
                                target!!,
                                apiResult.code.toString() + ": " + apiResult.body?.string()
                        )
                    }
                    body = apiResult.body
                    JsonParser.parseReader(body!!.charStream())
                } catch (e: Exception) {
                    throw e
                } finally {
                    body?.close()
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
                            onErrorAction?.onError(e)
                        }
                        break
                    }
                }
            }
            result
        }

        private fun callApiResult(): Response {
            val apiResult = startConnect(target!!, params ?: arrayOf())
            if (!apiResult.isSuccessful) {
                throw PlurkConnectionException(
                        target!!,
                        apiResult.code.toString() + ": " + apiResult.body?.string()
                )
            }
            return apiResult
        }
    }


}