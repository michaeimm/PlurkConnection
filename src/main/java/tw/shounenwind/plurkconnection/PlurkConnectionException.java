package tw.shounenwind.plurkconnection;

import tw.shounenwind.plurkconnection.responses.IResponse;

public class PlurkConnectionException extends Exception {

    private final IResponse apiResponse;

    public PlurkConnectionException(IResponse response) {
        super(response.toString());
        this.apiResponse = response;
    }

    public PlurkConnectionException(IResponse response, Exception e) {
        super(response.getStatusCode() + ": " + response.toString(), e);
        this.apiResponse = response;
    }

    public IResponse getApiResponseString() {
        return apiResponse;
    }

    @Override
    public void printStackTrace() {
        super.printStackTrace();
    }
}
