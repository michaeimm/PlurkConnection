package tw.shounenwind.plurkconnection.responses;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Response;
import okhttp3.ResponseBody;

public class ApiResponse {
    private final Response response;

    public ApiResponse(Response response) {
        this.response = response;
    }

    public ApiResponseString getAsApiResponseString() {
        ResponseBody body = null;
        ApiResponseString result = null;
        try {
            body = response.body();
            result = new ApiResponseString(response.code(), body.string());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (body != null) {
                body.close();
            }
        }
        return result;
    }

    public ApiResponseStream getAsApiResponseStream() {
        ResponseBody body = null;
        ApiResponseStream result;
        try {
            body = response.body();
            result = new ApiResponseStream(response.code(), body.charStream());
        } finally {
            if (body != null) {
                body.close();
            }
        }
        return result;
    }

    public ApiResponseJsonElement getAsApiResponseJsonElement() throws Exception {
        ResponseBody body = null;
        ApiResponseJsonElement result;
        try {
            body = response.body();
            JsonElement jsonElement = new JsonParser().parse(body.charStream());
            result = new ApiResponseJsonElement(response.code(), jsonElement);
        } catch (Exception e) {
            result = new ApiResponseJsonElement(new Exception(body.string(), e));
        } finally {
            if (body != null) {
                body.close();
            }
        }
        return result;
    }

    public ApiResponseNull getNoResult() {
        ResponseBody body = response.body();
        if (body != null)
            body.close();
        return new ApiResponseNull(response.code(), null);
    }
}
