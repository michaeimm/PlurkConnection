package tw.shounenwind.plurkconnection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.webkit.MimeTypeMap;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;
import com.squareup.okhttp.Protocol;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import oauth.signpost.http.HttpParameters;

public class PlurkConnection {

    private static final int MAX_IMAGE_SIZE = 10485760; //10MB
    private static final int DEFAULT_TIMEOUT = 10000;
    private static final String PREFIX = "www.plurk.com/APP/";
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final String BOUNDARY = "==================================";
    private static final String HYPHENS = "--";
    private static final String CRLF = "\r\n";

    private DefaultOAuthProvider provider;
    private int timeout;
    private OAuthConsumer consumer;
    private boolean useHttps;

    public PlurkConnection(String APP_KEY, String APP_SECRET, String token, String token_secret, boolean useHttps) {
        this(APP_KEY, APP_SECRET, useHttps);
        consumer.setTokenWithSecret(token, token_secret);
    }

    public PlurkConnection(String APP_KEY, String APP_SECRET, boolean useHttps) {
        timeout = DEFAULT_TIMEOUT;
        this.useHttps = useHttps;
        consumer = new DefaultOAuthConsumer(APP_KEY, APP_SECRET);
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
        HttpURLConnection urlConnection = getHttpURLConnection(uri);
        urlConnection.setRequestMethod("POST");
        urlConnection.setUseCaches(false);

        StringBuilder stringBuilder = new StringBuilder();
        HttpParameters httpParameters = new HttpParameters();
        for (Param param : params) {
            httpParameters.put(OAuth.percentEncode(param.key), OAuth.percentEncode(param.value));
            if (stringBuilder.length() > 0) {
                stringBuilder.append("&");
            }
            stringBuilder.append(OAuth.percentEncode(param.key));
            stringBuilder.append("=");
            stringBuilder.append(OAuth.percentEncode(param.value));
        }

        OAuthConsumer consumer = getNewConsumer();
        consumer.setAdditionalParameters(httpParameters);
        consumer.sign(urlConnection);

        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8");
        outputStreamWriter.write(stringBuilder.toString());
        outputStreamWriter.close();

        ApiResponse result = responseStreamToString(urlConnection);

        urlConnection.disconnect();

        return result;
    }

    public ApiResponse startConnect(String uri, File imageFile, String imageName) throws Exception {

        HttpURLConnection urlConnection = getHttpURLConnection(uri, 180000);
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);
        urlConnection.setRequestMethod("POST");
        urlConnection.setUseCaches(false);
        urlConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
        String fileType = "Content-Type: " + MimeTypeMap.getFileExtensionFromUrl(imageFile.getPath());

        OAuthConsumer consumer = getNewConsumer();
        consumer.sign(urlConnection);

        FileInputStream fileInputStream = new FileInputStream(imageFile);

        String strContentDisposition = "Content-Disposition: form-data; name=\"" + imageName + "\"; filename=\"" + imageName + "\"";
        String strContentType = "Content-Type: " + fileType;
        DataOutputStream dataOS = new DataOutputStream(urlConnection.getOutputStream());
        dataOS.writeBytes(HYPHENS + BOUNDARY + CRLF);
        dataOS.writeBytes(strContentDisposition + CRLF);
        dataOS.writeBytes(strContentType + CRLF);
        dataOS.writeBytes(CRLF);
        int iBytesAvailable = fileInputStream.available();
        String filenameExtension = getFilenameExtension(imageFile.getName());
        switch (filenameExtension) {
            case "jpg":
            case "jpeg":
                if (iBytesAvailable > MAX_IMAGE_SIZE) {
                    fileInputStream.close();
                    Bitmap bt = getBitmapWithCompressOption(imageFile, iBytesAvailable);
                    Matrix matrix = getPictureOrientationMartix(imageFile);
                    bt = Bitmap.createBitmap(bt, 0, 0,
                            bt.getWidth(), bt.getHeight(), matrix, true);

                    bitmapCompressAndWriteToStream(bt, dataOS);
                    break;
                }
            case "png":
                if (iBytesAvailable > MAX_IMAGE_SIZE) {
                    fileInputStream.close();
                    Bitmap bt = getBitmapWithCompressOption(imageFile, iBytesAvailable);

                    bitmapCompressAndWriteToStream(bt, dataOS);
                    break;
                }
            default:
                uploadNoCompress(iBytesAvailable, fileInputStream, dataOS);
                fileInputStream.close();
                break;
        }
        dataOS.writeBytes(CRLF);
        dataOS.writeBytes(HYPHENS + BOUNDARY + HYPHENS);
        dataOS.flush();
        dataOS.close();

