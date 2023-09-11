package top.kwseeker.reactor.netty.webflux.http.server.reactive;

import org.springframework.core.io.buffer.NettyDataBufferFactory;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.net.URISyntaxException;
import java.util.function.BiFunction;

/**
 * 只是入口类
 */
public class ReactorHttpHandlerAdapter implements BiFunction<HttpServerRequest, HttpServerResponse, Mono<Void>> {

    /**
     * 被适配的处理器
     */
    private final HttpHandler httpHandler;

    public ReactorHttpHandlerAdapter(HttpHandler httpHandler) {
        this.httpHandler = httpHandler;
    }

    @Override
    public Mono<Void> apply(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        NettyDataBufferFactory bufferFactory = new NettyDataBufferFactory(httpServerResponse.alloc());

        try {
            ServerHttpRequest request = new ReactorServerHttpRequest(httpServerRequest, bufferFactory);
            ServerHttpResponse response = new ReactorServerHttpResponse(httpServerResponse, bufferFactory);

            return this.httpHandler.handle(request, response);
        } catch (URISyntaxException e) {
            System.out.println("Failed to get request URI: " + e.getMessage());

            return Mono.empty();
        }
    }
}
