package tw.shounenwind.plurkconnection;

import com.google.common.base.Objects;

public class Param {
    public final String key;
    public final String value;

    public Param(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public Param(String key, long value) {
        this.key = key;
        this.value = value + "";
    }

    public Param(String key, int value) {
        this.key = key;
        this.value = value + "";
    }

    public Param(String key, boolean value) {
        this.key = key;
        this.value = (value) ? "true" : "false";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key, value);
    }

    @Override
    public String toString() {
        return key + ", " + value;
    }
}