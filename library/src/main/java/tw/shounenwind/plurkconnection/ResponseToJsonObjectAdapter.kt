package tw.shounenwind.plurkconnection

import okhttp3.Response
import org.json.JSONObject
import tw.shounenwind.plurkconnection.interfaces.IResponseAdapter

class ResponseToJsonObjectAdapter : IResponseAdapter<JSONObject> {
    override fun convert(apiResponse: Response): JSONObject {
        return JSONObject(apiResponse.body!!.string())
    }
}