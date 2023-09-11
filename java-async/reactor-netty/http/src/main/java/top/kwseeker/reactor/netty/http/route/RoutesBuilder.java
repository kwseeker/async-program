package top.kwseeker.reactor.netty.http.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.http.server.HttpServerRoutes;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.function.Consumer;

public class RoutesBuilder implements Consumer<HttpServerRoutes> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void accept(HttpServerRoutes httpServerRoutes) {
        httpServerRoutes.get("/hello", (request, response) -> {
                    NettyOutbound outbound = response.sendString(Mono.just("Hello World!"));
                    return outbound;
                })
                .post("/echo", (request, response) -> {
                    NettyOutbound outbound = response.send(request.receive().retain());
                    return outbound;
                })
                .get("/path/{param}", (request, response) -> {
                    NettyOutbound outbound = response.sendString(Mono.just(request.param("param")));
                    return outbound;
                })
                .ws("/ws", (wsInbound, wsOutbound) -> {
                    NettyOutbound outbound = wsOutbound.send(wsInbound.receive().retain());
                    return outbound;
                });

        //Server-Sent Events
        httpServerRoutes.get("/sse", (request, response) -> {
            System.out.println("sse ...");
            Flux<Long> intervalFlux = Flux.interval(Duration.ofSeconds(1));
            return response.sse().send(intervalFlux.map(RoutesBuilder::toByteBuf), b -> true);
        });
    }

    private static ByteBuf toByteBuf(Object any) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write("data: ".getBytes(Charset.defaultCharset()));
            MAPPER.writeValue(out, any);
            out.write("\n\n".getBytes(Charset.defaultCharset()));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ByteBufAllocator.DEFAULT
                .buffer()
                .writeBytes(out.toByteArray());
    }
}
