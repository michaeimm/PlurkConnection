package tw.shounenwind.plurkconnection.responses;

import com.google.common.io.CharStreams;

import java.io.Reader;

public class ApiResponseStream implements IResponse<Reader> {
    public final Reader content;
    public final int statusCode;
    private Throwable e;

    public ApiResponseStream(int statusCode, Reader content) {
        this.statusCode = statusCode;
        this.content = content;
    }

    public ApiResponseStream(Throwable e) {
        this.statusCode = -1;
        this.content = null;
        this.e = e;
    }

    public Throwable getE() {
        return e;
    }

    @Override
    public String toString() {
        try {
            return statusCode + ", " + (content == null ? "" : CharStreams.toString(content));
        } catch (Exception e1) {
            return statusCode + ", " + e1.toString();
        }
    }

    @Override
    public Reader getContent() {
        return content;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }
}
