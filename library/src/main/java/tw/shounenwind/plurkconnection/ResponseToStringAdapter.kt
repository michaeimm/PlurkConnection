package tw.shounenwind.plurkconnection

import okhttp3.Response
import tw.shounenwind.plurkconnection.interfaces.IResponseAdapter

class ResponseToStringAdapter : IResponseAdapter<String> {
    override fun convert(apiResponse: Response): String {
        val body = apiResponse.body
        return body?.string()!!
    }
}