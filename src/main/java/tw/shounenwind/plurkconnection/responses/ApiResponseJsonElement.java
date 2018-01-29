package tw.shounenwind.plurkconnection.responses;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.apache.commons.lang3.StringEscapeUtils;

public class ApiResponseJsonElement implements IResponse<JsonElement> {
    public final JsonElement content;
    public final int statusCode;
    private Throwable e;

    public ApiResponseJsonElement(int statusCode, JsonElement content) {
        this.statusCode = statusCode;
        this.content = content;
    }

    public ApiResponseJsonElement(Throwable e) {
        this.statusCode = -1;
        this.content = new JsonParser().parse("{error_text: \"" + StringEscapeUtils.escapeJson(e.toString()) + "\"}");
        this.e = e;
    }

    public Throwable getE() {
        return e;
    }

    @Override
    public String toString() {
        try {
            return statusCode + ", " + (content == null ? "" : content.toString());
        } catch (Exception e1) {
            return statusCode + ", " + e1.toString();
        }
    }

    @Override
    public JsonElement getContent() {
        return content;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }
}
