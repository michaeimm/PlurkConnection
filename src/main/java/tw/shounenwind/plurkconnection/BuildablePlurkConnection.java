package tw.shounenwind.plurkconnection;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class BuildablePlurkConnection extends PlurkConnection {

    public BuildablePlurkConnection(String APP_KEY, String APP_SECRET, String token, String token_secret) {
        super(APP_KEY, APP_SECRET, token, token_secret);
    }

    public BuildablePlurkConnection(String APP_KEY, String APP_SECRET) {
        super(APP_KEY, APP_SECRET);
    }

    public Builder builder(){
        return new Builder(this);
    }

    public interface Callback {
        void onSuccess(ApiResponse response) throws Exception;

        void onRetry(long retryTimes, long totalTimes);

        void onError(Throwable e);
    }

    public class Builder {

        private BuildablePlurkConnection mHealingPlurkConnection;
        private String target;
        private Param[] params;
        private BuildablePlurkConnection.Callback callback;
        private int retryTimes;
        private RetryCheck retryCheck;

        public Builder(BuildablePlurkConnection healingPlurkConnection) {
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

        public Builder setCallback(BuildablePlurkConnection.Callback callback) {
            this.callback = callback;
            return this;
        }

        public Builder setRetryCheck(RetryCheck retryCheck) {
            this.retryCheck = retryCheck;
            return this;
        }

        public void call() {

            Flowable.create(new FlowableOnSubscribe<ApiResponse>() {
                @Override
                public void subscribe(FlowableEmitter<ApiResponse> emitter) throws Exception {
                    try {
                        ApiResponse response = mHealingPlurkConnection.startConnect(target, params);
                        if (response.statusCode != HttpURLConnection.HTTP_OK) {
                            throw new PlurkConnectionException(response);
                        }
                        emitter.onNext(response);
                        emitter.onComplete();
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                }
            }, BackpressureStrategy.BUFFER)
                    .subscribeOn(Schedulers.io())
                    .retryWhen(new RetryWithDelay(retryTimes, 1000, callback, retryCheck))
                    .observeOn(Schedulers.io())
                    .subscribe(runCallbackObserver());
        }

        public void upload(final File imageFile, final String fileName) {
            Flowable.create(new FlowableOnSubscribe<ApiResponse>() {
                @Override
                public void subscribe(FlowableEmitter<ApiResponse> emitter) throws Exception {
                    try {
                        ApiResponse response = mHealingPlurkConnection.startConnect(target, imageFile, fileName);
                        if (response.statusCode != HttpURLConnection.HTTP_OK) {
                            throw new PlurkConnectionException(response);
                        }
                        emitter.onNext(response);
                        emitter.onComplete();
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                }


            }, BackpressureStrategy.BUFFER)
                    .subscribeOn(Schedulers.io())
                    .retryWhen(new RetryWithDelay(retryTimes, 1000, callback, retryCheck))
                    .observeOn(Schedulers.io())
                    .subscribe(runCallbackObserver());
        }

        private Subscriber<ApiResponse> runCallbackObserver() {
            return new Subscriber<ApiResponse>() {
                @Override
                public void onComplete() {

                }

                @Override
                public void onError(Throwable e) {
                    if (callback == null) {
                        return;
                    }
                    callback.onError(e);
                }

                @Override
                public void onSubscribe(Subscription s) {
                    s.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(ApiResponse apiResponse) {
                    if (callback != null) {
                        try {
                            callback.onSuccess(apiResponse);
                        } catch (Exception e) {
                            e.printStackTrace();
                            onError(e);
                        }
                    }
                }
            };
        }
    }

    public class RetryWithDelay implements
            Function<Flowable<? extends Throwable>, Publisher<?>> {

        private final int maxRetries;
        private final int retryDelayMillis;
        private final Callback callback;
        private final RetryCheck retryCheck;
        private int retryCount;

        public RetryWithDelay(final int maxRetries, final int retryDelayMillis, Callback callback, RetryCheck retryCheck) {
            this.maxRetries = maxRetries;
            this.retryDelayMillis = retryDelayMillis;
            this.retryCount = 0;
            this.callback = callback;
            this.retryCheck = retryCheck;
        }

        @Override
        public Publisher<?> apply(Flowable<? extends Throwable> flowable) throws Exception {
            return flowable.flatMap(new Function<Throwable, Publisher<?>>() {
                @Override
                public Publisher<?> apply(Throwable throwable) throws Exception {
                    throwable.printStackTrace();
                    if (retryCheck != null && !retryCheck.isNeedRetry(throwable)) {
                        return Flowable.error(throwable);
                    }

                    if (++retryCount < maxRetries) {
                        callback.onRetry(retryCount, maxRetries);
                        return Flowable.timer(retryDelayMillis,
                                TimeUnit.MILLISECONDS);
                    }

                    // Max retries hit. Just pass the error along.
                    return Flowable.error(throwable);
                }
            });
        }
    }
}
