package tw.shounenwind.plurkconnection.callbacks;

import tw.shounenwind.plurkconnection.responses.IResponse;

public interface ICallback<T extends IResponse> {
    void onSuccess(T response) throws Exception;

    void onRetry(long retryTimes, long totalTimes);

    void onError(Throwable e);
}
