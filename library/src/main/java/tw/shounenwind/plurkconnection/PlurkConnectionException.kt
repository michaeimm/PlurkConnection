package tw.shounenwind.plurkconnection

class PlurkConnectionException : Exception {


    constructor(target: String) : super(target)
    constructor(target: String, message: String) : super("$target, $message")
    constructor(target: String, message: String, cause: Throwable) : super("$target, $message", cause)
    constructor(target: String, cause: Throwable) : super(target, cause)


}