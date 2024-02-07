package top.kwseeker.async.threadpool.forkjoin.sum;

import java.util.concurrent.*;

/**
 * ForkJoinPool 数组元素求和
 * 性能对比：
 * 1 for循环计算
 * 2 ThreadPoolExecutor 分批计算
 * 3 ForkJoinPool 计算
 * <p>
 * 1亿个整数累加 (测试太简单了，线程切换比累加计算耗时地多，大炮打蚊子，只看看ForkJoinPool怎么用就行了)：
 * cost(ms): 37, sum: 4949900825
 * cost(ms): 96, sum: 4949900825  ThreadPoolExecutor
 * cost(ms): 62, sum: 4949900825  ForkJoinPool
 */
public class ArraySumMain {

    static final int NCPU = Runtime.getRuntime().availableProcessors();
    static final int SEQUENTIAL_THRESHOLD = 1000;

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        //int[] array = RandomIntArrayUtil.buildRandomIntArray(10 * 10000 * 10000);
        //int[] array = RandomIntArrayUtil.buildRandomIntArray(10000 * 10000);
        int[] array = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        System.out.println("build array done");

        //1 for循环计算
        forSum(array);

        //2 ThreadPoolExecutor 分批计算
        executorSum(array);

        //3 ForkJoinPool 计算
        forkJoinSum(array);
    }

    public static void forSum(int[] array) {
        long begin = System.currentTimeMillis();
        long sum = 0L;
        for (int element : array) {
            sum += element;
        }
        System.out.println("cost(ms): " + (System.currentTimeMillis() - begin) + ", sum: " + sum);
    }

    public static void executorSum(int[] array) throws ExecutionException, InterruptedException {
        long result = 0;
        int batch = SEQUENTIAL_THRESHOLD;
        int taskCount = array.length / batch + (array.length % batch > 0 ? 1 : 0);
        System.out.println(taskCount);
        ExecutorService executor = Executors.newFixedThreadPool(NCPU);

        long begin = System.currentTimeMillis();
        SumTask[] tasks = new SumTask[taskCount];
        Future<Long>[] sums = new Future[taskCount];
        for (int i = 0; i < taskCount; i++) {
            int hi = Math.min((i + 1) * batch, array.length);
            tasks[i] = new SumTask(array, (i * batch), hi);
            sums[i] = executor.submit(tasks[i]);
        }

        for (int i = 0; i < taskCount; i++) {
            result += sums[i].get();
        }

        System.out.println("cost(ms): " + (System.currentTimeMillis() - begin) + ", sum: " + result);

        executor.shutdown();
    }

    public static void forkJoinSum(int[] array) throws ExecutionException, InterruptedException {
        long result;
        ForkJoinPool fjp = new ForkJoinPool(NCPU); //使用的线程数

        long begin = System.currentTimeMillis();
        SumRTask sumRTask = new SumRTask(array, 0, array.length);
        ForkJoinTask<Long> task = fjp.submit(sumRTask);
        result = task.get();

        System.out.println("cost(ms): " + (System.currentTimeMillis() - begin) + ", sum: " + result);

        if (task.isCompletedAbnormally()) {
            System.out.println(task.getException().getMessage());
        }
        fjp.shutdown();
    }


    static class SumTask implements Callable<Long> {
        int lo; //low index
        int hi; //high index
        int[] arr;

        public SumTask(int[] a, int l, int h) {
            lo = l;
            hi = h;
            arr = a;
        }

        @Override
        public Long call() throws Exception {
            long result = 0;
            for (int j = lo; j < hi; j++)
                result += arr[j];
            return result;
        }
    }

    static class SumRTask extends RecursiveTask<Long> {
        int low;
        int high;
        int[] array;

        SumRTask(int[] arr, int lo, int hi) {
            array = arr;
            low = lo;
            high = hi;
        }

        /**
         * fork()：将任务放入队列并安排异步执行，一个任务应该只调用一次fork()函数，除非已经执行完毕并重新初始化
         * join()：等待计算完成并返回计算结果
         */
        protected Long compute() {
            //最小任务，直接执行累加
            if (high - low <= SEQUENTIAL_THRESHOLD) {
                long sum = 0;
                for (int i = low; i < high; ++i) {
                    sum += array[i];
                }
                return sum;
            }
            //非最小任务，继续拆分
            else {
                int mid = low + (high - low) / 2;
                SumRTask left = new SumRTask(array, low, mid);
                SumRTask right = new SumRTask(array, mid, high);
                left.fork();
                right.fork();
                long rightAns = right.join();
                long leftAns = left.join();
                return leftAns + rightAns;
            }
        }
    }
}
