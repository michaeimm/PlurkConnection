package tw.shounenwind.plurkconnection;

import android.content.Context;

import java.lang.ref.WeakReference;

class NewThreadRetryExecutor {

    public static void run(final Context mContext, Tasks tasks, RetryCheck retryCheck) {
        run(mContext, 1, 1, tasks, retryCheck);
    }

    public static void run(final Context mContext, final int times, final int delay, final Tasks tasks, final RetryCheck retryCheck) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                WeakReference<Context> activity = new WeakReference<>(mContext);
                for (int retry = 0; retry < times; retry++) {
                    try {
                        tasks.mainTask();
                        break;
                    } catch (final Exception e) {
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                            break;
                        }
                        if (retry >= times - 1) {
                            final Context mActivity = activity.get();
                            if (mActivity == null) {
                                break;
                            }
                            tasks.onError(e);
                            break;
                        } else {
                            final Context mActivity = activity.get();
                            if (mActivity == null) {
                                break;
                            }
                            if (retryCheck == null || retryCheck.isNeedRetry(e)) {
                                tasks.onRetry(retry + 1, times);
                            }
                        }
                    }
                }
            }
        }).start();
    }

    interface Tasks {
        void mainTask() throws Exception;

        void onRetry(int retryTimes, int totalTimes);

        void onError(Exception e);
    }
}
