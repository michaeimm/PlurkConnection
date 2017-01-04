package tw.shounenwind.plurkconnection;

public class Param {
    public final String key;
    public final String value;

    public Param(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return key + ", " + value;
    }
}