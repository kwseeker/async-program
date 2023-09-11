package top.kwseeker.reactor.netty.webflux.web.server;

import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import top.kwseeker.reactor.netty.webflux.http.server.reactive.HttpHandler;
import top.kwseeker.reactor.netty.webflux.http.server.reactive.ServerHttpRequest;
import top.kwseeker.reactor.netty.webflux.http.server.reactive.ServerHttpResponse;
import top.kwseeker.reactor.netty.webflux.web.server.adapter.DefaultServerWebExchange;

/**
 * 实际的请求处理器
 */
public class HttpWebHandlerAdapter implements HttpHandler, WebHandler {

    //webflux中的delegate是经过装饰器模式层层装饰的 ExceptionHandlingWebHandler {FilteringWebHandler {DispatcherHandler}}
    private final WebHandler delegate;
    private ServerCodecConfigurer codecConfigurer = ServerCodecConfigurer.create();

    public HttpWebHandlerAdapter(WebHandler delegate) {
        Assert.notNull(delegate, "'delegate' must not be null");
        this.delegate = delegate;
    }

    public WebHandler getDelegate() {
        return this.delegate;
    }

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
        ServerWebExchange exchange = createExchange(request, response);

        return getDelegate().handle(exchange)
                //.doOnSuccess()
                //.onErrorResume()
                .then(Mono.defer(response::setComplete));
    }

    protected ServerWebExchange createExchange(ServerHttpRequest request, ServerHttpResponse response) {
        return new DefaultServerWebExchange(request, response, getCodecConfigurer());
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange) {
        return this.delegate.handle(exchange);
    }

    public ServerCodecConfigurer getCodecConfigurer() {
        return this.codecConfigurer;
    }
}
