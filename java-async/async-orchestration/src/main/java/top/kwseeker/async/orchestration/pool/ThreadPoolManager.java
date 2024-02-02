package top.kwseeker.async.orchestration.pool;

import top.kwseeker.async.orchestration.Task;

import java.util.concurrent.*;

public class ThreadPoolManager {

    public static final ThreadPoolManager INSTANCE = new ThreadPoolManager();

    // 任务异步执行线程池, 暂时随便弄个线程池
    private ExecutorService threadPool = new ThreadPoolExecutor(4, 8,
            60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(10000));

    public static Future<?> submit(Task task) {
        return INSTANCE.getThreadPool().submit(task);
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    public void setThreadPool(ExecutorService threadPool) {
        this.threadPool = threadPool;
    }
}
