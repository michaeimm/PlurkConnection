package tw.shounenwind.plurkconnection.callbacks

interface RetryChecker {
    fun onCheck(e: Throwable): Boolean
}
