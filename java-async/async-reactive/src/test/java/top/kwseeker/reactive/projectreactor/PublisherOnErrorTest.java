package top.kwseeker.reactive.projectreactor;

import org.junit.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

public class PublisherOnErrorTest {

    @Test
    public void testOnErrorReturn() {
        Flux.error(new RuntimeException("some error"))
                .doOnError(e -> System.out.println("error occur: " + e.getMessage()))
                .onErrorReturn("DefaultValue")
                .subscribe(System.out::println);
    }

    /**
     * 异常会终止序列继续发送
     * 1
     * error occur: error with 2
     * 22
     */
    @Test
    public void testErrorTerminateProcess() {
        Flux<Integer> integerFlux = Flux.just(1, 2, 3)
                .map(i -> {
                    if (i == 2)
                        throw new RuntimeException("error with 2");
                    else
                        return i;
                })
                .doOnError(e -> System.out.println("error occur: " + e.getMessage()))
                .onErrorReturn(22);

        integerFlux.subscribe(System.out::println);

        ThreadUtil.sleep(1000);
    }

    /**
     * 替换发布者
     */
    @Test
    public void testOnErrorResume() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Flux.just(11L, 22L, 33L)
                .map(i -> {
                    if (i == 22)
                        throw new RuntimeException("error with 22");
                    else
                        return i;
                })
                .doOnError(e -> System.out.println("error occur: " + e.getMessage()))
                //.onErrorResume(e -> Flux.just(44L, 55L, 66L))
                //.onErrorResume(e -> Flux.fromArray(new Long[]{44L, 55L, 66L}))
                .onErrorResume(e -> Flux.interval(Duration.ofMillis(100)).take(3))
                .doOnComplete(latch::countDown)
                .subscribe(System.out::println);
        latch.await();
    }

    @Test
    public void testOnErrorContinue() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Flux.just(11L, 22L, 33L)
                .map(i -> {
                    if (i == 22)
                        throw new RuntimeException("error with 22");
                    else
                        return i;
                })
                .doOnError(e -> System.out.println("error occur: " + e.getMessage()))
                .onErrorContinue((e, u) -> System.out.println("error occur when send : " + u + ", message: " + e.getMessage()))
                .doOnComplete(latch::countDown)
                .subscribe(System.out::println);
        latch.await();
    }

    @Test
    public void testOnErrorMap() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Flux.just(11L, 22L, 33L)
                .map(i -> {
                    if (i == 22)
                        throw new RuntimeException("error with 22");
                    else
                        return i;
                })
                .doOnError(e -> System.out.println("error occur: " + e.getMessage()))
                .onErrorMap(e -> new RuntimeException("some error"))
                .doOnTerminate(latch::countDown)
                .subscribe(System.out::println);
        latch.await();
    }
}
