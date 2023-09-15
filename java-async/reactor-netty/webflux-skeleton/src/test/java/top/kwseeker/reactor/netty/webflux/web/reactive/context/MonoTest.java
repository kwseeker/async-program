package top.kwseeker.reactor.netty.webflux.web.reactive.context;

import io.netty.channel.Channel;
import org.junit.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;

public class MonoTest {

    @Test
    public void testMonoCreateOnCancel() {
        Mono<DisposableBind> mono = Mono.create(sink -> {
            DisposableBind disposableServer = new DisposableBind();
            sink.onCancel(() -> {
                System.out.println("do onCancel ...");
            });
            if (System.currentTimeMillis() % 2 == 0) {
                sink.error(new RuntimeException("bind error"));
            } else {
                sink.success(disposableServer);
            }
        });
        DisposableServer disposableServer = mono.block();
        assert disposableServer != null;
        Mono.when(disposableServer.onDispose()).block();

    }

    static class DisposableBind implements Disposable, DisposableServer {
        @Override
        public void dispose() {
            System.out.println("do dispose ...");
        }

        @Override
        public Channel channel() {
            return null;
        }
    }
}
