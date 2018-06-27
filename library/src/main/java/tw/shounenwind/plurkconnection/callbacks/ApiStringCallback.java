package tw.shounenwind.plurkconnection.callbacks;

import okhttp3.Response;
import okhttp3.ResponseBody;
import tw.shounenwind.plurkconnection.responses.ApiResponseString;

public abstract class ApiStringCallback extends BasePlurkCallback<ApiResponseString> {


    @Override
    public final void runResult(Response response) throws Exception {
        super.runResult(response);
        ResponseBody body = null;
        ApiResponseString result;
        try {
            body = response.body();
            result = new ApiResponseString(response.code(), body.string());
        } finally {
            if (body != null) {
                body.close();
            }
        }
        onSuccess(result);
    }
}
