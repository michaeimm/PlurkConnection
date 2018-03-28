package tw.shounenwind.plurkconnection.responses;

public interface IResponse<T> {
    T getContent();

    int getStatusCode();
}
