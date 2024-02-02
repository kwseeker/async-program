package top.kwseeker.async.orchestration;

import org.junit.Test;
import top.kwseeker.async.orchestration.listener.TaskResultListener;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class DefaultTaskGroupTest {

    @Test
    public void testTaskGroupNoDependency() throws InterruptedException {
        int timeoutMs = 5000;
        DefaultTaskGroup taskGroup = new DefaultTaskGroup(timeoutMs);

        DefaultTask A = new DefaultTask("A", p -> "A", "a");
        DefaultTask D = new DefaultTask("D", p -> "D", "d");
        taskGroup.appendTask(A);
        taskGroup.appendTask(D);

        taskGroup.start();
        boolean b = taskGroup.getThreadPool().awaitTermination(timeoutMs + 1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testTaskGroupWithStrongDependency() throws InterruptedException {
        int timeoutMs = 5000;
        DefaultTaskGroup taskGroup = new DefaultTaskGroup(timeoutMs);

        Function<Object, Object> randomEtFunc = p -> {
            int randomEt = new SecureRandom().nextInt(500) + 500;
            try {
                Thread.sleep(randomEt);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return p;
        };
        Function<Object, Object> taskFunc = p -> "r" + p;
        taskFunc = taskFunc.compose(randomEtFunc);

        DefaultTask A = new DefaultTask("A", taskFunc, "a");
        DefaultTask B = new DefaultTask("B", taskFunc);
        DefaultTask C = new DefaultTask("C", taskFunc);
        DefaultTask D = new DefaultTask("D", taskFunc, "d");
        DefaultTask E = new DefaultTask("E", taskFunc);
        DefaultTask F = new DefaultTask("F", taskFunc);
        DefaultTask G = new DefaultTask("G", taskFunc);
        DefaultTask H = new DefaultTask("H", taskFunc);
        //注册监听器，用于比如回调获取结果
        A.addListener(new TaskResultListener());
        B.addListener(new TaskResultListener());
        C.addListener(new TaskResultListener());
        D.addListener(new TaskResultListener());
        E.addListener(new TaskResultListener());
        F.addListener(new TaskResultListener());
        G.addListener(new TaskResultListener());
        H.addListener(new TaskResultListener());

        //任务编排
        taskGroup.appendTask(A);
        taskGroup.appendTask(A, B);
        taskGroup.appendTask(A, C);
        taskGroup.appendTask(B, F);
        taskGroup.appendTask(C, F);
        taskGroup.appendTask(F, H);
        taskGroup.appendTask(D);
        taskGroup.appendTask(D, E);
        taskGroup.appendTask(E, G);
        taskGroup.appendTask(G, H);

        taskGroup.start();
        boolean b = taskGroup.getThreadPool().awaitTermination(timeoutMs + 1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testTaskGroupWithWeakDependency() throws InterruptedException {
        int timeoutMs = 5000;
        DefaultTaskGroup taskGroup = new DefaultTaskGroup(timeoutMs);

        CustomEtFunction<Object, Object> taskFunc = p -> "r" + p;

        //DefaultTask A = new DefaultTask("A", taskFunc.customExecutionTime(1000), "a");
        DefaultTask A = new DefaultTask("A", taskFunc.customExecutionTime(200), "a");
        DefaultTask B = new DefaultTask("B", taskFunc.customExecutionTime(800));
        //DefaultTask B = new DefaultTask("B", taskFunc.customExecutionTime(200));
        DefaultTask C = new DefaultTask("C", taskFunc.customExecutionTime(1200));
        DefaultTask CPlus = new DefaultTask("CPlus", taskFunc.customExecutionTime(200));
        //DefaultTask C = new DefaultTask("C", taskFunc.customExecutionTime(200));
        DefaultTask D = new DefaultTask("D", taskFunc.customExecutionTime(200), "d");
        //DefaultTask E = new DefaultTask("E", taskFunc);
        DefaultTask F = new DefaultTask("F", true, taskFunc.customExecutionTime(1000)); //F弱依赖于所有前置依赖
        DefaultTask G = new DefaultTask("G", taskFunc.customExecutionTime(200));
        DefaultTask H = new DefaultTask("H", true, taskFunc.customExecutionTime(1000)); //H弱依赖于所有前置依赖

        //任务编排
        taskGroup.appendTask(A);
        taskGroup.appendTask(A, B);
        taskGroup.appendTask(A, C);
        taskGroup.appendTask(B, F);
        taskGroup.appendTask(C, CPlus);
        taskGroup.appendTask(CPlus, F);
        taskGroup.appendTask(F, H);
        taskGroup.appendTask(D);
        taskGroup.appendTask(D, G);
        taskGroup.appendTask(G, H);

        taskGroup.start();
        boolean b = taskGroup.getThreadPool().awaitTermination(timeoutMs + 1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testWaitTaskGroupFinished() throws InterruptedException {
        int timeoutMs = 3000;
        DefaultTaskGroup taskGroup = new DefaultTaskGroup(timeoutMs);

        CustomEtFunction<Object, Object> taskFunc = p -> "r" + p;

        DefaultTask A = new DefaultTask("A", taskFunc.customExecutionTime(1000), "a");
        DefaultTask B = new DefaultTask("B", taskFunc.customExecutionTime(1000));
        DefaultTask C = new DefaultTask("C", taskFunc.customExecutionTime(1000));
        DefaultTask D = new DefaultTask("D", taskFunc.customExecutionTime(200), "d");
        //DefaultTask E = new DefaultTask("E", taskFunc);
        DefaultTask F = new DefaultTask("F", true, taskFunc.customExecutionTime(1000)); //F弱依赖于所有前置依赖
        DefaultTask G = new DefaultTask("G", taskFunc.customExecutionTime(200));
        DefaultTask H = new DefaultTask("H", true, taskFunc.customExecutionTime(1000)); //H弱依赖于所有前置依赖

        //任务编排
        taskGroup.appendTask(A);
        taskGroup.appendTask(A, B);
        taskGroup.appendTask(A, C);
        taskGroup.appendTask(B, F);
        taskGroup.appendTask(C, F);
        taskGroup.appendTask(F, H);
        taskGroup.appendTask(D);
        taskGroup.appendTask(D, G);
        taskGroup.appendTask(G, H);

        taskGroup.start();
        taskGroup.awaitTimeout();
        //taskGroup.awaitTimeout(5000);
    }

    @FunctionalInterface
    interface CustomEtFunction<T, R> extends Function<T, R> {

        default Function<T, R> customExecutionTime(int time) {
            return (T t) -> apply(customExecutionTime(t, time));
        }

        default T customExecutionTime(T t, int time) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return t;
        }
    }
}