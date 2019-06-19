package tw.shounenwind.plurkconnection

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import tw.shounenwind.plurkconnection.callbacks.*
import java.io.File
import java.lang.ref.WeakReference

open class BuildablePlurkConnection : PlurkConnection {

    constructor(APP_KEY: String, APP_SECRET: String, token: String, token_secret: String) : super(APP_KEY, APP_SECRET, token, token_secret)

    constructor(APP_KEY: String, APP_SECRET: String) : super(APP_KEY, APP_SECRET)

    fun builder(mContext: Context): Builder {
        return Builder(mContext, this)
    }

    inner class Builder(mContext: Context, private val mHealingPlurkConnection: BuildablePlurkConnection) : Application.ActivityLifecycleCallbacks {
        private val retryExecutor: NewThreadRetryExecutor
        private val contextWeakReference: WeakReference<Context> = WeakReference(mContext)
        private var target: String? = null
        private var params: Array<Param>? = null
        private var callback: BasePlurkCallback<*>? = null
        private var onRetryAction: OnRetryAction? = null
        private var onErrorAction: OnErrorAction? = null
        private var mainScope: CoroutineScope? = MainScope()

        init {
            params = arrayOf()
            retryExecutor = NewThreadRetryExecutor()
        }

        fun setRetryTimes(retryTimes: Int) = apply {
            retryExecutor.setTotalRetryTimes(retryTimes)
        }

        fun setTarget(target: String) = apply {
            this.target = target
        }

        fun setParams(params: Array<Param>) = apply {
            this.params = params
        }

        fun setParam(param: Param) = apply {
            this.params = arrayOf(param)
        }

        fun setCallback(callback: BasePlurkCallback<*>) = apply {
            this.callback = callback
        }

        fun setOnRetryAction(action: OnRetryAction) = apply {
            onRetryAction = action
        }

        inline fun setOnRetryAction(crossinline body: (
                e: Throwable,
                retryTimes: Long,
                totalTimes: Long,
                errorAction: ErrorAction
        ) -> Unit) = apply {
            setOnRetryAction(object: OnRetryAction{
                override fun onRetry(e: Throwable, retryTimes: Long, totalTimes: Long, errorAction: ErrorAction) {
                    body(e, retryTimes, totalTimes, errorAction)
                }

            })
        }

        fun setOnErrorAction(action: OnErrorAction) = apply {
            onErrorAction = action
        }

        inline fun setOnErrorAction(crossinline body: (e: Throwable) -> Unit) = apply {
            setOnErrorAction(object : OnErrorAction{
                override fun onError(e: Throwable) {
                    body(e)
                }
            })
        }

        fun call() {
            val mContext = contextWeakReference.get()?:return
            retryExecutor.setTasks(object : Tasks(mContext) {
                @Throws(Exception::class)
                override fun mainTask() {
                    if (callback == null) {
                        callback = ApiNullCallback()
                    }
                    callback!!.runResult(mHealingPlurkConnection.startConnect(target!!, params!!))
                }

                override fun onRetry(e: Throwable, retryTimes: Int, totalTimes: Int) {
                    onRetryAction?.onRetry(e, retryTimes.toLong(), totalTimes.toLong(), ErrorAction(this))
                }

                override fun onError(e: Throwable) {
                    onErrorAction?.onError(e)
                }
            })
            synchronized(Builder::class.java) {
                retryExecutor.run(mainScope)
            }

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
                    onRetryAction?.onRetry(e, retryTimes.toLong(), totalTimes.toLong(), ErrorAction(this))
                }

                fun onError(e: Exception) {
                    onErrorAction?.onError(e)
                }
            })

            synchronized(Builder::class.java) {
                retryExecutor.run(mainScope)
            }

        }

        override fun onActivityPaused(p0: Activity) {

        }

        override fun onActivityStarted(p0: Activity) {

        }

        override fun onActivityDestroyed(p0: Activity) {
            if (contextWeakReference.get() == p0) {
                synchronized(Builder::class.java) {
                    mainScope?.cancel()
                    mainScope = null
                }
            }
        }

        override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {

        }

        override fun onActivityStopped(p0: Activity) {

        }

        override fun onActivityCreated(p0: Activity, p1: Bundle?) {

        }

        override fun onActivityResumed(p0: Activity) {

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
