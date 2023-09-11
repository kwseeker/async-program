package top.kwseeker.reactor.netty.http.server;

import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import top.kwseeker.reactor.netty.http.route.RoutesBuilder;

public class ReactiveHttpServer {

    private final String host;
    private final int port;
    private final RoutesBuilder routesBuilder;
    private boolean accessLog = true;

    private HttpServer httpServer;

    public ReactiveHttpServer(String host, int port, RoutesBuilder routesBuilder) {
        this.host = host;
        this.port = port;
        this.routesBuilder = routesBuilder;
    }

    public static ReactiveHttpServer build(String host, int port, RoutesBuilder routesBuilder) {
        ReactiveHttpServer reactiveHttpServer = new ReactiveHttpServer(host, port, routesBuilder);
        return reactiveHttpServer.createHttpServer();
    }

    public ReactiveHttpServer createHttpServer() {
        httpServer = HttpServer.create()
                .host(host)
                .port(port)
                .accessLog(accessLog)
                .route(routesBuilder);
        return this;
    }

    public void start() {
        DisposableServer disposableServer = httpServer.bindNow();
        Mono.when(disposableServer.onDispose()).block();
    }

}
