package tw.shounenwind.plurkconnection.responses;

public class ApiResponseNull implements IResponse<String> {
    public final String content;
    public final int statusCode;
    private Throwable e;

    public ApiResponseNull(int statusCode, String content) {
        this.statusCode = statusCode;
        this.content = "";
    }

    public ApiResponseNull(Throwable e) {
        this.statusCode = -1;
        this.content = e.toString();
        this.e = e;
    }

    public Throwable getE() {
        return e;
    }

    @Override
    public String toString() {
        return statusCode + ", " + content;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }
}
