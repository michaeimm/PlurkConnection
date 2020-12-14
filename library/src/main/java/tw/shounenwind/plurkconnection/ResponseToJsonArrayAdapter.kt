package tw.shounenwind.plurkconnection

import okhttp3.Response
import org.json.JSONArray
import tw.shounenwind.plurkconnection.interfaces.IResponseAdapter

class ResponseToJsonArrayAdapter : IResponseAdapter<JSONArray> {
    override fun convert(apiResponse: Response): JSONArray {
        return JSONArray(apiResponse.body!!.string())
    }
}