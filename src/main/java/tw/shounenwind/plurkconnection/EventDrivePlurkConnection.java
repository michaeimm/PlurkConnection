package tw.shounenwind.plurkconnection;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class EventDrivePlurkConnection extends PlurkConnection {

    public EventDrivePlurkConnection(String APP_KEY, String APP_SECRET, String token, String token_secret, boolean useHttps) {
        super(APP_KEY, APP_SECRET, token, token_secret, useHttps);
    }

    public EventDrivePlurkConnection(String APP_KEY, String APP_SECRET, boolean useHttps) {
        super(APP_KEY, APP_SECRET, useHttps);
    }

    public Builder builder(){
        return new Builder(this);
    }

    public interface Callback {
        void onSuccess(ApiResponse response);

        void onRetry(long retryTimes, long totalTimes);

        void onError(Throwable e);
    }

    public class Builder {

        private EventDrivePlurkConnection mHealingPlurkConnection;
        private String target;
        private Param[] params;
        private EventDrivePlurkConnection.Callback callback;
        private int retryTimes;

        public Builder(EventDrivePlurkConnection healingPlurkConnection) {
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

        public Builder setCallback(EventDrivePlurkConnection.Callback callback) {
            this.callback = callback;
            return this;
        }

        public void call() {
            Observable
                    .create(new Observable.OnSubscribe<ApiResponse>() {
                        @Override
                        public void call(Subscriber<? super ApiResponse> subscriber) {
                            try {
                                ApiResponse response = mHealingPlurkConnection.startConnect(target, params);
                                if (response.statusCode != HttpURLConnection.HTTP_OK) {
                                    throw new PlurkConnectionException(response);
                                }
                                subscriber.onNext(response);
                                subscriber.onCompleted();
                            } catch (Exception e) {
                                subscriber.onError(e);
                            }
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .retryWhen(new RetryWithDelay(retryTimes, 1000, callback))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(runCallbackObserver());
        }

        public void upload(final File imageFile, final String fileName) {
            Observable.create(new Observable.OnSubscribe<ApiResponse>() {
                @Override
                public void call(Subscriber<? super ApiResponse> subscriber) {
                    try {
                        ApiResponse response = mHealingPlurkConnection.startConnect(target, imageFile, fileName);
                        if (response.statusCode != HttpURLConnection.HTTP_OK) {
                            throw new PlurkConnectionException(response);
                        }
                        subscriber.onNext(response);
                        subscriber.onCompleted();
                    } catch (Exception e) {
                        subscriber.onError(e);
                    }
                }
            })
                    .subscribeOn(Schedulers.io())
                    .retryWhen(new RetryWithDelay(retryTimes, 1000, callback))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(runCallbackObserver());
        }

        private Observer<ApiResponse> runCallbackObserver() {
            return new Observer<ApiResponse>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    if (callback == null) {
                        return;
                    }
                    callback.onError(e);
                }

                @Override
                public void onNext(ApiResponse apiResponse) {
                    if (callback != null)
                        callback.onSuccess(apiResponse);
                }
            };
        }
    }

    public class RetryWithDelay implements
            Func1<Observable<? extends Throwable>, Observable<?>> {

        private final int maxRetries;
        private final int retryDelayMillis;
        private final Callback callback;
        private int retryCount;

        public RetryWithDelay(final int maxRetries, final int retryDelayMillis, Callback callback) {
            this.maxRetries = maxRetries;
            this.retryDelayMillis = retryDelayMillis;
            this.retryCount = 0;
            this.callback = callback;
        }

        @Override
        public Observable<?> call(Observable<? extends Throwable> attempts) {
            return attempts
                    .flatMap(new Func1<Throwable, Observable<?>>() {
                        @Override
                        public Observable<?> call(Throwable throwable) {
                            throwable.printStackTrace();
                            if (++retryCount < maxRetries) {
                                callback.onRetry(retryCount, maxRetries);
                                return Observable.timer(retryDelayMillis,
                                        TimeUnit.MILLISECONDS);
                            }

                            // Max retries hit. Just pass the error along.
                            return Observable.error(throwable);
                        }
                    });
        }
    }
}
