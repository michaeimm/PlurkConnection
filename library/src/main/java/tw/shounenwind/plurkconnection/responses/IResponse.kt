package tw.shounenwind.plurkconnection.responses

interface IResponse<T> {
    val content: T

    val statusCode: Int
}
