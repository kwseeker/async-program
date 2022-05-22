package top.kwseeker.async.threadpool;

import lombok.Data;
import org.junit.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

/**
 * ForkJoinPool 中的 UNSAFE 操作测试。
 */
public class UnsafeTest {

    private final static Unsafe UNSAFE;
    static {
        try {
            //Unsafe是单例模式对象，包含一个 theUnsafe 静态成员实例
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new Error();
        }
    }

    /**
     * 简单示范ForkJoinPool中往任务队列的数组中插入任务的操作
     *
     *  ABASE = markword(8) + 压缩指针(4) + 数组长度(4)
     *  "(alen - 1) & s" 是防止 数组索引溢出
     *  "<< ASHIFT" = "* 指针类型长度(4)"
     */
    @Test
    public void testMockAddTask2WorkQueue() {
        int alen = 8192;
        int s = alen >>> 1;
        int ASHIFT = 2, ABASE = 16;
        int j = (((alen - 1) & s) << ASHIFT) + ABASE;   // 首次插入时的位置偏移量 16400
        System.out.println(j);

        String[] wqArr = new String[8];
        int top = 4;
        int offset = (((wqArr.length - 1) & top) << 2) + (8 + 4 + 4);
        UNSAFE.putOrderedObject(wqArr, offset, "Hello");
        System.out.println(wqArr[top]);
    }

    // 1 ForkJoinPool中有大量”UNSAFE CAS 自旋“操作
    // 结果：
    //  nameCounter=12, ageCounter=16, addrCounter=20       //即 对象头（8bytes markword + 4bytes 压缩的类型指针）+ 实例数据（4byte name对象指针 + 4byte age对象指针 + 4 byte addr对象指针）
    //  Thread-0 done
    //  Thread-1 done
    //  user age : 100000
    @Test
    public void testUnsafeConcurrencyModify() throws Exception {
        User user = new User();
        user.setAge(0);

        Class<?> clazz = User.class;
        long nameCounter = UNSAFE.objectFieldOffset(clazz.getDeclaredField("name"));
        long ageCounter = UNSAFE.objectFieldOffset(clazz.getDeclaredField("age"));
        long addrCounter = UNSAFE.objectFieldOffset(clazz.getDeclaredField("addr"));
        System.out.println(String.format("nameCounter=%d, ageCounter=%d, addrCounter=%d", nameCounter, ageCounter, addrCounter));

        CountDownLatch latch = new CountDownLatch(2);
        CyclicBarrier barrier = new CyclicBarrier(3);

        Runnable work = () -> {
            try {
                latch.await();

                for (int i = 0; i < 50000; i++) {
                    //对比：没有同步的并发修改
                    //int currentAge = user.getAge();
                    //user.setAge(currentAge + 1);

                    //UNSAFE CAS 自旋修改
                    for(;;) {   //自旋重试
                        Integer currentAge = user.getAge();
                        //boolean casRes = UNSAFE.compareAndSwapInt(user, ageCounter, currentAge, currentAge + 1);
                        boolean casRes = UNSAFE.compareAndSwapObject(user, ageCounter, currentAge, currentAge + 1);
                        if (casRes) {
                            break;
                        }
                    }
                }

                System.out.println(Thread.currentThread().getName() + " done");
                barrier.await();
            } catch (InterruptedException e) {
                System.out.println("been interrupted !");
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        };

        Thread[] threads = {new Thread(work), new Thread(work)};
        for (Thread thread : threads) {
            thread.start();
            latch.countDown();
        }

        barrier.await();
        System.out.println("user age : " + user.getAge());
    }


    @Data
    static class User {
        private String name;
        //private int age;
        private Integer age;
        private Addr addr;
    }

    @Data
    static class Addr {
        private String province;
        private String city;
        private String detail;
    }
}
