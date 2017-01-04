package tw.shounenwind.plurkconnection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.webkit.MimeTypeMap;

import com.google.common.io.Files;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

    private static final int MAX_IMAGE_SIZE = 10 * 1024 * 1024; //10MB
    private static final String PREFIX = "www.plurk.com/APP/";
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";

    private DefaultOAuthProvider provider;
    private OkHttpOAuthConsumer consumer;
    private boolean useHttps;

    public PlurkConnection(String APP_KEY, String APP_SECRET, String token, String token_secret, boolean useHttps) {
        this(APP_KEY, APP_SECRET, useHttps);
        consumer.setTokenWithSecret(token, token_secret);
    }

    public PlurkConnection(String APP_KEY, String APP_SECRET, boolean useHttps) {
        this.useHttps = useHttps;
        consumer = new OkHttpOAuthConsumer(APP_KEY, APP_SECRET);
        provider = new DefaultOAuthProvider(
                "https://www.plurk.com/OAuth/request_token",
                "https://www.plurk.com/OAuth/access_token",
                "https://www.plurk.com/m/authorize"
        );
    }

    public ApiResponse startConnect(String uri) throws Exception {
        return startConnect(uri, new Param[]{});
    }

    public ApiResponse startConnect(String uri, Param param) throws Exception {
        return startConnect(uri, new Param[]{param});
    }

    public ApiResponse startConnect(String uri, Param[] params) throws Exception {

        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        HttpParameters httpParameters = new HttpParameters();
        OkHttpOAuthConsumer consumer = getNewConsumer();

        for (Param param : params) {
            httpParameters.put(OAuth.percentEncode(param.key), OAuth.percentEncode(param.value));
            formBodyBuilder.add(param.key, param.value);
        }

        RequestBody requestBody = formBodyBuilder.build();
        consumer.setAdditionalParameters(httpParameters);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .protocols(getProtocols())
                .proxy(Proxy.NO_PROXY)
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(new SigningInterceptor(consumer)).build();
        Request request = new Request.Builder()
                .cacheControl(
                        new CacheControl.Builder()
                                .noCache()
                                .build()
                ).url((useHttps ? HTTPS : HTTP) + PREFIX + uri)
                .post(requestBody)
                .build();

        Response response = okHttpClient.newCall(
                (Request)consumer.sign(request).unwrap()
        ).execute();
        ApiResponse result = new ApiResponse(response.code(), response.body().string());

        response.close();

        return result;
    }

    public ApiResponse startConnect(String uri, File imageFile, String imageName) throws Exception {

        OkHttpOAuthConsumer consumer = getNewConsumer();

        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        FileInputStream fileInputStream = new FileInputStream(imageFile);
        int iBytesAvailable = fileInputStream.available();
        fileInputStream.close();

        String format = Files.getFileExtension(imageFile.getName()).toLowerCase(Locale.ENGLISH);
        if (isNeedCompress(format, iBytesAvailable)) {
            fileInputStream.close();
            requestBodyBuilder.addFormDataPart(imageName,
                    imageName,
                    RequestBody.create(
                            MediaType.parse("image/jpeg"),
                            getCompressByteArray(
                                    imageFile,
                                    iBytesAvailable
                            )
                    )
            );
        } else {
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(format);
            if (mimeType == null) {
                throw new Exception("MimeType is null. File name: " + imageFile.getName());
            }
            requestBodyBuilder.addFormDataPart(imageName,
                    imageName,
                    RequestBody.create(
                            MediaType.parse(MimeTypeMap.getSingleton().getMimeTypeFromExtension(format)),
                            imageFile
                    )
            );
        }

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .protocols(getProtocols())
                .proxy(Proxy.NO_PROXY)
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(180, TimeUnit.SECONDS)
                .addInterceptor(new SigningInterceptor(consumer)).build();
        Request request = new Request.Builder()
                .cacheControl(
                        new CacheControl.Builder()
                                .noCache()
                                .build()
                ).url((useHttps ? HTTPS : HTTP) + PREFIX + uri)
                .post(requestBodyBuilder.build())
                .build();

        Request signedRequest = (Request) consumer.sign(request).unwrap();

        Response response = okHttpClient.newCall(signedRequest).execute();
        ApiResponse result = new ApiResponse(response.code(), response.body().string());

        response.close();

        return result;
    }

    private byte[] getCompressByteArray(File imageFile, long iBytesAvailable) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Bitmap bt = getBitmapWithCompressOption(imageFile, iBytesAvailable);
        Matrix matrix = getPictureOrientationMatrix(imageFile);
        bt = Bitmap.createBitmap(bt, 0, 0,
                bt.getWidth(), bt.getHeight(), matrix, true);
        bt.compress(Bitmap.CompressFormat.JPEG, 80, bos);

        byte[] result = bos.toByteArray();

        bos.flush();
        bos.close();
        return result;
    }

    private boolean isNeedCompress(String format, int iBytesAvailable) {
        return (format.equals("jpg") || format.equals("jpeg") || format.equals("png")) && iBytesAvailable > MAX_IMAGE_SIZE;
    }

    private OkHttpOAuthConsumer getNewConsumer() {
        OkHttpOAuthConsumer consumer = new OkHttpOAuthConsumer(this.consumer.getConsumerKey(), this.consumer.getConsumerSecret());
        consumer.setTokenWithSecret(this.consumer.getToken(), this.consumer.getTokenSecret());
        return consumer;
    }

    private Matrix getPictureOrientationMatrix(File imageFile) throws IOException {
        ExifInterface exifInterface = new ExifInterface(imageFile.getPath());

        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        int degree;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                degree = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                degree = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                degree = 270;
                break;
            default:
                degree = 0;
                break;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return matrix;
    }

    private Bitmap getBitmapWithCompressOption(File imageFile, long fileSize) {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();

        newOpts.inJustDecodeBounds = false;
        newOpts.inSampleSize = (int) Math.floor(fileSize / MAX_IMAGE_SIZE);
        return BitmapFactory.decodeFile(imageFile.getPath(), newOpts);
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
        return Arrays.asList(Protocol.HTTP_2, Protocol.SPDY_3, Protocol.HTTP_1_1);
    }
}