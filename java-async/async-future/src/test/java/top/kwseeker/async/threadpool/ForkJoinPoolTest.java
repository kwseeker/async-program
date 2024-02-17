package top.kwseeker.async.threadpool;

import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class ForkJoinPoolTest {

    //ForkJoinPool doJoin() 不易栈深度溢出的原理：
    // 通过对象链表调用方法替代方法递归调用，同时结合多线程任务偷取，相当于把方法递归执行给平铺开了（每次偷取执行，栈的深度就又变成个位数了）
    //Demo: 怎么获取执行过程中的方法栈最大深度？
    //没找到怎么查看运行时方法栈最大深度，不过可以设置-Xss降低栈最大允许深度，对比对栈深度的损耗
    //private static final ExecutorService executor = Executors.newFixedThreadPool(8);
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Map<Object, Integer> methodDepthCounter = new HashMap<>();

    /**
     * 测试通过阻塞将ForkJoinPool工作者线程耗尽
     * 并行度就是可创建工作者线程最大数量
     */
    @Test
    public void testBlockedTask() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(10);
        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println(cores);
        ForkJoinPool pool = new ForkJoinPool();
        for (int i = 0; i < cores; i++) {   //本机8核，最多可以建8个工作者线程
        //for (int i = 0; i < cores-1; i++) {
            pool.submit(() -> {
                System.out.println("wait in thread: " + Thread.currentThread().getName());
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        //上面将工作者线程耗尽了，所有工作者线程都在latch.await();
        //无法处理新提交的任务
        for (int i = 0; i < 3; i++) {
            pool.submit(() -> {
                System.out.println("won't see this message, because no worker to use");
            });
        }

        latch.await();
    }

    /**
     * 借助 ManagedBlocker 避免上述因为阻塞导致工作者线程被占用耗尽的问题
     */
    @Test
    public void testBlockedTask2() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(10);
        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println(cores);
        ForkJoinPool pool = new ForkJoinPool();
        for (int i = 0; i < cores; i++) {   //本机8核，最多可以建8个工作者线程
            pool.submit(() -> {
                System.out.println("wait in thread: " + Thread.currentThread().getName());
                try {
                    //latch.await();
                    //这里将阻塞条件封装到 ManagedBlocker 防止因工作者线程被耗尽导致后续任务无法处理
                    //参考自 CompletableFuture 的封装
                    ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker() {
                        //Returns:
                        //true if no additional blocking is necessary (i.e., if isReleasable would return true)
                        @Override
                        public boolean block() throws InterruptedException {
                            latch.await();
                            return isReleasable();
                        }

                        @Override
                        public boolean isReleasable() { //任务是否可释放
                            long count = latch.getCount();
                            return count <= 0;
                        }
                    });
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        //对比 testBlockedTask, 上面虽然将并行度设置的工作者线程耗尽了
        //但是因为 ForkJoinPool.managedBlock() 内部会检测当所有工作者线程都被阻塞时会突破并行度的限制创建新的工作者线程
        //所以下面提交的任务会被新创建的工作者线程处理
        for (int i = 0; i < 3; i++) {
            pool.submit(() -> {
                System.out.println("will see this message, because ForkJoinPool.managedBlock() create new worker to handle this task");
            });
        }

        latch.await();
    }

    @Test
    public void testSumTask2() throws ExecutionException, InterruptedException {
        SumTask2 task = new SumTask2(1, 200);
        ForkJoinPool pool = new ForkJoinPool();
        Future<Integer> sum = pool.submit(task);
        System.out.println(sum.get());
    }
    static class SumTask2 extends RecursiveTask<Integer> {

        private final int low;
        private final int high;

        public SumTask2(int low, int high) {
            if (low > high) {
                throw new RuntimeException("high need not little than low");
            }
            this.low = low;
            this.high = high;
        }

        @Override
        protected Integer compute() {
            if (high - low > 1) {
                int mid = low + (high - low) / 2;
                SumTask2 t1 = new SumTask2(low, mid);
                SumTask2 t2 = new SumTask2(mid+1, high);
                //return t1.compute() + t2.compute();   //这个方法是直接递归调用不会用多个线程处理
                ForkJoinTask<Integer> f1 = t1.fork();
                ForkJoinTask<Integer> f2 = t2.fork();
                return f1.join() + f2.join();
                //return f1.get() + f2.get(); //or
            } else {
                System.out.println("calc sub task in thread: " + Thread.currentThread().getName());
                if (high - low == 1) {
                    return low + high;
                } else {
                    return low;
                }
            }
        }
    }

    /**
     * 并行度取反的用意（其实是预留）
     * long np = (long)(-parallelism);
     * this.ctl = ((np << AC_SHIFT) & AC_MASK) | ((np << TC_SHIFT) & TC_MASK);
     * 即 this.ctl = ((np << 48) & 0xffffL << 48) | ((np << 32) & 0xffffL << 32);
     * 比如核心数8, commonPool parallelism=8-1=7, ctl = fff9fff900000000, 初始为负数
     * 结合后面 tryAddWorker() 的条件，如果没有空闲的工作者线程且ctl索引为47的位不为0才可以创建线程，
     * 以commonPool为例将并行度取反即 fff9，每次新建线程TC加“1”，加“7”之后索引为47的位将变为0，将不能创建新线程，推理可以创建7个工作者线程
     */
    @Test
    public void testParallelism() {
        int base = 0xfffffff9;  //0xfffffff9 --(-1)--> 0xfffffff8 --(取反)--> 0x00000007 --(加负号)--> -7
        System.out.println(base);
        System.out.println(base + 1);   //0xfffffff9 + 1 = 0xfffffffa ----> -6
        System.out.println(base + 7);   //0xfffffff9 + 7 = 0x00000000, 即符号位由1变为0
    }

    @Test
    public void testDoJoin() throws ExecutionException, InterruptedException {
        SumTask task = new SumTask(1, 100);
        Future<Integer> sum = executor.submit(task);
        System.out.println(sum.get());
        System.out.println(methodDepthCounter.size());  //创建了很多任务（126个），完全给平铺开了，很多任务会导致线程阻塞
                                                        // 固定线程数的话会被堵死，永远执行不完
                                                        // 对比ForkJoinPool也会先创建一批线程，但是由于有线程数上限，无法继续创建线程后，
        System.out.println(methodDepthCounter);         //执行深度都是1
    }
    static class SumTask implements Callable<Integer> {
        private final int low;
        private final int high;

        public SumTask(int low, int high) {
            this.low = low;
            this.high = high;
        }

        @Override
        public Integer call() throws Exception {
            methodDepthCounter.compute(this, (key, oldVal) -> oldVal == null ? 1 : oldVal + 1);

            int dif = high - low;
            if (dif > 1) {  //fork
                int mid = low + dif / 2;
                SumTask task1 = new SumTask(low, mid);
                SumTask task2 = new SumTask(mid+1, high);
                Future<Integer> f1 = executor.submit(task1);
                Future<Integer> f2 = executor.submit(task2);
                return f1.get() + f2.get();
            } else {
                return  (dif == 0) ? low : low + high;
            }
        }
    }

    @Test
    public void testDoJoinByRecursive() {
        int low = 1, high = 100;        //100个数栈深度大概是7
        int sum = sumByRecursive(low, high);
        System.out.println(sum);
        System.out.println(methodDepthCounter.size()); //只有一个线程
        System.out.println(methodDepthCounter);         //执行深度大概是7
    }
    private int sumByRecursive(int low, int high) {
        int dif = high - low;
        if (dif > 1) {
            int mid = low + dif / 2;
            return sumByRecursive(low, mid) + sumByRecursive( mid+1, high);
        } else {
            return  (dif == 0) ? low : low + high;
        }
    }

    //private int doJoin() {
    //    int s; Thread t; ForkJoinWorkerThread wt; ForkJoinPool.WorkQueue w;
    //    return (s = status) < 0 ?
    //                s
    //                :
    //                ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) ?
    //                        (w = (wt = (ForkJoinWorkerThread)t).workQueue).tryUnpush(this) && (s = doExec()) < 0 ?
    //                                s
    //                                :
    //                                wt.pool.awaitJoin(w, this, 0L)
    //                        :
    //                        externalAwaitDone();
    //}

    @Test
    public void testFirstSignalWorkCtl() {
        int parallelism = 8;
        long np = (long)(-parallelism); // offset ctl counts
        long ctl = ((np << 48) & 0xffffL << 48) | ((np << 32) & 0xffffL << 32);
        //long ctl = -1970359196712960L;
        System.out.println(Integer.toBinaryString(-8));
        System.out.println(ctl);
        System.out.println(Long.toBinaryString(ctl));
        // 1111111111111000 1111111111111000    高32位
        // 0000000000000000 0000000000000000    低32位
        int sp = (int) ctl;
        System.out.println(sp);

        long ADD_WORKER = 0x0001L << (32 + 15); // sign
        System.out.println(Long.toBinaryString(ADD_WORKER));
        boolean addWork = (ctl & ADD_WORKER) != 0L;
        System.out.println("add work ? " + addWork);
    }

    @Test
    public void testFinally() {
        for (int i = 0; i < 10; i++) {
            if (i == 3) {
                try {
                    System.out.println("3");
                    //return;
                } finally {
                    System.out.println("do finally");
                }
            } else {
                System.out.println(i);
            }
        }
    }

    //8 -> 16
    //wq array size: 4
    //000000 000001 000002
    //wq array size: 8
    //000003 000004
    //wq array size: 16
    //000005 000006 000007 000008
    //wq array size: 32
    //000009 000010 000011 000012 000013 000014 000015 000016
    //wq array size: 64
    //000017 000018 000019 000020 000021 000022 000023 000024 000025 000026 000027 000028 000029 000030 000031 000032
    //wq array size: 128
    //000033 000034 000035 000036 000037 000038 000039 000040 000041 000042 000043 000044 000045 000046 000047 000048 000049 000050 000051 000052 000053 000054 000055 000056 000057 000058 000059 000060 000061 000062 000063 000064
    //wq array size: 256
    //000065 000066 000067 000068 000069 000070 000071 000072 000073 000074 000075 000076 000077 000078 000079 000080 000081 000082 000083 000084 000085 000086 000087 000088 000089 000090 000091 000092 000093 000094 000095 000096 000097 000098 000099 000100 000101 000102 000103 000104 000105 000106 000107 000108 000109 000110 000111 000112 000113 000114 000115 000116 000117 000118 000119 000120 000121 000122 000123 000124 000125 000126 000127 000128
    //......
    @Test
    public void testGenerateWorkQueueArraySize() {
        Set<Integer> arrSizeSet = new HashSet<>();
        int n;
        for (int p = 0; p <= 65535; p++) {
            n = (p > 1) ? p - 1 : 1;
            n |= n >>> 1;
            n |= n >>> 2;
            n |= n >>> 4;
            n |= n >>> 8;
            n |= n >>> 16;
            n = (n + 1) << 1;

            if (!arrSizeSet.contains(n)) {
                System.out.println();
                System.out.println("wq array size: " + n);
                arrSizeSet.add(n);
            }
            System.out.printf("%06d ", p);
        }
    }
}
