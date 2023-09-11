package top.kwseeker.reactor.netty.webflux.web.reactive.context;

import reactor.core.publisher.Mono;
import top.kwseeker.reactor.netty.webflux.http.server.reactive.HttpHandler;
import top.kwseeker.reactor.netty.webflux.http.server.reactive.ServerHttpRequest;
import top.kwseeker.reactor.netty.webflux.http.server.reactive.ServerHttpResponse;
import top.kwseeker.reactor.netty.webflux.web.reactive.server.ReactiveWebServerFactory;
import top.kwseeker.reactor.netty.webflux.web.server.WebServer;

import java.util.function.Supplier;

/**
 * Web服务器 + Http请求处理器
 */
public class ServerManager implements HttpHandler {

    //Web服务器
    private final WebServer server;
    //Http请求入口处理器，如 HttpWebHandlerAdapter
    private volatile HttpHandler handler;

    private ServerManager(ReactiveWebServerFactory factory) {
        this.handler = this::handleUninitialized;
        this.server = factory.getWebServer(this);
    }

    private Mono<Void> handleUninitialized(ServerHttpRequest request, ServerHttpResponse response) {
        throw new IllegalStateException("The HttpHandler has not yet been initialized");
    }

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
        return this.handler.handle(request, response);
    }

    public static ServerManager get(ReactiveWebServerFactory factory) {
        return new ServerManager(factory);
    }

    public static void start(ServerManager manager, Supplier<HttpHandler> handlerSupplier) {
        if (manager != null && manager.server != null) {
            manager.handler = handlerSupplier.get();
            manager.server.start();
        }
    }

    public static void start(ServerManager manager, HttpHandler httpHandler) {
        if (manager != null && manager.server != null) {
            manager.handler = httpHandler;
            manager.server.start();
        }
    }

    public static void stop(ServerManager manager) {
        if (manager != null && manager.server != null) {
            manager.server.stop();
        }
    }
}
