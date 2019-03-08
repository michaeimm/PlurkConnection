package tw.shounenwind.plurkconnection

import android.content.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class NewThreadRetryExecutor {
    private var totalRetryTimes = 1
    private var currentRetryTimes = 0
    private var tasks: Tasks? = null

    fun setTasks(tasks: Tasks) {
        this.tasks = tasks
        tasks.bindExecutor(this)
    }

    fun setTotalRetryTimes(totalRetryTimes: Int) {
        this.totalRetryTimes = totalRetryTimes
    }

    fun run(mContext: Context?) {

        GlobalScope.launch {
            if (mContext == null)
                return@launch
            val wrContext = WeakReference(mContext)
            tasks!!.also {
                try {
                    it.mainTask()
                } catch (e: Exception) {
                    currentRetryTimes++
                    if (currentRetryTimes >= totalRetryTimes) {
                        wrContext.get() ?: return@launch
                        it.onError(e)
                    } else {
                        wrContext.get() ?: return@launch
                        it.onRetry(e, currentRetryTimes, totalRetryTimes)
                    }
                }
            }
        }
    }

}
