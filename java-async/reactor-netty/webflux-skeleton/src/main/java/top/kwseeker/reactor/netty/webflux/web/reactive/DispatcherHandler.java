package top.kwseeker.reactor.netty.webflux.web.reactive;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;

import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.RequestMethod;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.kwseeker.reactor.netty.webflux.http.server.reactive.ServerHttpRequest;
import top.kwseeker.reactor.netty.webflux.http.server.reactive.ServerHttpResponse;
import top.kwseeker.reactor.netty.webflux.web.server.ServerWebExchange;
import top.kwseeker.reactor.netty.webflux.web.server.WebHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 最终的请求处理器
 */
public class DispatcherHandler implements WebHandler {

    private ObjectMapper objectMapper = new ObjectMapper();

    //WebFlux DispatcherHandler 定义了下面的3个组件，分别定义路由处理方法、适配请求的处理方法并反射调用、实现结果处理（比如序列化、写response等）
    ////定义 请求路由 -> 请求处理方法 的映射（Map）
    //private List<HandlerMapping> handlerMappings;
    ////请求处理适配器，适配不同的请求处理器定义方式（比如@RequestMapping、RouterFunction）
    //private List<HandlerAdapter> handlerAdapters;
    ////响应结果处理器，比如结果序列化
    //private List<HandlerResultHandler> resultHandlers;

    //DispatcherHandler的封装不是此模块的重点，这里省略上方3大组件复杂的处理
    @Override
    public Mono<Void> handle(ServerWebExchange exchange) {
        MediaType mediaType = MediaType.APPLICATION_JSON;
        Flux<DataBuffer> body = null;

        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        if (RequestMethod.GET.name().equals(request.getMethodValue())
                && "/users/list".equals(request.getPath().value())) {
            List<UserVO> result = new ArrayList<>();
            result.add(new UserVO().setId(1).setUsername("Arvin"));
            result.add(new UserVO().setId(2).setUsername("Bob"));
            Flux<UserVO> resultFlux = Flux.fromIterable(result);

            if (request.getHeaders().getContentType() != null) {
                mediaType = request.getHeaders().getContentType();
            }

            body = Flux.from(resultFlux).collectList().map(list -> {
                boolean release = true;

                // HTTP Cookie https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Cookies
                ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from("SomeCookie", "123456")
                                .maxAge(Duration.ofHours(2)).secure(false).httpOnly(false);
                response.addCookie(cookieBuilder.build());

                DataBuffer buffer = response.bufferFactory().allocateBuffer();
                CollectionType valueType = getObjectMapper().getTypeFactory().constructCollectionType(List.class, UserVO.class);
                ObjectWriter writer = getObjectMapper().writer().forType(valueType);
                OutputStream outputStream = buffer.asOutputStream();
                try {
                    JsonGenerator generator = getObjectMapper().getFactory().createGenerator(outputStream, JsonEncoding.UTF8);
                    writer.writeValue(generator, list);
                    generator.flush();
                    release = false;
                } catch (IOException e) {
                    throw new IllegalStateException("Unexpected I/O error while writing to data buffer", e);
                } finally {
                    if (release) {
                        DataBufferUtils.release(buffer);
                    }
                }
                return buffer;
            }).flux();
        }

        //写响应数据
        if (body == null) {
            return Mono.error(new RuntimeException("empty response"));
        }
        response.getHeaders().setContentType(mediaType);
        return response.writeWith(body);
    }

    private ObjectMapper getObjectMapper() {
        return this.objectMapper;
    }
}
