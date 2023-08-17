package top.kwseeker.reactive.projectreactor;

import org.junit.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class BlockingMonoSubscriberTest {

    @Test
    public void testWebFluxBindMono() {
        Mono<DisposableBind> mono = Mono.create(sink -> {
            //WebFlux bind 源码 ----------------------------------------------------------------

            //ServerBootstrap bootstrap = b.clone();
            //
            //ConnectionObserver obs = BootstrapHandlers.connectionObserver(bootstrap);
            //ConnectionObserver childObs =
            //    BootstrapHandlers.childConnectionObserver(bootstrap);
            //ChannelOperations.OnSetup ops =
            //    BootstrapHandlers.channelOperationFactory(bootstrap);
            //
            //convertLazyLocalAddress(bootstrap);
            //
            //BootstrapHandlers.finalizeHandler(bootstrap, ops, new ChildObserver(childObs));
            //
            //ChannelFuture f = bootstrap.bind();
            //
            //DisposableBind disposableServer = new DisposableBind(sink, f, obs, bootstrap);
            //f.addListener(disposableServer);
            //sink.onCancel(disposableServer);

            //去除Netty业务只保留Mono操作简化后的代码 -----------------------------------------------

            System.out.println("do server bind in publisher subscribe ...");
            //WebFlux中是ChannelFuture
            FutureTask<Boolean> future = new FutureTask<>(() -> {
                //模拟50%概率bind成功
                boolean bindResult = System.currentTimeMillis() % 2 == 1;
                System.out.println("simulate netty server bind result: " + (bindResult ? "success" : "failed"));
                return bindResult;
            });
            future.run();   //WebFlux netty server bind 是异步执行的，这里只是研究WebFlux对Mono的用法，忽略业务同时用同步模拟

            //WebFlux中是 DisposableBind disposableServer = new DisposableBind(sink, f, obs, bootstrap);
            DisposableBind disposableServer = new DisposableBind(sink, future);
            sink.onCancel(disposableServer);

            //WebFlux是通过给ChannelFuture注册监听器监听bind结果的
            //如果bind成功，就调用sink.success()否则调用error()
            disposableServer.operationComplete(future);
        });

        //阻塞等待bind完成，返回元素
        DisposableBind disposableServer = mono.block(Duration.ofSeconds(45));
        System.out.println("block wait bind, disposableServer: " + disposableServer);

        //bind成功后启动一个非守护线程等待channel关闭
        Thread awaitThread = new Thread("server") {
            @Override
            public void run() {
                //disposableServer.onDispose().block();
                //FutureMono.from(channel().closeFuture()).block();
                //TODO FutureMono 是 reactor-netty 中实现的一个Mono子类
            }
        };
        awaitThread.setContextClassLoader(getClass().getClassLoader());
        awaitThread.setDaemon(false);
        awaitThread.start();
    }

    static final class DisposableBind implements Disposable {
        //WebFlux DisposableBind 的成员：
        //final MonoSink<DisposableServer> sink;
        //final ChannelFuture              f;
        //final ServerBootstrap            bootstrap;
        //final ConnectionObserver         selectorObserver;
        final MonoSink<DisposableBind> sink;
        final Future<Boolean> future;

        public DisposableBind(MonoSink<DisposableBind> sink, Future<Boolean> future) {
            this.sink = sink;
            this.future = future;
        }

        @Override
        public void dispose() {
            System.out.println("do dispose ...");
        }

        public void operationComplete(Future<Boolean> f) {
            try {
                if (f.get()) {
                    //WebFlux中DisposableBind对象，DisposableBind disposableServer = new DisposableBind(sink, f, obs, bootstrap);
                    //发送元素
                    System.out.println("bind success, send element: " + this);
                    sink.success(this);
                } else {
                    //WebFlux中是ChannelBindException
                    sink.error(new RuntimeException("bind error"));
                }
            } catch (InterruptedException | ExecutionException e) {
                sink.error(new RuntimeException("bind error"));
            }
        }
    }
}