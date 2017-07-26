package tw.shounenwind.plurkconnection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.support.annotation.WorkerThread;
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

    private static final int MAX_IMAGE_SIZE = 8 * 1024 * 1024; //8MB
    private static final String PREFIX = "www.plurk.com/APP/";
    private static final String HTTPS = "https://";
    private final OkHttpClient normalOkHttpClient;
    private final OkHttpClient imageUploadOkHttpClient;
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
        normalOkHttpClient = Tls12SocketFactory.enableTls12OnPreLollipop(
                new OkHttpClient.Builder()
                        .protocols(getProtocols())
                        .proxy(Proxy.NO_PROXY)
                        .connectTimeout(8, TimeUnit.SECONDS)
                        .readTimeout(20, TimeUnit.SECONDS)
                        .writeTimeout(20, TimeUnit.SECONDS)
                        .addInterceptor(new SigningInterceptor(consumer))
        ).build();
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

    public ApiResponse startConnect(String uri) throws Exception {
        return startConnect(uri, new Param[]{});
    }

    public ApiResponse startConnect(String uri, Param param) throws Exception {
        return startConnect(uri, new Param[]{param});
    }

    @WorkerThread
    public ApiResponse startConnect(String uri, Param[] params) throws Exception {
        Response response = null;
        try {
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

            response = normalOkHttpClient.newCall(
                    (Request) consumer.sign(request).unwrap()
            ).execute();

            ApiResponse result = new ApiResponse(response.code(), response.body().string());

            response.close();

            return result;
        } catch (Exception e) {
            if (response != null) {
                response.close();
            }
            return new ApiResponse(500, e.toString());
        }
    }

    @WorkerThread
    public ApiResponse startConnect(String uri, File imageFile, String imageName) throws Exception {
        if (!imageFile.exists())
            throw new IllegalArgumentException("The image file is not exist.");
        Response response = null;
        try {
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

            Request request = new Request.Builder()
                    .cacheControl(
                            new CacheControl.Builder()
                                    .noCache()
                                    .build()
                    ).url(HTTPS + PREFIX + uri)
                    .post(requestBodyBuilder.build())
                    .build();

            Request signedRequest = (Request) consumer.sign(request).unwrap();

            response = imageUploadOkHttpClient.newCall(signedRequest).execute();
            ApiResponse result = new ApiResponse(response.code(), response.body().string());

            response.close();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            if (response != null) {
                response.close();
            }
            return new ApiResponse(500, e.toString());

        }

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
        OkHttpOAuthConsumer consumer = new OkHttpOAuthConsumer(app_key, app_secret);
        consumer.setTokenWithSecret(token, token_secret);
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
        return Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1);
    }
}