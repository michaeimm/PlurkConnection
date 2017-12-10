package tw.shounenwind.plurkconnection.callbacks;

import tw.shounenwind.plurkconnection.responses.ApiResponseNull;

public class ApiNullCallback implements ICallback<ApiResponseNull> {

    @Override
    public void onSuccess(ApiResponseNull response) throws Exception {

    }

    public void onRetry(long retryTimes, long totalTimes) {

    }

    public void onError(Throwable e) {

    }
}
