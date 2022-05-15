package top.kwseeker.async.threadpool;

import org.junit.Test;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolTest {

    private final static ThreadPoolExecutor SIMPLE_POOL_EXECUTOR = new ThreadPoolExecutor(2,
            4,
            1,
            TimeUnit.MINUTES,
            new LinkedBlockingQueue<>(5),
            new ThreadPoolExecutor.CallerRunsPolicy());

    private final Runnable runnableWork = () -> {
        System.out.println("[" + Thread.currentThread().getName() +  "] exec runnable work ...");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    };

    @Test
    public void testExecute() {
        SIMPLE_POOL_EXECUTOR.execute(runnableWork);
    }

    //ThreadPoolExecutor 创建工作者线程后加入线程池Set集合前会判断线程是否可启动 isAlive()
    //那么什么样的状态的线程是alive的？
    //线程新建状态和终止状态是 not alive 的
    @Test
    public void testThreadIsAlive() throws InterruptedException {
        Object obj = new Object();
        Thread thread = new Thread(() -> {
            try {
                System.out.println("thread start");
                Thread.sleep(5000);
                System.out.println("thread to waiting");
                synchronized (obj) {
                    obj.wait();
                }
                System.out.println("thread awake");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        //"新建"状态
        System.out.println(thread.isAlive());       //false

        thread.start();
        //"就绪/运行中"状态 不好控制这两种状态，完全是系统执行调度的
        //System.out.println(thread.isAlive());       //true

        //"超时等待"
        Thread.sleep(3000);
        System.out.println(thread.isAlive());       //false

        Thread.sleep(4000);
        //"等待"状态
        System.out.println(thread.isAlive());       //false
        synchronized (obj) {
            obj.notify();
        }
        System.out.println("wake thread up");       //false

        //"阻塞"状态需要再创建一个线程测试，不过肯定是alive的，略

        thread.join();
        //"终止"状态
        System.out.println(thread.isAlive());       //false
    }

    /**
     * 线程被中断只不过是设置下中断标志位
     * 如果线程在等待状态会抛出 InterruptedException, 并清除中断标志位
     */
    @Test
    public void testInterruptThread() throws InterruptedException {
        // 不听不听，王八念经
        Runnable work1 = () -> {
            while (true) {
                Thread.yield();
            }
        };
        // 属下这就去办
        Runnable work2 = () -> {
            while (true) {
                Thread.yield();
                // 响应中断
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("属下这就退出");
                    return;
                }
            }
        };
        // 我不要你认为，我要我认为
        Runnable work3 = () -> {
            while(true) {
                // 响应中断
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("我认为要中断退出");
                    return;
                }

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {      //sleep()被中断后会抛出异常并清除中断标记
                    System.out.println("我不要你认为要中断，我要我认为要中断");
                    // 我认为要中断
                    Thread.currentThread().interrupt();
                }
            }
        };

        Object obj = new Object();
        Runnable work4 = () -> {
            //while (true)
                synchronized (obj) {
                    try {
                        obj.wait();
                    } catch (InterruptedException e) {  //wait()被中断后也会抛出异常并清除中断标记
                        e.printStackTrace();
                        System.out.println("你不讲武德, 我异常了不干了，你耗子尾汁！");
                    }
                }
        };

        //Thread thread = new Thread(work1);
        //Thread thread = new Thread(work2);
        //Thread thread = new Thread(work3);
        Thread thread = new Thread(work4);
        thread.start();

        Thread.sleep(2000);
        thread.interrupt();

        thread.join();
    }

    @Test
    public void test() {
        System.out.println(Integer.toBinaryString(-1 << 29));   //111...
        System.out.println(Integer.toBinaryString(1 << 29));    //001
        System.out.println(Integer.toBinaryString(2 << 29));    //010
        System.out.println(Integer.toBinaryString(3 << 29));    //011

        System.out.println(Integer.toBinaryString(-536870911)); //11100000000000000000000000000001
    }
}
