package tw.shounenwind.plurkconnection.interfaces

import okhttp3.Response

fun interface IResponseAdapter<T> {
    fun convert(apiResponse: Response): T
}