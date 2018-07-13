package tw.shounenwind.plurkconnection;

import android.graphics.Bitmap;
import android.support.annotation.WorkerThread;
import android.webkit.MimeTypeMap;

import com.google.common.io.Files;

import java.io.File;
import java.net.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import oauth.signpost.OAuth;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import oauth.signpost.http.HttpParameters;
import okhttp3.CacheControl;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import se.akerfeldt.okhttp.signpost.OkHttpOAuthConsumer;
import se.akerfeldt.okhttp.signpost.SigningInterceptor;

public class PlurkConnection {

    private static final int MAX_IMAGE_SIZE = 1024;
    private static final String PREFIX = "www.plurk.com/APP/";
    private static final String HTTPS = "https://";
    private OkHttpClient normalOkHttpClient;
    private OkHttpClient imageUploadOkHttpClient;
    private String app_key;
    private String app_secret;
    private String token;
    private String token_secret;

    private DefaultOAuthProvider provider;
    private OkHttpOAuthConsumer consumer;

    public PlurkConnection(String APP_KEY, String APP_SECRET, String token, String token_secret) {
        this(APP_KEY, APP_SECRET);
        this.token = token;
        this.token_secret = token_secret;
        consumer.setTokenWithSecret(token, token_secret);
    }

    public PlurkConnection(String APP_KEY, String APP_SECRET) {
        this.app_key = APP_KEY;
        this.app_secret = APP_SECRET;
        consumer = new OkHttpOAuthConsumer(APP_KEY, APP_SECRET);
        provider = new DefaultOAuthProvider(
                "https://www.plurk.com/OAuth/request_token",
                "https://www.plurk.com/OAuth/access_token",
                "https://www.plurk.com/m/authorize"
        );
    }

    public void setNormalOkHttpClient(OkHttpClient normalOkHttpClient) {
        synchronized (PlurkConnection.class) {
            this.normalOkHttpClient = normalOkHttpClient;
        }
    }

    public void setImageUploadOkHttpClient(OkHttpClient imageUploadOkHttpClient) {
        synchronized (PlurkConnection.class) {
            this.imageUploadOkHttpClient = imageUploadOkHttpClient;
        }
    }

    public Response startConnect(String uri) throws Exception {
        return startConnect(uri, new Param[]{});
    }

    public Response startConnect(String uri, Param param) throws Exception {
        return startConnect(uri, new Param[]{param});
    }

    private void checkLinkExist() {
        synchronized (PlurkConnection.class) {
            if (normalOkHttpClient == null) {
                normalOkHttpClient = Tls12SocketFactory.enableTls12OnPreLollipop(
                        new OkHttpClient.Builder()
                                .protocols(getProtocols())
                                .proxy(Proxy.NO_PROXY)
                                .connectTimeout(8, TimeUnit.SECONDS)
                                .readTimeout(20, TimeUnit.SECONDS)
                                .writeTimeout(20, TimeUnit.SECONDS)
                                .addInterceptor(new SigningInterceptor(consumer))
                ).build();
            }
            if (imageUploadOkHttpClient == null) {
                imageUploadOkHttpClient = Tls12SocketFactory.enableTls12OnPreLollipop(
                        new OkHttpClient.Builder()
                                .protocols(getProtocols())
                                .proxy(Proxy.NO_PROXY)
                                .connectTimeout(10, TimeUnit.SECONDS)
                                .readTimeout(60, TimeUnit.SECONDS)
                                .writeTimeout(180, TimeUnit.SECONDS)
                                .addInterceptor(new SigningInterceptor(consumer))
                ).build();
            }
        }
    }

    @WorkerThread
    public Response startConnect(String uri, Param[] params) throws Exception {
        checkLinkExist();
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        HttpParameters httpParameters = new HttpParameters();
        OkHttpOAuthConsumer consumer = getNewConsumer();

        for (Param param : params) {
            httpParameters.put(OAuth.percentEncode(param.key), OAuth.percentEncode(param.value));
            formBodyBuilder.add(param.key, param.value);
        }

        RequestBody requestBody = formBodyBuilder.build();
        consumer.setAdditionalParameters(httpParameters);


        Request request = new Request.Builder()
                .cacheControl(
                        new CacheControl.Builder()
                                .noCache()
                                .build()
                ).url(HTTPS + PREFIX + uri)
                .post(requestBody)
                .build();

        return normalOkHttpClient.newCall(
                (Request) consumer.sign(request).unwrap()
        ).execute();

    }

    @WorkerThread
    public Response startConnect(String uri, File imageFile, String imageName) throws Exception{
        return startConnect(uri, imageFile, imageName, Bitmap.CompressFormat.PNG);
    }

    @WorkerThread
    public Response startConnect(String uri, File imageFile, String imageName, Bitmap.CompressFormat compressFormat) throws Exception {
        checkLinkExist();
        if (!imageFile.exists())
            throw new IllegalArgumentException("The image file is not exist.");

        OkHttpOAuthConsumer consumer = getNewConsumer();

        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        String format = Files.getFileExtension(imageFile.getName()).toLowerCase(Locale.ENGLISH);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(format);
        if (mimeType == null) {
            if (compressFormat.equals(Bitmap.CompressFormat.JPEG)){
                mimeType = "image/jpeg";
            } else if (compressFormat.equals(Bitmap.CompressFormat.PNG)){
                mimeType = "image/png";
            } else {
                throw new Exception("MimeType is null. File name: " + imageFile.getName());
            }
        }
        MediaType parsedMimeType = MediaType.parse(
                mimeType
        );
        requestBodyBuilder.addFormDataPart(imageName,
                imageName,
                RequestBody.create(
                        parsedMimeType,
                        imageFile
                )
        );

        Request request = new Request.Builder()
                .cacheControl(
                        new CacheControl.Builder()
                                .noCache()
                                .build()
                ).url(HTTPS + PREFIX + uri)
                .post(requestBodyBuilder.build())
                .build();

        Request signedRequest = (Request) consumer.sign(request).unwrap();

        return imageUploadOkHttpClient.newCall(signedRequest).execute();

    }

    private OkHttpOAuthConsumer getNewConsumer() {
        OkHttpOAuthConsumer consumer = new OkHttpOAuthConsumer(app_key, app_secret);
        consumer.setTokenWithSecret(token, token_secret);
        return consumer;
    }

    public DefaultOAuthProvider getProvider() {
        return provider;
    }

    public String retrieveRequestToken(String callbackUrl, String... customOAuthParams) throws OAuthCommunicationException, OAuthExpectationFailedException, OAuthNotAuthorizedException, OAuthMessageSignerException {
        return provider.retrieveRequestToken(consumer, callbackUrl, customOAuthParams);
    }

    public void retrieveAccessToken(String verifier) throws OAuthCommunicationException, OAuthExpectationFailedException, OAuthNotAuthorizedException, OAuthMessageSignerException {
        provider.retrieveAccessToken(consumer, verifier);
    }

    public String getToken() {
        return consumer.getToken();
    }

    public String getTokenSecret() {
        return consumer.getTokenSecret();
    }

    private List<Protocol> getProtocols() {
        return Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1);
    }

}