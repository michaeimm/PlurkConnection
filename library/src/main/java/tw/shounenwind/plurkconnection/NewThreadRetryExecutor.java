package tw.shounenwind.plurkconnection;

import android.content.Context;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NewThreadRetryExecutor {

    private ExecutorService threadPool;
    private int totalRetryTimes = 1;
    private int currentRetryTimes = 0;
    private WeakReference<Context> wrContext;
    private Tasks tasks;

    public void setThreadPool(ExecutorService threadPool) {
        this.threadPool = threadPool;
    }

    public void setTasks(Tasks tasks) {
        this.tasks = tasks;
    }

    public void setTotalRetryTimes(int totalRetryTimes) {
        this.totalRetryTimes = totalRetryTimes;
    }

    public void run(final Context mContext) {
        if (threadPool == null) {
            threadPool = Executors.newCachedThreadPool();
        }

        threadPool.execute(() -> {
            if (mContext == null)
                return;
            wrContext = new WeakReference<>(mContext);
            try {
                tasks.mainTask();
            } catch (final Exception e) {
                currentRetryTimes++;
                if (currentRetryTimes >= totalRetryTimes) {
                    final Context mActivity = wrContext.get();
                    if (mActivity == null) {
                        return;
                    }
                    tasks.onError(e);
                } else {
                    final Context mActivity = wrContext.get();
                    if (mActivity == null) {
                        return;
                    }
                    tasks.onRetry(e, currentRetryTimes, totalRetryTimes);
                }
            }
        });
    }

    public abstract class Tasks {
        abstract void mainTask() throws Exception;

        public void onRetry(Throwable e, int retryTimes, int totalTimes) {

        }

        public void onError(Throwable e) {

        }

        public final void retry() {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            run(wrContext.get());
        }

        public final void error(Throwable e) {
            tasks.onError(e);
        }
    }

}
