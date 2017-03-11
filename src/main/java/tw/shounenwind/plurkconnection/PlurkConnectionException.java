package tw.shounenwind.plurkconnection;

public class PlurkConnectionException extends Exception {

    private final ApiResponse apiResponse;

    public PlurkConnectionException(ApiResponse response) {
        super(response.statusCode + ": " + response.content);
        this.apiResponse = response;
    }

    public ApiResponse getApiResponse() {
        return apiResponse;
    }

    @Override
    public void printStackTrace() {
        super.printStackTrace();
    }
}
