package tw.shounenwind.plurkconnection

import android.content.Context
import tw.shounenwind.plurkconnection.callbacks.ApiNullCallback
import tw.shounenwind.plurkconnection.callbacks.ApiStringCallback
import tw.shounenwind.plurkconnection.callbacks.BasePlurkCallback
import tw.shounenwind.plurkconnection.responses.ApiResponseNull
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService

open class BuildablePlurkConnection : PlurkConnection {
    private var threadPool: ExecutorService? = null


    constructor(APP_KEY: String, APP_SECRET: String, token: String, token_secret: String) : super(APP_KEY, APP_SECRET, token, token_secret)

    constructor(APP_KEY: String, APP_SECRET: String) : super(APP_KEY, APP_SECRET)

    fun setThreadPool(threadPool: ExecutorService) {
        this.threadPool = threadPool
    }

    fun builder(mContext: Context): Builder {
        return Builder(mContext, this)
    }

    inner class Builder(mContext: Context, private val mHealingPlurkConnection: BuildablePlurkConnection) {

        private val retryExecutor: NewThreadRetryExecutor
        private val contextWeakReference: WeakReference<Context> = WeakReference(mContext)
        private var target: String? = null
        private var params: Array<Param>? = null
        private var callback: BasePlurkCallback<*>? = null

        init {
            params = arrayOf()
            retryExecutor = NewThreadRetryExecutor()
            if (threadPool != null) {
                retryExecutor.setThreadPool(threadPool!!)
            }
        }

        fun setRetryTimes(retryTimes: Int): Builder {
            retryExecutor.setTotalRetryTimes(retryTimes)
            return this
        }

        fun setTarget(target: String): Builder {
            this.target = target
            return this
        }

        fun setParams(params: Array<Param>): Builder {
            this.params = params
            return this
        }

        fun setParam(param: Param): Builder {
            this.params = arrayOf(param)
            return this
        }

        fun setCallback(callback: BasePlurkCallback<*>): Builder {
            this.callback = callback
            return this
        }

        fun call() {
            val mContext = contextWeakReference.get()?:return
            retryExecutor.setTasks(object : Tasks(mContext) {
                @Throws(Exception::class)
                override fun mainTask() {
                    if (callback == null) {
                        callback = object : ApiNullCallback() {
                            @Throws(Exception::class)
                            public override fun onSuccess(parsedResponse: ApiResponseNull) {

                            }
                        }
                    }
                    callback!!.runResult(mHealingPlurkConnection.startConnect(target!!, params!!))
                }

                override fun onRetry(e: Throwable, retryTimes: Int, totalTimes: Int) {
                    if (callback != null)
                        callback!!.onRetry(e, retryTimes.toLong(), totalTimes.toLong(), ErrorAction(this))
                }

                override fun onError(e: Throwable) {
                    if (callback != null)
                        callback!!.onError(e)
                }
            })
            retryExecutor.run(contextWeakReference.get())

        }

        fun upload(imageFile: File, fileName: String) {
            val mContext = contextWeakReference.get()?:return
            retryExecutor.setTasks(object : Tasks(mContext) {

                @Throws(Exception::class)
                override fun mainTask() {
                    if (callback !is ApiStringCallback) {
                        throw Exception("Callback needs to instanceof ApiStringCallback")
                    }
                    callback!!.runResult(mHealingPlurkConnection.startConnect(target!!, imageFile, fileName))
                }

                fun onRetry(e: Exception, retryTimes: Int, totalTimes: Int) {
                    if (callback != null) {
                        callback!!.onRetry(e, retryTimes.toLong(), totalTimes.toLong(), ErrorAction(this))
                    }
                }

                fun onError(e: Exception) {
                    if (callback != null)
                        callback!!.onError(e)
                }
            })

            retryExecutor.run(contextWeakReference.get())

        }
    }

    inner class ErrorAction(private val tasks: Tasks) {

        fun retry() {
            tasks.retry()
        }

        fun error(e: Throwable) {
            tasks.error(e)
        }
    }

    companion object {
        private const val TAG = "BPC"
    }

}
