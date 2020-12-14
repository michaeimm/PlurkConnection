package tw.shounenwind.plurkconnection.interfaces

fun interface OnRetryAction {
    fun onRetry(e: Throwable, retryTimes: Int, totalTimes: Int)
}