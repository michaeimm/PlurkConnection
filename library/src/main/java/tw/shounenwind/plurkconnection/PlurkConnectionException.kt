package tw.shounenwind.plurkconnection

import tw.shounenwind.plurkconnection.responses.IResponse

class PlurkConnectionException : Exception {

    val apiResponseString: IResponse<*>

    constructor(response: IResponse<*>) : super(response.toString()) {
        this.apiResponseString = response
    }

    constructor(response: IResponse<*>, e: Exception) : super(response.statusCode.toString() + ": " + response.toString(), e) {
        this.apiResponseString = response
    }
}
