package top.kwseeker.reactive.projectreactor;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class WebFluxHttpHandlerTest {

    /**
     * 剥离业务后看WebFlux Http请求的响应式处理
     */
    @Test
    public void testHandle() {
        //对应WebFlux中是HandlerMapping的链表
        List<Integer> mappings = new ArrayList<>(); //假设这个是HandlerMapping的链表
        mappings.add(1);
        Mono<Void> mono = Flux.fromIterable(mappings)
                //将此Flux发出的元素异步转换为publisher，然后将这些内部publisher平铺成单个Flux，并使用串联保持顺序
                //对应WebFlux中是异步查询和当前请求匹配的Handler
                .concatMap(mapping -> {
                    System.out.println("get matched handler");
                    String handler = String.valueOf(mapping);
                    return Mono.justOrEmpty(handler)
                            //同步转换
                            .map(h -> {
                                System.out.println("decorate cors configure on handler");
                                return h;
                            });
                })
                //只发送Flux中第一个元素到新的Mono中，如果Flux为空就创建空的Mono
                //对应WebFlux中只返回第一个匹配的Handler
                .next()
                //如果为空的处理
                //对应WebFlux中如果没有匹配的Handler就返回抛异常的Mono
                .switchIfEmpty(Mono.defer(() -> {
                    Exception ex = new RuntimeException("No matching handler");
                    return Mono.error(ex);
                }))
                //对应WebFlux中如果能走到这里说明至少有一个Handler,调用Handler的处理逻辑
                .flatMap(handler -> {
                    Function<Long, String> handlerMethod = reqData -> reqData + "D";
                    return Mono.empty()    //对应WebFlux这一步处理session、attr等的初始化
                        //.then(Mono.fromCallable(() -> {
                        //    long reqData = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
                        //    System.out.println("invoke handler method");        //这里面还有很多逻辑
                        //    return handlerMethod.apply(reqData);
                        //}))
                        .then(Mono.defer(() -> {    //defer()创建一个Mono发布者,每次订阅都会重新生成Mono发布者
                            //WebFlux中请求数据是ServerWebExchange带进来的
                            long reqData = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
                            System.out.println("invoke handler method");        //这里面还有很多逻辑
                            String result = handlerMethod.apply(reqData);
                            return Mono.just(result);
                        }))
                        .doOnNext(result -> System.out.println("set ExceptionHandler to result"))
                        .doOnNext(result -> System.out.println("save model in bindContext"))
                        //如果发生异常用Mono.error取代
                        .onErrorResume(ex -> {
                            System.out.println(ex.getMessage());
                            return Mono.error(ex);
                        });
                })
                //对应WebFlux中走到这里请求已经处理，这步用于处理响应结果
                .flatMap(result -> {
                    //WebFlux中这一步将响应写入response对象
                    System.out.println("handle result: " + result);             //这里面还有很多逻辑
                    return Mono.empty();
                });

        Mono<Void> defer = Mono.defer(() -> mono)
                .onErrorResume(ex -> {
                    System.out.println("handler (ExceptionHandlingWebHandler) handle exception and set response status");
                    return Mono.error(ex);
                })
                .doOnSuccess(aVoid -> System.out.println("request success"))
                .onErrorResume(ex -> {
                    System.out.println("handler (ReactorHttpHandlerAdapter) handle exception");
                    //ReactorHttpHandlerAdapter
                    return Mono.error(ex);
                })
                .then();
                //.doOnError()
                //.doOnSuccess()

        Mono<Void> voidMono = Mono.fromDirect(defer);
        voidMono.subscribe();
        System.out.println("-----------------------");
        ThreadUtil.sleep(2000);
        voidMono.subscribe();

        ThreadUtil.sleep(1000);
    }

    /**
     * Tue Sep 05 00:43:41 CST 2023
     * Tue Sep 05 00:43:41 CST 2023
     * Tue Sep 05 00:43:41 CST 2023
     * Tue Sep 05 00:43:46 CST 2023
     */
    @Test
    public void testDefer() {
        Mono<Date> m1 = Mono.just(new Date());  //此时数据已经准备好
        //延迟创建一个Mono发布者，每次订阅都会重新执行defer()中的Supplier方法
        Mono<Date> m2 = Mono.defer(()->Mono.just(new Date()));
        System.out.println(new Date());

        ThreadUtil.sleep(5000);

        m1.subscribe(System.out::println);      //发送之前准备好的数据
        m2.subscribe(System.out::println);

        ThreadUtil.sleep(5000);
        m1.subscribe(System.out::println);
        m2.subscribe(System.out::println);
    }
}
