package tw.shounenwind.plurkconnection.callbacks;

import okhttp3.Response;
import okhttp3.ResponseBody;
import tw.shounenwind.plurkconnection.responses.ApiResponseNull;

public abstract class ApiNullCallback extends BasePlurkCallback<ApiResponseNull> {

    @Override
    public void runResult(Response response) throws Exception {
        super.runResult(response);
        ResponseBody body = response.body();
        if (body != null)
            body.close();
        onSuccess(new ApiResponseNull(response.code(), null));
    }
}
