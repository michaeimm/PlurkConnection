package tw.shounenwind.plurkconnection.interfaces

fun interface OnErrorAction {
    fun onError(e: Throwable)
}