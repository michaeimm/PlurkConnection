package tw.shounenwind;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.webkit.MimeTypeMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import oauth.signpost.OAuth;
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

    private static final int DEFAULT_TIMEOUT = 10000;
    private static final String PREFIX = "www.plurk.com/APP/";
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final int COMPRESS_MAX_SIZE = 10485760; //10MB

    private String response;
    private int statusCode;
    private int timeout;
    private OkHttpOAuthConsumer consumer;
    private boolean useHttps;
    private HashMap<String, String> params;

    protected PlurkConnection(String APP_KEY, String APP_SECRET, String token, String token_secret, boolean useHttps) {
        timeout = DEFAULT_TIMEOUT;
        this.useHttps = useHttps;
        consumer = new OkHttpOAuthConsumer(APP_KEY, APP_SECRET);
        consumer.setTokenWithSecret(token, token_secret);
        params = new HashMap<String, String>();
    }

    public PlurkConnection(String APP_KEY, String APP_SECRET, boolean useHttps) {
        timeout = DEFAULT_TIMEOUT;
        this.useHttps = useHttps;
        consumer = new OkHttpOAuthConsumer(APP_KEY, APP_SECRET);
    }

    public void addParam(String name, String value) {
        params.put(name, value);
    }

    public void startConnect(String uri) throws Exception {

        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
        Request.Builder requestBuilder = new Request.Builder();
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        HttpParameters hp = new HttpParameters();

        okHttpClientBuilder
                .protocols(getProtocols())
                .proxy(Proxy.NO_PROXY)
                .addInterceptor(new SigningInterceptor(consumer));
        setTimeout(okHttpClientBuilder, timeout);

        requestBuilder.cacheControl(
                new CacheControl.Builder()
                        .noCache()
                        .build()
        ).url((useHttps ? HTTPS : HTTP) + PREFIX + uri);

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            hp.put(OAuth.percentEncode(key), OAuth.percentEncode(value));
            formBodyBuilder.add(key, value);
        }
        RequestBody requestBody = formBodyBuilder.build();
        requestBuilder.post(requestBody);
        consumer.setAdditionalParameters(hp);

        OkHttpClient okHttpClient = okHttpClientBuilder.build();
        Request request = requestBuilder.build();

        Request signedRequest = (Request) consumer.sign(request).unwrap();

        Response response = okHttpClient.newCall(signedRequest).execute();
        this.response = response.body().string();
        statusCode = response.code();

        response.close();

    }

    public void startConnect(String uri, final File imageFile, String imageName) throws Exception {

        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
        Request.Builder requestBuilder = new Request.Builder();

        okHttpClientBuilder
                .protocols(getProtocols())
                .proxy(Proxy.NO_PROXY)
                .addInterceptor(new SigningInterceptor(consumer));
        setTimeout(okHttpClientBuilder, 180000L);

        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        requestBuilder.cacheControl(
                new CacheControl.Builder()
                        .noCache()
                        .build()
        ).url((useHttps ? HTTPS : HTTP) + PREFIX + uri);

        FileInputStream fileInputStream = new FileInputStream(imageFile);
        int iBytesAvailable = fileInputStream.available();
        fileInputStream.close();

        String format = MimeTypeMap.getFileExtensionFromUrl(imageFile.getName());
        if (isNeedCompress(format, iBytesAvailable)) {
            fileInputStream.close();
            requestBodyBuilder.addFormDataPart("image",
                    imageName,
                    RequestBody.create(
                            MediaType.parse("image/jpeg"),
                            getCompressByteArray(
                                    imageFile.getPath(),
                                    iBytesAvailable,
                                    getOrientationMatrix(imageFile.getPath())
                            )
                    )
            );
        } else {
            requestBodyBuilder.addFormDataPart("image",
                    imageName,
                    RequestBody.create(
                            MediaType.parse(MimeTypeMap.getSingleton().getMimeTypeFromExtension(format)),
                            imageFile
                    )
            );
        }

        requestBuilder.post(requestBodyBuilder.build());
        OkHttpClient okHttpClient = okHttpClientBuilder.build();
        Request request = requestBuilder.build();

        Request signedRequest = (Request) consumer.sign(request).unwrap();

        Response response = okHttpClient.newCall(signedRequest).execute();
        this.response = response.body().string();
        statusCode = response.code();

        response.close();
    }

    public String getResponse() {
        return response;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int t) {
        timeout = t;
    }

    private boolean isNeedCompress(String format, int iBytesAvailable) {
        return (format.equals("jpg") || format.equals("jpeg") || format.equals("png")) && iBytesAvailable > COMPRESS_MAX_SIZE;
    }

    private byte[] getCompressByteArray(String imagePath, long iBytesAvailable, Matrix matrix) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        newOpts.inSampleSize = (int) Math.floor(iBytesAvailable / COMPRESS_MAX_SIZE);
        newOpts.inJustDecodeBounds = false;

        Bitmap bt = BitmapFactory.decodeFile(imagePath, newOpts);
        if (matrix != null) {
            bt = Bitmap.createBitmap(bt, 0, 0, bt.getWidth(), bt.getHeight(), matrix, true);
        }
        bt.compress(Bitmap.CompressFormat.JPEG, 80, bos);

        byte[] result = bos.toByteArray();

        bos.flush();
        bos.close();
        return result;
    }

    private Matrix getOrientationMatrix(String imagePath) throws IOException {
        ExifInterface exifInterface = new ExifInterface(imagePath);

        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        int degree = 0;
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
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return matrix;
    }

    private void setTimeout(OkHttpClient.Builder okHttpClientBuilder, long timeout) {
        if (timeout != 0) {
            okHttpClientBuilder
                    .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                    .readTimeout(timeout, TimeUnit.MILLISECONDS)
                    .writeTimeout(timeout, TimeUnit.MILLISECONDS);
        }
    }

    private List<Protocol> getProtocols() {
        return Arrays.asList(Protocol.HTTP_2, Protocol.SPDY_3, Protocol.HTTP_1_1);
    }
}
