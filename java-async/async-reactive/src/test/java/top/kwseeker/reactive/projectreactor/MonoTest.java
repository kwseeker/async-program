package top.kwseeker.reactive.projectreactor;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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

    @Test
    public void testMonoFromCallable() {
        Mono<String> mono = Mono.fromCallable(() -> {
            long reqData = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
            return String.valueOf(reqData);
        });
        mono.subscribe(System.out::println);
        ThreadUtil.sleep(2000);
        mono.subscribe(System.out::println);
    }

    @Test
    public void testMonoZipAndWhen() {
        final Map<String, Object> bindingContext = new HashMap<>();

        Mono<Void> voidMono = Mono.zip(Arrays.asList(Mono.just(1), Mono.just(2)), objectArray ->
                        Arrays.stream(objectArray)
                                //.map(object -> handleResult(((HandlerResult) object), bindingContext))
                                .map(object -> {
                                    bindingContext.put(String.valueOf(object), object);
                                    return Mono.empty();
                                })
                                .collect(Collectors.toList()))
                .flatMap(Mono::when)
                .doOnSuccess(aVoid -> System.out.println("zip all done"));

        voidMono.then(Mono.defer(() -> {
            System.out.println("after zip all done");
            return Mono.just("done");
        })).subscribe();

        assertEquals(2, bindingContext.size());
    }

    @Test
    public void testMonoCheckpoint() {
        Mono.fromRunnable(() -> {
                    if (System.currentTimeMillis() % 2 == 0) {
                        System.out.println("simulated except");
                        throw new RuntimeException("simulated except");
                    } else {
                        System.out.println("normal exec");
                    }
                })
                .checkpoint("检查点，检查到上流发生错误") //为这个特定的Mono激活回溯(程序集标记)，方法是给它一个描述，如果在检查点上游发生错误，该描述将反映在程序集回溯中。
                                                                 //即上游发生错误会打印这个描述
                .onErrorResume(ex -> {
                    System.out.println("onErrorResume ...");
                    return Mono.error(ex);
                })
                .subscribe();
    }
}
