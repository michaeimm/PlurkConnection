package tw.shounenwind.plurkconnection;

import android.util.Log;

public class PlurkConnectionException extends Exception {

    private final ApiResponse apiResponse;

    public PlurkConnectionException(ApiResponse response) {
        super();
        this.apiResponse = response;
    }

    public ApiResponse getApiResponse() {
        return apiResponse;
    }

    @Override
    public void printStackTrace() {
        super.printStackTrace();
        Log.w("API response", apiResponse.toString());
    }
}
