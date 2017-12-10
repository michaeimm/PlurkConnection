package tw.shounenwind.plurkconnection.callbacks;

import tw.shounenwind.plurkconnection.responses.ApiResponseJsonElement;

public class ApiJsonElementCallback implements ICallback<ApiResponseJsonElement> {

    @Override
    public void onSuccess(ApiResponseJsonElement response) throws Exception {

    }

    public void onRetry(long retryTimes, long totalTimes) {

    }

    public void onError(Throwable e) {

    }
}
