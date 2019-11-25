package tw.shounenwind.plurkconnection.callbacks

interface OnErrorAction {
    suspend fun onError(e: Throwable)
}