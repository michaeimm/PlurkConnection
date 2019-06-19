package tw.shounenwind.plurkconnection

import kotlinx.coroutines.CoroutineScope

abstract class Tasks(private val mainScope: CoroutineScope?) {

    private var tasks: NewThreadRetryExecutor? = null

    @Throws(Exception::class)
    internal abstract fun mainTask()

    open fun onRetry(e: Throwable, retryTimes: Int, totalTimes: Int) {
        retry()
    }

    open fun onError(e: Throwable) {

    }

    fun bindExecutor(tasks: NewThreadRetryExecutor){
        this.tasks = tasks
    }

    fun retry() {
        try {
            Thread.sleep(250)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        tasks!!.run(mainScope)
    }

    fun error(e: Throwable) {
        onError(e)
    }
}