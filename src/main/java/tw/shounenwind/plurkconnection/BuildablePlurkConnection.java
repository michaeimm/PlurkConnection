package tw.shounenwind.plurkconnection;

import android.content.Context;

import java.io.File;
import java.net.HttpURLConnection;

public class BuildablePlurkConnection extends PlurkConnection {

    public BuildablePlurkConnection(String APP_KEY, String APP_SECRET, String token, String token_secret) {
        super(APP_KEY, APP_SECRET, token, token_secret);
    }

    public BuildablePlurkConnection(String APP_KEY, String APP_SECRET) {
        super(APP_KEY, APP_SECRET);
    }

    public Builder builder(Context mContext) {
        return new Builder(mContext, this);
    }

    public class Builder {

        private Context mContext;
        private BuildablePlurkConnection mHealingPlurkConnection;
        private String target;
        private Param[] params;
        private ICallback callback;
        private int retryTimes;
        private RetryCheck retryCheck;

        public Builder(Context mContext, BuildablePlurkConnection healingPlurkConnection) {
            this.mContext = mContext;
            mHealingPlurkConnection = healingPlurkConnection;
            params = new Param[]{};
            retryTimes = 1;
        }

        public Builder setRetryTimes(int retryTimes) {
            this.retryTimes = retryTimes;
            return this;
        }

        public Builder setTarget(String target) {
            this.target = target;
            return this;
        }

        public Builder setParams(Param[] params) {
            this.params = params;
            return this;
        }

        public Builder setParam(Param param) {
            this.params = new Param[]{param};
            return this;
        }

        public Builder setCallback(APICallback callback) {
            this.callback = callback;
            return this;
        }

        public Builder setRetryCheck(RetryCheck retryCheck) {
            this.retryCheck = retryCheck;
            return this;
        }

        public void call() {

            NewThreadRetryExecutor.run(mContext, retryTimes, 1000, new NewThreadRetryExecutor.Tasks() {
                        @Override
                        public void mainTask() throws Exception {
                            ApiResponse response = mHealingPlurkConnection.startConnect(target, params);
                            if (response.statusCode != HttpURLConnection.HTTP_OK) {
                                throw new PlurkConnectionException(response);
                            }
                            if (callback != null)
                                callback.onSuccess(response);
                        }

                        @Override
                        public void onRetry(int retryTimes, int totalTimes) {
                            if (callback != null)
                                callback.onRetry(retryTimes, totalTimes);
                        }

                        @Override
                        public void onError(Exception e) {
                            if (callback != null)
                                callback.onError(e);
                        }
                    }, retryCheck
            );

        }

        public void upload(final File imageFile, final String fileName) {

            NewThreadRetryExecutor.run(mContext, retryTimes, 1000, new NewThreadRetryExecutor.Tasks() {

                        @Override
                        public void mainTask() throws Exception {
                            ApiResponse response = mHealingPlurkConnection.startConnect(target, imageFile, fileName);
                            if (response.statusCode != HttpURLConnection.HTTP_OK) {
                                throw new PlurkConnectionException(response);
                            }
                            if (callback != null)
                                callback.onSuccess(response);
                        }

                        @Override
                        public void onRetry(int retryTimes, int totalTimes) {
                            if (callback != null) {
                                callback.onRetry(retryTimes, totalTimes);
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            if (callback != null)
                                callback.onError(e);
                        }
                    }, retryCheck
            );

        }
    }
}
