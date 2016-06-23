package tw.shounenwind;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.http.HttpParameters;

public class PlurkConnection {

    private static final int DEFAULT_TIMEOUT = 10000;
    private static final String PREFIX = "www.plurk.com/APP/";
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final String BOUNDARY = "==================================";
    private static final String HYPHENS = "--";
    private static final String CRLF = "\r\n";

    private String response;
    private int statusCode;
    private int timeout;
    private OAuthConsumer consumer;
    private boolean useHttps;
    private HashMap<String, String> params;

    protected PlurkConnection(String APP_KEY, String APP_SECRET, String token, String token_secret, boolean useHttps) {
        timeout = DEFAULT_TIMEOUT;
        this.useHttps = useHttps;
        consumer = new DefaultOAuthConsumer(APP_KEY, APP_SECRET);
        consumer.setTokenWithSecret(token, token_secret);
        params = new HashMap<String, String>();
    }

    public PlurkConnection(String APP_KEY, String APP_SECRET, boolean useHttps) {
        timeout = DEFAULT_TIMEOUT;
        this.useHttps = useHttps;
        consumer = new DefaultOAuthConsumer(APP_KEY, APP_SECRET);
    }

    public void addParam(String name, String value) {
        params.put(name, value);
    }

    public void startConnect(String uri) throws Exception {
        HttpURLConnection urlConnection = getHttpURLConnection(uri);
        urlConnection.setRequestMethod("POST");
        urlConnection.setUseCaches(false);

        StringBuilder sb = new StringBuilder("");
        HttpParameters hp = new HttpParameters();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            hp.put(OAuth.percentEncode(key), OAuth.percentEncode(value));
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(OAuth.percentEncode(key));
            sb.append("=");
            sb.append(OAuth.percentEncode(value));
        }
        consumer.setAdditionalParameters(hp);
        consumer.sign(urlConnection);

        String formEncoded = sb.toString();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8");
        outputStreamWriter.write(formEncoded);
        outputStreamWriter.close();

        responseStreamToString(urlConnection);
    }

    public void startConnect(String uri, File imageFile, String imageName) throws Exception {

        HttpURLConnection urlConnection = getHttpURLConnection(uri, 180000);
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);
        urlConnection.setRequestMethod("POST");
        urlConnection.setUseCaches(false);
        urlConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
        String fileType = "Content-Type: " + MimeTypeMap.getFileExtensionFromUrl(imageFile.getPath());

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
        Log.d("Bytes", iBytesAvailable + "");
        if (imageFile.getName().substring(imageFile.getName().length() - 3).toLowerCase(Locale.US).equals("jpg")) {
            if (iBytesAvailable > 10485760) {
                fileInputStream.close();
                uploadWithCompress(imageFile.getPath(), dataOS, iBytesAvailable, getOrientationMatrix(imageFile.getPath()));
            } else {
                uploadNoCompress(iBytesAvailable, fileInputStream, dataOS);
                fileInputStream.close();
            }
        } else if (imageFile.getName().substring(imageFile.getName().length() - 3).toLowerCase(Locale.US).equals("png")) {
            if (iBytesAvailable > 10485760) {
                fileInputStream.close();
                uploadWithCompress(imageFile.getPath(), dataOS, iBytesAvailable, null);
            } else {
                uploadNoCompress(iBytesAvailable, fileInputStream, dataOS);
                fileInputStream.close();
            }
        } else {
            uploadNoCompress(iBytesAvailable, fileInputStream, dataOS);
            fileInputStream.close();
        }
        dataOS.writeBytes(CRLF);
        dataOS.writeBytes(HYPHENS + BOUNDARY + HYPHENS);
        dataOS.flush();
        dataOS.close();

        responseStreamToString(urlConnection);

        urlConnection.disconnect();
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

    private void uploadWithCompress(String imagePath, DataOutputStream dataOS, long iBytesAvailable, Matrix matrix) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        newOpts.inSampleSize = (int) Math.floor(iBytesAvailable / 10485760); //10MB
        newOpts.inJustDecodeBounds = false;

        Bitmap bt = BitmapFactory.decodeFile(imagePath, newOpts);
        if(matrix != null){
            bt = Bitmap.createBitmap(bt, 0, 0, bt.getWidth(), bt.getHeight(), matrix, true);
        }
        bt.compress(Bitmap.CompressFormat.JPEG, 80, bos);

        bos.writeTo(dataOS);

        bos.flush();
        bos.close();
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

    private void responseStreamToString(HttpURLConnection urlConnection) throws Exception {
        StringBuilder stringBuilder;
        BufferedReader reader;
        statusCode = urlConnection.getResponseCode();
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


        response = stringBuilder.toString();
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

    private List<Protocol> protocols() {
        return Arrays.asList(Protocol.HTTP_2, Protocol.SPDY_3, Protocol.HTTP_1_1);
    }

    private HttpURLConnection getHttpURLConnection(String uri) throws MalformedURLException {
        return getHttpURLConnection(uri, this.timeout);
    }

    private HttpURLConnection getHttpURLConnection(String uri, int timeout) throws MalformedURLException {
        URL url = new URL((useHttps ? HTTPS : HTTP) + PREFIX + uri);
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.setProxy(Proxy.NO_PROXY);
        okHttpClient.setProtocols(protocols());
        if (timeout != 0) {
            okHttpClient.setReadTimeout((long) timeout, TimeUnit.MILLISECONDS);
            okHttpClient.setConnectTimeout((long) timeout, TimeUnit.MILLISECONDS);
            okHttpClient.setWriteTimeout((long) timeout, TimeUnit.MILLISECONDS);
        }
        OkUrlFactory factory = new OkUrlFactory(okHttpClient);
        return factory.open(url);
    }
}
