package tw.shounenwind.plurkconnection;

public interface ICallback {
    void onSuccess(ApiResponse response) throws Exception;

    void onRetry(long retryTimes, long totalTimes);

    void onError(Throwable e);
}
