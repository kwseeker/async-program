package top.kwseeker.async.threadpool;

import java.util.concurrent.*;

public class AsyncThreadPoolTest {

    public static void doSomethingA() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("--- doSomethingA---");
    }

    public static String doSomethingB() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("--- doSomethingB---");
        return "B Task Done";
    }

    // 0自定义线程池
    private final static int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
    private final static ThreadPoolExecutor POOL_EXECUTOR = new ThreadPoolExecutor(AVAILABLE_PROCESSORS,
            AVAILABLE_PROCESSORS * 2,
            1,
            TimeUnit.MINUTES,
            new LinkedBlockingQueue<>(5),
            new ThreadPoolExecutor.CallerRunsPolicy());

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        long start = System.currentTimeMillis();

        // 1.开启异步单元执行任务A
        POOL_EXECUTOR.execute(AsyncThreadPoolTest::doSomethingA);

        // 2.执行任务B
        Future<String> future = POOL_EXECUTOR.submit(AsyncThreadPoolTest::doSomethingB);

        // 3.同步等待线程B执行结果
        System.out.println(future.get());

        System.out.println(System.currentTimeMillis() - start);
    }
}