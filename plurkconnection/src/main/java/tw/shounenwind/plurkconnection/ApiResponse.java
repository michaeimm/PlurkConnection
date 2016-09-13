package tw.shounenwind.plurkconnection;

public class ApiResponse {
    public final String content;
    public final int statusCode;

    public ApiResponse(int statusCode, String content) {
        this.statusCode = statusCode;
        this.content = content;
    }

    @Override
    public String toString() {
        return statusCode + ", " + content;
    }
}