        ApiResponse result = responseStreamToString(urlConnection);

        urlConnection.disconnect();

        return result;
    }

    private OAuthConsumer getNewConsumer() {
        OAuthConsumer consumer = new DefaultOAuthConsumer(this.consumer.getConsumerKey(), this.consumer.getConsumerSecret());
        consumer.setTokenWithSecret(this.consumer.getToken(), this.consumer.getTokenSecret());
        return consumer;
    }

    private String getFilenameExtension(String filename) {
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase(Locale.US);
    }

    private Matrix getPictureOrientationMartix(File imageFile) throws IOException {
        ExifInterface exifInterface = new ExifInterface(imageFile.getPath());

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

    private Bitmap getBitmapWithCompressOption(File imageFile, long fileSize) {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();

        newOpts.inJustDecodeBounds = false;
        newOpts.inSampleSize = (int) Math.floor(fileSize / MAX_IMAGE_SIZE);
        return BitmapFactory.decodeFile(imageFile.getPath(), newOpts);
    }

    private void bitmapCompressAndWriteToStream(Bitmap bt, DataOutputStream dataOS) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bt.compress(Bitmap.CompressFormat.JPEG, 80, bos);
        bos.writeTo(dataOS);
        bos.flush();
        bos.close();
    }

    private ApiResponse responseStreamToString(HttpURLConnection urlConnection) throws Exception {
        StringBuilder stringBuilder;
        BufferedReader reader;
        int statusCode = urlConnection.getResponseCode();
        if (statusCode == HttpURLConnection.HTTP_OK) {
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
        } else {
            reader = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream(), "UTF-8"));
        }

        String line;
        stringBuilder = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append("\n");
        }
        reader.close();


        String response = stringBuilder.toString();

        return new ApiResponse(statusCode, response);
    }

    private void uploadNoCompress(int iBytesAvailable, FileInputStream fileInputStream, DataOutputStream dataOS) throws IOException {
        int maxBufferSize = 1024;
        int bufferSize = Math.min(iBytesAvailable, maxBufferSize);
        byte[] byteData = new byte[bufferSize];
        int iBytesRead = fileInputStream.read(byteData, 0, bufferSize);
        while (iBytesRead > 0) {
            dataOS.write(byteData, 0, bufferSize);
            iBytesAvailable = fileInputStream.available();
            bufferSize = Math.min(iBytesAvailable, maxBufferSize);
            iBytesRead = fileInputStream.read(byteData, 0, bufferSize);
        }
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

    private HttpURLConnection getHttpURLConnection(String uri) throws MalformedURLException {
        return getHttpURLConnection(uri, this.timeout);
    }

    private HttpURLConnection getHttpURLConnection(String uri, int timeout) throws MalformedURLException {
        URL url = new URL((useHttps ? HTTPS : HTTP) + PREFIX + uri);
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.setProxy(Proxy.NO_PROXY);
        okHttpClient.setProtocols(getProtocols());
        if (timeout != 0) {
            okHttpClient.setReadTimeout((long) timeout, TimeUnit.MILLISECONDS);
            okHttpClient.setConnectTimeout((long) timeout, TimeUnit.MILLISECONDS);
            okHttpClient.setWriteTimeout((long) timeout, TimeUnit.MILLISECONDS);
        }
        OkUrlFactory factory = new OkUrlFactory(okHttpClient);
        return factory.open(url);
    }
}