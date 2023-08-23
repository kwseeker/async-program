package top.kwseeker.reactive.projectreactor;

import org.junit.Test;
import reactor.core.publisher.Flux;

public class SubscriberOnXxxTest {

    @Test
    public void testFluxOnXxx() {
        Flux.fromArray(new Integer[]{1, 2, 3, 4, 5})
                //转换逻辑
                .map(i -> {
                    System.out.println("map: " + i);
                    if (i == 2) {
                        throw new RuntimeException("exception for 2");
                        //} else if (i == 4) {
                        //    System.out.println("now do cancel");
                    } else {
                        return String.valueOf(i);
                    }
                })
                .doOnSubscribe(s -> System.out.println("doOnSubscribe ..."))
                .doOnRequest(l -> System.out.println("doOnRequest ... , request count: " + l))
                //注意是在map之后执行的，每个信号（保存有元素对象）发出时
                .doOnEach(stringSignal -> System.out.println("doOnEach: " + stringSignal.get()))
                .doOnNext(str -> System.out.println("doOnNext ... , str: " + str))
                //异常发生，然后会异常终止
                .doOnError(e -> System.out.println("doOnError ... , error msg: " + e.getMessage()))
                //主动取消，然后正常结束
                .doOnCancel(() -> System.out.println("doOnComplete ..."))
                //正常完成
                .doOnComplete(() -> System.out.println("doOnComplete ..."))
                //终止时调用，无论是正常还是异常
                .doOnTerminate(() -> System.out.println("doOnTerminate ..."))
                .subscribe();
    }
}
