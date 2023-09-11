package top.kwseeker.reactor.netty.webflux.web.reactive.server;

import top.kwseeker.reactor.netty.webflux.http.server.reactive.HttpHandler;
import top.kwseeker.reactor.netty.webflux.web.server.WebServer;

@FunctionalInterface
public interface ReactiveWebServerFactory {

	WebServer getWebServer(HttpHandler httpHandler);
}