package tw.shounenwind.plurkconnection.responses

class ApiResponseNull : IResponse<String> {
    override val content: String
    override val statusCode: Int
    val e: Throwable?

    constructor(statusCode: Int) {
        this.statusCode = statusCode
        this.content = ""
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
