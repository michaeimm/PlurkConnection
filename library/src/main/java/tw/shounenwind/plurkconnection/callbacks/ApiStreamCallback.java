package tw.shounenwind.plurkconnection.callbacks;

import okhttp3.Response;
import okhttp3.ResponseBody;
import tw.shounenwind.plurkconnection.responses.ApiResponseStream;

public abstract class ApiStreamCallback extends BasePlurkCallback<ApiResponseStream> {


    @Override
    public final void runResult(Response response) throws Exception {
        super.runResult(response);
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
        onSuccess(result);
    }
}
