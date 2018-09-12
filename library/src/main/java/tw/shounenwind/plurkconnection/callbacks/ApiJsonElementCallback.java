package tw.shounenwind.plurkconnection.callbacks;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import okhttp3.Response;
import okhttp3.ResponseBody;
import tw.shounenwind.plurkconnection.responses.ApiResponseJsonElement;

public abstract class ApiJsonElementCallback extends BasePlurkCallback<ApiResponseJsonElement> {

    @Override
    public void runResult(Response response) throws Exception {
        super.runResult(response);
        ResponseBody body = null;
        ApiResponseJsonElement result;
        try {
            body = response.body();
            JsonElement jsonElement = new JsonParser().parse(body.charStream());
            result = new ApiResponseJsonElement(response.code(), jsonElement);
        } finally {
            if (body != null) {
                body.close();
            }
        }
        onSuccess(result);
    }
}
