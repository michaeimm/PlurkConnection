package tw.shounenwind.plurkconnection.callbacks;

import tw.shounenwind.plurkconnection.responses.ApiResponseString;

public class ApiStringCallback implements ICallback<ApiResponseString> {

    @Override
    public void onSuccess(ApiResponseString response) throws Exception {

    }

    public void onRetry(long retryTimes, long totalTimes) {

    }

    public void onError(Throwable e) {

    }
}
