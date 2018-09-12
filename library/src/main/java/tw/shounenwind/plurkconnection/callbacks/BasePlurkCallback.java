package tw.shounenwind.plurkconnection.callbacks;

import java.net.HttpURLConnection;

import okhttp3.Response;
import tw.shounenwind.plurkconnection.PlurkConnectionException;
import tw.shounenwind.plurkconnection.responses.ApiResponseString;
import tw.shounenwind.plurkconnection.responses.IResponse;

public abstract class BasePlurkCallback<T extends IResponse> {

    protected abstract void onSuccess(T parsedResponse) throws Exception;

    public void onRetry(long retryTimes, long totalTimes) {

    }

    public void onError(Throwable e) {

    }

    public void runResult(Response response) throws Exception{
        if(response.code() != HttpURLConnection.HTTP_OK){
            PlurkConnectionException exception = new PlurkConnectionException(
                    new ApiResponseString(response.code(), response.body().string())
            );
            response.close();
            throw exception;
        }
    }

}
