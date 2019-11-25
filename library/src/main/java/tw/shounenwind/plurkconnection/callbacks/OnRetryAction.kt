package tw.shounenwind.plurkconnection.callbacks

interface OnRetryAction {
    suspend fun onRetry(e: Throwable, retryTimes: Int, totalTimes: Int)
}