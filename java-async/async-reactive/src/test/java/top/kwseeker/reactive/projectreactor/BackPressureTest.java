package top.kwseeker.reactive.projectreactor;

import org.junit.Test;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;

/**
 * 背压测试
 */
public class BackPressureTest {

    @Test
    public void testBackPressure() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Flux.interval(Duration.ofMillis(10))
                .take(100)
                .publishOn(Schedulers.newSingle("pub-1"))
                .log()
                .subscribeOn(Schedulers.newSingle("sub-1"))
                .doOnComplete(latch::countDown)
                //.limitRate(2)       //每次请求2个元素，处理完再执行下次请求
                //.limitRate(10)      //只有第一次请求10个元素，后面都变成了请求8个元素，因为有预取优化：https://projectreactor.io/docs/core/release/reference/#_operators_that_change_the_demand_from_downstream
                                        //一旦收到超过75%元素，就会继续请求75%的元素
                //.limitRate(12)
                .limitRate(10, 8)   //相当于 limitRate(10)，10表示请求元素最大数量，8表示当收到8个元素后开始请求下一批元素（8个）
                //.limitRequest(20)   //废弃，它就是通过take()实现的
                .subscribe(new BaseSubscriber<Long>() {
                    @Override
                    protected void hookOnNext(Long value) {
                        System.out.println("value=" + value);
                    }

                    @Override
                    protected void hookOnCancel() {
                        System.out.println("onCancel ...");
                    }

                    @Override
                    protected void hookOnComplete() {
                        System.out.println("onComplete ...");
                    }
                });

        latch.await();
    }

    @Test
    public void testBackPressureStrategy() {
        Sinks.Many<Integer> tp = Sinks.unsafe().many().multicast().directBestEffort();
    }
}
