package tw.shounenwind.plurkconnection

import android.webkit.MimeTypeMap
import androidx.annotation.WorkerThread
import oauth.signpost.OAuth
import oauth.signpost.basic.DefaultOAuthProvider
import oauth.signpost.exception.OAuthCommunicationException
import oauth.signpost.exception.OAuthExpectationFailedException
import oauth.signpost.exception.OAuthMessageSignerException
import oauth.signpost.exception.OAuthNotAuthorizedException
import oauth.signpost.http.HttpParameters
import okhttp3.*
import se.akerfeldt.okhttp.signpost.OkHttpOAuthConsumer
import se.akerfeldt.okhttp.signpost.SigningInterceptor
import java.io.File
import java.net.Proxy
import java.util.*
import java.util.concurrent.TimeUnit

open class PlurkConnection(private val app_key: String, private val app_secret: String) {
    private var normalOkHttpClient: OkHttpClient? = null
    private var imageUploadOkHttpClient: OkHttpClient? = null
    var token: String? = null
        private set(value) {field = value}
        get() = consumer.token
    var tokenSecret: String? = null
        private set(value) {field = value}
        get() = consumer.tokenSecret

    private val provider: DefaultOAuthProvider = DefaultOAuthProvider(
            "https://www.plurk.com/OAuth/request_token",
            "https://www.plurk.com/OAuth/access_token",
            "https://www.plurk.com/m/authorize"
    )
    private val consumer: OkHttpOAuthConsumer = OkHttpOAuthConsumer(app_key, app_secret)

    private val newConsumer: OkHttpOAuthConsumer
        get() {
            val consumer = OkHttpOAuthConsumer(app_key, app_secret)
            consumer.setTokenWithSecret(token, this.tokenSecret)
            return consumer
        }

    private val protocols: List<Protocol>
        get() = Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1)

    constructor(APP_KEY: String, APP_SECRET: String, token: String, token_secret: String) : this(APP_KEY, APP_SECRET) {
        this.token = token
        this.tokenSecret = token_secret
        consumer.setTokenWithSecret(token, token_secret)
    }

    fun setNormalOkHttpClient(normalOkHttpClient: OkHttpClient) {
        synchronized(PlurkConnection::class.java) {
            this.normalOkHttpClient = normalOkHttpClient
        }
    }

    fun setImageUploadOkHttpClient(imageUploadOkHttpClient: OkHttpClient) {
        synchronized(PlurkConnection::class.java) {
            this.imageUploadOkHttpClient = imageUploadOkHttpClient
        }
    }

    @Throws(Exception::class)
    fun startConnect(uri: String): Response {
        return startConnect(uri, arrayOf())
    }

    @Throws(Exception::class)
    fun startConnect(uri: String, param: Param): Response {
        return startConnect(uri, arrayOf(param))
    }

    private fun checkLinkExist() {
        synchronized(PlurkConnection::class.java) {
            if (normalOkHttpClient == null) {
                normalOkHttpClient = Tls12SocketFactory.enableTls12OnPreLollipop(
                        OkHttpClient.Builder()
                                .protocols(protocols)
                                .proxy(Proxy.NO_PROXY)
                                .connectTimeout(8, TimeUnit.SECONDS)
                                .readTimeout(20, TimeUnit.SECONDS)
                                .writeTimeout(20, TimeUnit.SECONDS)
                                .addInterceptor(SigningInterceptor(consumer))
                ).build()
            }
            if (imageUploadOkHttpClient == null) {
                imageUploadOkHttpClient = Tls12SocketFactory.enableTls12OnPreLollipop(
                        OkHttpClient.Builder()
                                .protocols(protocols)
                                .proxy(Proxy.NO_PROXY)
                                .connectTimeout(10, TimeUnit.SECONDS)
                                .readTimeout(60, TimeUnit.SECONDS)
                                .writeTimeout(180, TimeUnit.SECONDS)
                                .addInterceptor(SigningInterceptor(consumer))
                ).build()
            }
        }
    }

    @WorkerThread
    @Throws(Exception::class)
    fun startConnect(uri: String, params: Array<Param>): Response {
        checkLinkExist()
        val formBodyBuilder = FormBody.Builder()
        val httpParameters = HttpParameters()
        val consumer = newConsumer

        for (param in params) {
            httpParameters.put(OAuth.percentEncode(param.key), OAuth.percentEncode(param.value))
            formBodyBuilder.add(param.key, param.value)
        }

        val requestBody = formBodyBuilder.build()
        consumer.setAdditionalParameters(httpParameters)


        val request = Request.Builder()
                .cacheControl(
                        CacheControl.Builder()
                                .noCache()
                                .build()
                ).url(HTTPS + PREFIX + uri)
                .post(requestBody)
                .build()

        return normalOkHttpClient!!.newCall(
                consumer.sign(request).unwrap() as Request
        ).execute()

    }

    @WorkerThread
    @Throws(Exception::class)
    fun startConnect(uri: String, imageFile: File, imageName: String): Response {
        checkLinkExist()
        if (!imageFile.exists())
            throw IllegalArgumentException("The image file is not exist.")

        val consumer = newConsumer

        val requestBodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)

        val format = getFileExtension(imageFile.name).toLowerCase(Locale.ENGLISH)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(format)
                ?: throw Exception("MimeType is null. File name: " + imageFile.name)
        val parsedMimeType = MediaType.parse(
                mimeType
        )
        requestBodyBuilder.addFormDataPart(imageName,
                imageName,
                RequestBody.create(
                        parsedMimeType,
                        imageFile
                )
        )

        val request = Request.Builder()
                .cacheControl(
                        CacheControl.Builder()
                                .noCache()
                                .build()
                ).url(HTTPS + PREFIX + uri)
                .post(requestBodyBuilder.build())
                .build()

        val signedRequest = consumer.sign(request).unwrap() as Request

        return imageUploadOkHttpClient!!.newCall(signedRequest).execute()

    }

    @Throws(OAuthCommunicationException::class, OAuthExpectationFailedException::class, OAuthNotAuthorizedException::class, OAuthMessageSignerException::class)
    fun retrieveRequestToken(callbackUrl: String, vararg customOAuthParams: String): String {
        return provider.retrieveRequestToken(consumer, callbackUrl, *customOAuthParams)
    }

    @Throws(OAuthCommunicationException::class, OAuthExpectationFailedException::class, OAuthNotAuthorizedException::class, OAuthMessageSignerException::class)
    fun retrieveAccessToken(verifier: String) {
        provider.retrieveAccessToken(consumer, verifier)
    }

    private fun getFileExtension(fileName: String): String{
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex == -1) "" else fileName.substring(dotIndex + 1)
    }

    companion object {

        private const val PREFIX = "www.plurk.com/APP/"
        private const val HTTPS = "https://"
    }

}