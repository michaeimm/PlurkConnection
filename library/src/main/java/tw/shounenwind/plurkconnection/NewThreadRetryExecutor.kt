package tw.shounenwind.plurkconnection

import android.content.Context

import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class NewThreadRetryExecutor {

    private var threadPool: ExecutorService? = null
    private var totalRetryTimes = 1
    private var currentRetryTimes = 0
    private var tasks: Tasks? = null

    fun setThreadPool(threadPool: ExecutorService) {
        this.threadPool = threadPool
    }

    fun setTasks(tasks: Tasks) {
        this.tasks = tasks
        tasks.bindExecutor(this)
    }

    fun setTotalRetryTimes(totalRetryTimes: Int) {
        this.totalRetryTimes = totalRetryTimes
    }

    fun run(mContext: Context?) {
        if (threadPool == null) {
            threadPool = Executors.newCachedThreadPool()
        }

        threadPool!!.execute {
            if (mContext == null)
                return@execute
            val wrContext = WeakReference(mContext)
            try {
                tasks!!.mainTask()
            } catch (e: Exception) {
                currentRetryTimes++
                if (currentRetryTimes >= totalRetryTimes) {
                    wrContext.get() ?: return@execute
                    tasks!!.onError(e)
                } else {
                    wrContext.get() ?: return@execute
                    tasks!!.onRetry(e, currentRetryTimes, totalRetryTimes)
                }
            }
        }
    }

}
