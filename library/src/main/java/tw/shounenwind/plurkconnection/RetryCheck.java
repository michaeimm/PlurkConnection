package tw.shounenwind.plurkconnection;

public interface RetryCheck {
    boolean isNeedRetry(Throwable e);
}
