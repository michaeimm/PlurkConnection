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
import se.akerfeldt.okhttp.signpost.OkHttpOAuthConsumer;
import se.akerfeldt.okhttp.signpost.SigningInterceptor;
import tw.shounenwind.plurkconnection.responses.ApiResponse;

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

    public ApiResponse startConnect(String uri) throws Exception {
        return startConnect(uri, new Param[]{});
    }

    public ApiResponse startConnect(String uri, Param param) throws Exception {
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
    public ApiResponse startConnect(String uri, Param[] params) throws Exception {
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

        return new ApiResponse(normalOkHttpClient.newCall(
                (Request) consumer.sign(request).unwrap()
        ).execute());

    }

    @WorkerThread
    public ApiResponse startConnect(String uri, File imageFile, String imageName) throws Exception {
        checkLinkExist();
        if (!imageFile.exists())
            throw new IllegalArgumentException("The image file is not exist.");

        OkHttpOAuthConsumer consumer = getNewConsumer();

        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        String format = Files.getFileExtension(imageFile.getName()).toLowerCase(Locale.ENGLISH);
        if (isNeedCompress(format)) {
            try {
                requestBodyBuilder.addFormDataPart(imageName,
                        imageName,
                        RequestBody.create(
                                MediaType.parse("image/png"),
                                getCompressByteArray(
                                        imageFile
                                )
                        )
                );
            } catch (OutOfMemoryError e) {
                System.gc();
                throw e;
            }
        } else {
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(format);
            if (mimeType == null) {
                throw new Exception("MimeType is null. File name: " + imageFile.getName());
            }
            MediaType parsedMimeType = MediaType.parse(
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(format)
            );
            requestBodyBuilder.addFormDataPart(imageName,
                    imageName,
                    RequestBody.create(
                            parsedMimeType,
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

        return new ApiResponse(imageUploadOkHttpClient.newCall(signedRequest).execute());

    }

    private byte[] getCompressByteArray(File imageFile) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Bitmap oldBitmap = getBitmapWithCompressOption(imageFile);
        Matrix matrix = getPictureOrientationMatrix(imageFile);

        Bitmap newBitmap = Bitmap.createBitmap(oldBitmap, 0, 0,
                oldBitmap.getWidth(), oldBitmap.getHeight(), matrix, true);

        if (!oldBitmap.equals(newBitmap)) {
            oldBitmap.recycle();
        }

        newBitmap.compress(Bitmap.CompressFormat.PNG, 75, bos);

        byte[] result = bos.toByteArray();

        bos.flush();
        bos.close();
        newBitmap.recycle();
        System.gc();
        return result;
    }

    private boolean isNeedCompress(String format) {
        return (format.equals("jpg") || format.equals("jpeg") || format.equals("png"));
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

    private Bitmap getBitmapWithCompressOption(File imageFile) throws Exception {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();

        newOpts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFile.getPath(), newOpts);

        newOpts.inSampleSize = calculateInSampleSize(newOpts);

        newOpts.inPurgeable = true;
        newOpts.inJustDecodeBounds = false;
        newOpts.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeFile(imageFile.getAbsolutePath(), newOpts);
    }

    private int calculateInSampleSize(BitmapFactory.Options options) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;

        int inSampleSize = 1;

        if (height > MAX_IMAGE_SIZE || width > MAX_IMAGE_SIZE) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= MAX_IMAGE_SIZE && (halfWidth / inSampleSize) >= MAX_IMAGE_SIZE) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
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