package top.kwseeker.async.thread;

import java.util.concurrent.Callable;

/**
 * 获取线程执行结果
 *  可以参考逃逸分析的几种逃逸方式
 *  引用传参、返回值是不行了，但是可以用this实例引用、静态成员变量
 */
public class AsyncThreadGetResultTest {

    public static void main(String[] args) throws InterruptedException {
        long start = System.currentTimeMillis();

        Callable<String> callableA = () -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("--- doSomethingA---");
            return "A Task Done";
        };
        WorkA<String> workA = new WorkA<>(callableA);
        // 1.开启异步单元执行任务A
        Thread thread = new Thread(workA);
        thread.start();

        // 3.同步等待线程A运行结束
        thread.join();

        System.out.println("get thread result: " + workA.get());
        System.out.println(System.currentTimeMillis() - start);
    }

    /**
     * FutureTask 和 WorkA 这个实现差不多，为了让WorkA更像FutureTask,下面做些改造
     */
    static class WorkA<V> implements Runnable {

        private Object outcome;
        private Callable<V> callable;       //注意FutureTask没有直接实现Callable而是用的组合, 为何？

        public WorkA(Callable<V> callable) {
            this.callable = callable;
        }

        @Override
        public void run() {
            V result = null;
            try {
                result = callable.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
            //实例引用逃逸
            this.setOutcome(result);
        }

        @SuppressWarnings("unchecked")
        public V get() {
            Object x = outcome;
            return (V) x;
        }

        public Object getOutcome() {
            return outcome;
        }

        public void setOutcome(Object outcome) {
            this.outcome = outcome;
        }
    }

    //static class WorkA implements Runnable {
    //    private String result;
    //
    //    @Override
    //    public void run() {
    //        try {
    //            Thread.sleep(2000);
    //        } catch (InterruptedException e) {
    //            e.printStackTrace();
    //        }
    //        System.out.println("--- doSomethingA---");
    //        String result = "A Task Done";
    //        //实例引用逃逸
    //        this.setResult(result);
    //    }
    //
    //    public String getResult() {
    //        return result;
    //    }
    //
    //    public void setResult(String result) {
    //        this.result = result;
    //    }
    //}
}
