package tw.shounenwind.plurkconnection.callbacks

import tw.shounenwind.plurkconnection.BuildablePlurkConnection

interface OnRetryAction {
    fun onRetry(e: Throwable, retryTimes: Long, totalTimes: Long, errorAction: BuildablePlurkConnection.ErrorAction)
}