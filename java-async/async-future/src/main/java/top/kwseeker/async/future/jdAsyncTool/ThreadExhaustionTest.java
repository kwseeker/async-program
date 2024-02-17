package top.kwseeker.async.future.jdAsyncTool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * 看jd asyncTool 提交多个并行后置任务时，前置依赖任务会同步阻塞等待后置任务执行完毕
 * 使用有边界的线程池容易造成线程耗尽问题
 * 但是使用CompletableFuture默认的ForkJoinPool则不会出现线程耗尽问题，这里写个测试研究源码
 */
public class ThreadExhaustionTest {

    static final ExecutorService fixedPool = Executors.newFixedThreadPool(2);
    static final ExecutorService forkJoinPool = new ForkJoinPool();
    static final ExecutorService forkJoinPool2 = new ForkJoinPool(2);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Task c1 = new Task("c1");
        Task c2 = new Task("c2");
        Task c3 = new Task("c3");
        Task c4 = new Task("c4");
        Task b1 = new Task("b1", c1, c2);
        Task b2 = new Task("b2", c3, c4);
        Task a = new Task("a", b1, b2);

        //前面的任务会占有仅有的两个线程并等待后置任务执行完，后置任务则没有线程可以执行，会超时异常
        //testFixedPool(a);

        //但是使用ForkJoinPool不会被前面的任务一直占用
        testForkJoinPool(a);
    }

    //public static void main(String[] args) throws ExecutionException, InterruptedException {
    //    testForkJoinPoolMaxThreads();
    //}

    public static void testFixedPool(Task head) throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> head.work(fixedPool));
        future.get();
    }

    /**
     * new ForkJoinPool(2) 理论上可以创建两个工作者线程，
     * 这里的任务执行过程中会往线程池提交新任务，并阻塞等待新任务执行完毕，按说2个工作者线程会被前面的任务占尽
     * 但是测试发现实际创建了4个工作者线程处理任务，原因？
     */
    public static void testForkJoinPool(Task head) throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> head.work(forkJoinPool2), forkJoinPool2);
        future.get();
    }

    public static void testForkJoinPoolMaxThreads() throws ExecutionException, InterruptedException {
        RecursiveTask task = new RecursiveTask(1);
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> task.work(forkJoinPool), forkJoinPool);
        future.get();
    }

    public static class Task implements Runnable {
        //模拟asyncTool的后置任务（即依赖当前任务的任务）
        private final String id;
        private final List<Task> next = new ArrayList<>();

        public Task(String id, Task ...tasks) {
            this.id = id;
            next.addAll(Arrays.asList(tasks));
        }

        //这里只是简单模拟 asyncTool 提交后置任务
        public void work(Executor executor) {
            run();
            if (!next.isEmpty()) { //后置任务不为空
                CompletableFuture<?>[] futures = new CompletableFuture[next.size()];
                for (int i = 0; i < next.size(); i++) {
                    Task task = next.get(i);
                    CompletableFuture<Void> future = CompletableFuture
                            .runAsync(() -> task.work(executor), executor);
                    futures[i] = future;
                }
                try {
                    //等待所有后置依赖任务执行完毕
                    //CompletableFuture.allOf(futures).get(3000, TimeUnit.MILLISECONDS);
                    CompletableFuture.allOf(futures).get();
                    System.out.println("task: " + id + " done");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public void addTask(Task task) {
            next.add(task);
        }

        @Override
        public void run() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.printf("run task: %s in %s\n", getId(), Thread.currentThread().getName());
        }

        public String getId() {
            return id;
        }
    }

    public static class RecursiveTask extends Task {

        public RecursiveTask(int id) {
            super(id + "");
        }

        @Override
        public void work(Executor executor) {
            int id = Integer.parseInt(getId());
            if (id < 10) {
            //if (id < 32768 - 10) {
            //if (id < 32768 + 10) {
                addTask(new RecursiveTask(id+1));
            }
            super.work(executor);
        }

        @Override
        public void run() {
            System.out.printf("run task: %s in %s\n", getId(), Thread.currentThread().getName());
        }
    }
}
