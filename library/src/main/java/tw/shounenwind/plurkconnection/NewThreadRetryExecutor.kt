package tw.shounenwind.plurkconnection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

    fun run(mainScope: CoroutineScope?) {
        mainScope?.launch {
            tasks!!.also {
                try {
                    it.mainTask()
                } catch (e: Exception) {
                    currentRetryTimes++
                    if (this@launch.isActive) {
                        if (currentRetryTimes >= totalRetryTimes) {
                            it.onError(e)
                        } else {
                            it.onRetry(e, currentRetryTimes, totalRetryTimes)
                        }
                    }
                }
            }
        }
    }

}
