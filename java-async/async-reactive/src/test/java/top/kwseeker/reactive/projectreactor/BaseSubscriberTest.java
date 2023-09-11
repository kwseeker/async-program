package top.kwseeker.reactive.projectreactor;

import org.junit.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class BaseSubscriberTest {

    @Test
    public void partialRequestAndCancel() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger lastValue = new AtomicInteger(0);

        Flux<Integer> intFlux = Flux.range(1, 1000);
        intFlux.subscribe(new BaseSubscriber<Integer>() {

            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                request(1);
            }

            @Override
            public void hookOnNext(Integer integer) {
                System.out.println("current thread: " + Thread.currentThread().getName());
                assertThat(lastValue.compareAndSet(integer - 1, integer)).as("compareAndSet of %d", integer).isTrue();
                if (integer < 10) {
                    request(1);
                }
                else {
                    cancel();
                }
            }

            @Override
            protected void hookOnComplete() {
                fail("expected cancellation, not completion");
            }

            @Override
            protected void hookOnError(Throwable throwable) {
                fail("expected cancellation, not error " + throwable);
            }

            @Override
            protected void hookFinally(SignalType type) {
                latch.countDown();
                assertThat(type).isEqualTo(SignalType.CANCEL);
            }
        });

        System.out.println("current thread: " + Thread.currentThread().getName());
        latch.await(500, TimeUnit.MILLISECONDS);
        assertThat(lastValue).hasValue(10);
    }
}
