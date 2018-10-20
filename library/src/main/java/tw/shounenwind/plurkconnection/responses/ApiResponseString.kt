package tw.shounenwind.plurkconnection.responses

class ApiResponseString : IResponse<String> {
    override val content: String
    override val statusCode: Int
    val e: Throwable?

    constructor(statusCode: Int, content: String) {
        this.statusCode = statusCode
        this.content = content
        this.e = null
    }

    constructor(e: Throwable) {
        this.statusCode = -1
        this.content = e.toString()
        this.e = e
    }

    override fun toString(): String {
        return statusCode.toString() + ", " + content
    }
}
