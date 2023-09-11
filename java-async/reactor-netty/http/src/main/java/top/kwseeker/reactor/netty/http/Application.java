package top.kwseeker.reactor.netty.http;

import top.kwseeker.reactor.netty.http.route.RoutesBuilder;
import top.kwseeker.reactor.netty.http.server.ReactiveHttpServer;

public class Application {

    public static void main(String[] args) {
        RoutesBuilder routesBuilder = new RoutesBuilder();
        ReactiveHttpServer.build("localhost", 18080, routesBuilder)
                .start();
    }
}
