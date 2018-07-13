package tw.shounenwind.plurkconnection;

import android.content.Context;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;

import tw.shounenwind.plurkconnection.callbacks.ApiNullCallback;
import tw.shounenwind.plurkconnection.callbacks.ApiStringCallback;
import tw.shounenwind.plurkconnection.callbacks.BasePlurkCallback;
import tw.shounenwind.plurkconnection.responses.ApiResponseNull;

public class BuildablePlurkConnection extends PlurkConnection {

    private static final String TAG = "BPC";
    private final NewThreadRetryExecutor retryExecutor;

    public BuildablePlurkConnection(String APP_KEY, String APP_SECRET, String token, String token_secret) {
        super(APP_KEY, APP_SECRET, token, token_secret);
        retryExecutor = new NewThreadRetryExecutor();
    }

    public BuildablePlurkConnection(String APP_KEY, String APP_SECRET) {
        super(APP_KEY, APP_SECRET);
        retryExecutor = new NewThreadRetryExecutor();
    }

    public void setThreadPool(ExecutorService threadPool) {
        retryExecutor.setThreadPool(threadPool);
    }

    public Builder builder(Context mContext) {
        return new Builder(mContext, this);
    }

    public class Builder {

        private WeakReference<Context> contextWeakReference;
        private BuildablePlurkConnection mHealingPlurkConnection;
        private String target;
        private Param[] params;
        private BasePlurkCallback callback;
        private int retryTimes;
        private RetryCheck retryCheck;

        public Builder(Context mContext, BuildablePlurkConnection healingPlurkConnection) {
            contextWeakReference = new WeakReference<>(mContext);
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

        public Builder setCallback(BasePlurkCallback callback) {
            this.callback = callback;
            return this;
        }

        public Builder setRetryCheck(RetryCheck retryCheck) {
            this.retryCheck = retryCheck;
            return this;
        }

        public void call() {

            retryExecutor.run(contextWeakReference.get(), retryTimes, 1000, new NewThreadRetryExecutor.Tasks() {
                        @Override
                        public void mainTask() throws Exception {
                            if (callback == null) {
                                callback = new ApiNullCallback() {
                                    @Override
                                    public void onSuccess(ApiResponseNull parsedResponse) throws Exception {

                                    }
                                };
                            }
                            callback.runResult(mHealingPlurkConnection.startConnect(target, params));
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

            retryExecutor.run(contextWeakReference.get(), retryTimes, 1000, new NewThreadRetryExecutor.Tasks() {

                        @Override
                        public void mainTask() throws Exception {
                            if (!(callback instanceof ApiStringCallback)) {
                                throw new Exception("Callback needs to instanceof ApiStringCallback");
                            }
                            callback.runResult(mHealingPlurkConnection.startConnect(target, imageFile, fileName));
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
