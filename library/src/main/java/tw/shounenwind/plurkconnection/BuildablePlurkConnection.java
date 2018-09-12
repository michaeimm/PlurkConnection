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
    private ExecutorService threadPool;


    public BuildablePlurkConnection(String APP_KEY, String APP_SECRET, String token, String token_secret) {
        super(APP_KEY, APP_SECRET, token, token_secret);

    }

    public BuildablePlurkConnection(String APP_KEY, String APP_SECRET) {
        super(APP_KEY, APP_SECRET);
    }

    public void setThreadPool(ExecutorService threadPool) {
        this.threadPool = threadPool;
    }

    public Builder builder(Context mContext) {
        return new Builder(mContext, this);
    }

    public class Builder {

        private final NewThreadRetryExecutor retryExecutor;
        private WeakReference<Context> contextWeakReference;
        private BuildablePlurkConnection mHealingPlurkConnection;
        private String target;
        private Param[] params;
        private BasePlurkCallback callback;

        public Builder(Context mContext, BuildablePlurkConnection healingPlurkConnection) {
            contextWeakReference = new WeakReference<>(mContext);
            mHealingPlurkConnection = healingPlurkConnection;
            params = new Param[]{};
            retryExecutor = new NewThreadRetryExecutor();
            if (threadPool != null) {
                retryExecutor.setThreadPool(threadPool);
            }
        }

        public Builder setRetryTimes(int retryTimes) {
            retryExecutor.setTotalRetryTimes(retryTimes);
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

        public void call() {

            retryExecutor.setTasks(retryExecutor.new Tasks() {
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
                public void onRetry(Throwable e, int retryTimes, int totalTimes) {
                    if (callback != null)
                        callback.onRetry(e, retryTimes, totalTimes, new ErrorAction(this));
                }

                @Override
                public void onError(Throwable e) {
                    if (callback != null)
                        callback.onError(e);
                }
            });
            retryExecutor.run(contextWeakReference.get());

        }

        public void upload(final File imageFile, final String fileName) {
            retryExecutor.setTasks(retryExecutor.new Tasks() {

                public void mainTask() throws Exception {
                    if (!(callback instanceof ApiStringCallback)) {
                        throw new Exception("Callback needs to instanceof ApiStringCallback");
                    }
                    callback.runResult(mHealingPlurkConnection.startConnect(target, imageFile, fileName));
                }

                public void onRetry(Exception e, int retryTimes, int totalTimes) {
                    if (callback != null) {
                        callback.onRetry(e, retryTimes, totalTimes, new ErrorAction(this));
                    }
                }

                public void onError(Exception e) {
                    if (callback != null)
                        callback.onError(e);
                }
            });

            retryExecutor.run(contextWeakReference.get());

        }
    }

    public class ErrorAction {

        private NewThreadRetryExecutor.Tasks tasks;

        public ErrorAction(NewThreadRetryExecutor.Tasks tasks) {
            this.tasks = tasks;
        }

        public final void retry() {
            tasks.retry();
        }

        public final void error(Throwable e) {
            tasks.error(e);
        }
    }

}
