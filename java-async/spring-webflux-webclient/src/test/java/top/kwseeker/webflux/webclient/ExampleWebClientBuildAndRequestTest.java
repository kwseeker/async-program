package top.kwseeker.webflux.webclient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;

public class ExampleWebClientBuildAndRequestTest {

    private MockWebServer server;

    @AfterEach
    void shutdownServer() {
        try {
            if (server != null) {
                server.shutdown();
                server = null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testWebClientCreateAndGet() {
        server = new MockWebServer();
        String url = server.url("/json").toString();
        MockResponse response = new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"name\":\"arvin\",\"age\":18}");
        this.server.enqueue(response);

        //WebClient webClient = WebClient.create();
        //webClient = webClient.mutate().baseUrl(url).build();
        WebClient webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()))
                .baseUrl(url)
                .build();

        Mono<String> resultMono = webClient.get()
                .uri("/sss")        //MockWebServer uri 无论怎么设置都可以请求成功？
                .retrieve()
                .bodyToMono(String.class);
        //阻塞等待读取内容，项目中使用onXxx()方法处理（本质是回调方法）
        String result = resultMono.block();

        System.out.println(result);
        Assertions.assertTrue(StringUtils.hasLength(result));
    }
}
