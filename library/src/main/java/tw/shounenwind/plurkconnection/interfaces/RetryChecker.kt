package tw.shounenwind.plurkconnection.interfaces

fun interface RetryChecker {
    fun onCheck(e: Throwable): Boolean
}
