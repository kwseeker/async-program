package top.kwseeker.reactive.projectreactor;

import org.junit.Test;
import reactor.core.publisher.*;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;

public class HotAndColdPublisherTest {

    /**
     * 冷发布者只是强调为每个订阅都重新生成数据，但不确保为两个先后的订阅者生成一样的数据，刚开始容易错误理解为生成一样的数据。
     * 下面这个依然是冷发布器
     */
    @Test
    public void testColdFlux() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);

        Flux<Long> flux = Flux.create((Consumer<FluxSink<Long>>) sink -> {
                    System.out.println("current thread: " + Thread.currentThread().getName());
                    for (int i = 0; i < 5; i++) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        sink.next(System.currentTimeMillis());
                    }
                    sink.complete();
                })
                .subscribeOn(Schedulers.parallel())
                .doOnComplete(latch::countDown);

        flux.subscribe(l -> System.out.println(Thread.currentThread().getName() + " sub1: " + l));
        Thread.sleep(1000);
        flux.subscribe(l -> System.out.println(Thread.currentThread().getName() + " sub2: " + l));

        latch.await();
    }

    @Test
    public void testHotFluxWithUnicastProcessor() {
        //接口已经废弃
        UnicastProcessor<String> hotSource = UnicastProcessor.create();
        Flux<String> hotFlux = hotSource
                .publish()
                .autoConnect()
                .map(String::toUpperCase);

        hotFlux.subscribe(d -> System.out.println("Subscriber 1 to Hot Source: "+d));

        hotSource.onNext("blue");
        hotSource.onNext("green");

        hotFlux.subscribe(d -> System.out.println("Subscriber 2 to Hot Source: "+d));

        hotSource.onNext("orange");
        hotSource.onNext("purple");
        hotSource.onComplete();
    }

    @Test
    public void testHotFluxWithSinksMany() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        //TODO 原理
        Sinks.Many<Long> processor = Sinks.many().replay().limit(1);

        processor.emitNext(1L, FAIL_FAST);
        processor.emitNext(2L, FAIL_FAST);
        processor.emitNext(3L, FAIL_FAST);

        Flux<Long> longFlux = processor.asFlux()
                .switchOnFirst((s, f) -> f.filter(v -> v % s.get() == 0))   //过滤3的倍数
                .doOnComplete(latch::countDown);
        longFlux.subscribe(System.out::println);

        processor.emitNext(4L, FAIL_FAST);
        processor.emitNext(5L, FAIL_FAST);
        processor.emitNext(6L, FAIL_FAST);
        processor.emitNext(7L, FAIL_FAST);
        processor.emitNext(8L, FAIL_FAST);
        processor.emitNext(9L, FAIL_FAST);
        processor.emitComplete(FAIL_FAST);

        latch.await();
    }

    @Test
    public void testHotFluxWithSinksMany2() {
        Sinks.Many<String> hotSource = Sinks.unsafe().many().multicast().directBestEffort();

        Flux<String> hotFlux = hotSource.asFlux().map(String::toUpperCase);

        hotFlux.subscribe(d -> System.out.println("Subscriber 1 to Hot Source: "+d));

        hotSource.emitNext("blue", FAIL_FAST); // <1>
        hotSource.tryEmitNext("green").orThrow(); // <2>

        hotFlux.subscribe(d -> System.out.println("Subscriber 2 to Hot Source: "+d));

        hotSource.emitNext("orange", FAIL_FAST);
        hotSource.emitNext("purple", FAIL_FAST);
        hotSource.emitComplete(FAIL_FAST);
    }

    @Test
    public void testHotFluxWithShareMethod() {
        AtomicInteger subscriptionCount = new AtomicInteger();
        Mono<String> coldToHot = Mono.just("foo")
                .doOnSubscribe(sub -> subscriptionCount.incrementAndGet())
                .share() //this actually subscribes
                .filter(s -> s.length() < 4);

        coldToHot.block();
        coldToHot.block();
        coldToHot.block();

        assertThat(subscriptionCount).hasValue(1);
    }

    @Test
    public void testHotFluxWithPublishMethod() throws InterruptedException {
        Flux<Long> hotFlux = Flux.interval(Duration.ofSeconds(1))
                .take(10)
                .publish()
                .autoConnect();

        hotFlux.subscribe(d -> System.out.println("Subscriber 1 to Hot Source: "+d));

        Thread.sleep(5000);

        hotFlux.subscribe(d -> System.out.println("Subscriber 2 to Hot Source: "+d));

        hotFlux.blockLast();
    }
}
