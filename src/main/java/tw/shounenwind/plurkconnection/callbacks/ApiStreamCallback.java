package tw.shounenwind.plurkconnection.callbacks;

import tw.shounenwind.plurkconnection.responses.ApiResponseStream;

public class ApiStreamCallback implements ICallback<ApiResponseStream> {

    @Override
    public void onSuccess(ApiResponseStream response) throws Exception {

    }

    public void onRetry(long retryTimes, long totalTimes) {

    }

    public void onError(Throwable e) {

    }
}
