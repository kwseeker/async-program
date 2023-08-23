package top.kwseeker.reactive.projectreactor;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

import static org.assertj.core.api.Assertions.assertThat;

public class MonoTest {

    @Test
    public void testMonoJust() {
        Mono<Integer> mono = Mono.just(1)
                .log();
        mono.subscribe(System.out::println);    //第一次订阅
        StepVerifier.create(mono).expectNext(1).verifyComplete();   //第二次订阅，支持重复订阅
    }

    @Test
    public void testMonoEmpty() {
        //有什么用？
        System.out.println("begin: " + System.currentTimeMillis());
        Mono.empty().subscribe();
        System.out.println("next: " + System.currentTimeMillis());
        Mono.empty().subscribe();
        System.out.println("end: " + System.currentTimeMillis());
    }

    /**
     * 结束当前Mono并开启新的Mono
     */
    @Test
    public void testMonoThen() {
        Mono.fromRunnable(() -> System.out.println("first"))
                .log()
                .then(Mono.just("second")
                        .log()
                        .doOnSuccess(signal -> {
                            System.out.println("signal: " + signal);
                        }))
                .subscribe(System.out::println);
    }

    /**
     * fromRunnable()
     * take()   超时自动取消
     */
    @Test
    public void testMonoFromRunnable() {
        System.out.println("begin: " + System.currentTimeMillis());
        Mono.fromRunnable(() -> {
                    try {
                        //Thread.sleep(1200);
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                })
                .log()
                .take(Duration.ofSeconds(1))    //等待1s, 还没完成就执行cancel()
                .subscribe();
        System.out.println("end: " + System.currentTimeMillis());
    }

    /**
     * Mono.delay()
     * 延迟执行onNext(),如果在指定的时间内请求没有发出则执行 onError()
     * MonoDelayTest#multipleDelaysUsingDefaultScheduler()
     */
    @Test
    public void testMonoDelay() throws InterruptedException {
        AtomicLong counter = new AtomicLong();
        assertThat(counter.intValue()).isEqualTo(0);    //首次执行AssertJ断言比较耗时, 为防止影响后面结果提前执行下

        System.out.println("begin: " + System.currentTimeMillis());
        Mono.delay(Duration.ofMillis(50))
                //.log()
                .doOnSubscribe(s -> {
                    System.out.println("1: " + System.currentTimeMillis());
                })
                .subscribe(v -> {
                    System.out.println("1 sub: " + System.currentTimeMillis());
                    counter.incrementAndGet();
                });
        Mono.delay(Duration.ofMillis(100))
                .doOnSubscribe(s -> {
                    System.out.println("2: " + System.currentTimeMillis());
                })
                .subscribe(v -> {
                    System.out.println("2 sub: " + System.currentTimeMillis());
                    counter.incrementAndGet();
                });
        Mono.delay(Duration.ofMillis(150))
                .doOnSubscribe(s -> {
                    System.out.println("3: " + System.currentTimeMillis());
                })
                .subscribe(v -> {
                    System.out.println("3 sub: " + System.currentTimeMillis());
                    counter.incrementAndGet();
                });
        Mono.delay(Duration.ofMillis(200))
                .doOnSubscribe(s -> {
                    System.out.println("4: " + System.currentTimeMillis());
                })
                .subscribe(v -> {
                    System.out.println("4 sub: " + System.currentTimeMillis());
                    counter.incrementAndGet();
                });

        System.out.println("assert: " + System.currentTimeMillis());
        assertThat(counter.intValue()).isEqualTo(0);
        assertEquals(0, counter.intValue());
        System.out.println("assert: " + System.currentTimeMillis());

        Thread.sleep(110);
        assertEquals(2, counter.intValue());

        Thread.sleep(110);
        assertEquals(4, counter.intValue());
    }

    @Test
    public void testAssertJ() {
        AtomicLong counter = new AtomicLong();
        System.out.println("assert: " + System.currentTimeMillis());
        assertThat(counter.intValue()).isEqualTo(0);      //Assert断言首次执行比较耗时
        System.out.println("assert: " + System.currentTimeMillis());
        assertEquals(0, counter.intValue());
        System.out.println("assert: " + System.currentTimeMillis());
        assertThat(counter.intValue()).isEqualTo(0);      //再次执行很快
        System.out.println("assert: " + System.currentTimeMillis());
    }

    @Test
    public void testMonoFromFuture() {
        CompletableFuture<String> f = CompletableFuture.supplyAsync(() -> "helloFuture");
        assertThat(Mono.fromFuture(f)
                .block()).isEqualToIgnoringCase("helloFuture");
    }
}
