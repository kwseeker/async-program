package top.kwseeker.reactor.netty.webflux.http.server.reactive;

import reactor.core.publisher.Mono;

public interface HttpHandler {

    Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response);
}
