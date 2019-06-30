package tw.shounenwind.plurkconnection.callbacks

interface OnRetryAction {
    fun onRetry(e: Throwable, retryTimes: Int, totalTimes: Int)
}