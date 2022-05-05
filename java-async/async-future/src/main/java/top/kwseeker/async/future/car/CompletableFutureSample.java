package top.kwseeker.async.future.car;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * 设计一个场景将CompletableFuture主要接口都用上
 * 假如我们要造一辆车（需要轮子、车架、引擎）
 * 1）先造各个组件
 * 2）每个组件造完都要测试
 * 3）全部组件测试没问题后进行组装
 * 4）组装完成要进行各种测试，结构测试、性能测试、安全测试
 * 5）全部测试通过后宣告造车成功
 */
public class CompletableFutureSample {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Pipeline pipeline = Pipeline.getProxy();

        //1/2
        CompletableFuture<Boolean> frameFuture = CompletableFuture
                .supplyAsync(pipeline::buildFrame)
                //.thenApply(pipeline::testFrame);
                .thenApplyAsync(pipeline::testFrame);
        CompletableFuture<Boolean> wheelFuture = CompletableFuture
                .supplyAsync(pipeline::buildWheel)
                .thenApply(pipeline::testWheel);
                //.thenApplyAsync(pipeline::testWheel);
        CompletableFuture<Boolean> engineFuture = CompletableFuture
                .supplyAsync(pipeline::buildEngine)
                .thenApply(pipeline::testEngine);
                //.thenApplyAsync(pipeline::testEngine);
        //3
        //CompletableFuture<Void> componentResult = CompletableFuture.allOf(frameFuture, wheelFuture, engineFuture);
        //componentResult.get();
        //CompletableFuture<String> carFuture = componentResult.thenCompose(pipeline::buildCar);
        frameFuture.thenCombine(wheelFuture,
                (frameRet, wheelRet) -> {
                    if (frameRet && wheelRet) {
                        sleep(300);
                        System.out.println("install wheels onto frame ... [" + Thread.currentThread().getName() + "]");
                        return true;
                    }
                    return false;
                })
                .thenCombine(engineFuture, (stepOneRet, engineRet) -> {
                    if (stepOneRet && engineRet) {
                        sleep(500);
                        System.out.println("install engine onto frame ... [" + Thread.currentThread().getName() + "]");
                        return true;
                    }
                    return false;
                })
                .whenComplete((b, t) -> {
                    System.out.println("compose car done !");
                })
                //触发执行
                .get();
                //.thenCompose(pipeline::testArchitecture)
                //.thenCombine()


    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
