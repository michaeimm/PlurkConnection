package tw.shounenwind.plurkconnection.callbacks;

import tw.shounenwind.plurkconnection.responses.IResponse;

public interface ICallback<T extends IResponse> {
    void onSuccess(T response) throws Exception;

    default void onRetry(long retryTimes, long totalTimes) {

    }

    default void onError(Throwable e) {

    }
}
