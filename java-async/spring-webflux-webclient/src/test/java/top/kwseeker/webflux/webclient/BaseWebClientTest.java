package top.kwseeker.webflux.webclient;

import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

public class BaseWebClientTest {

    //okhttp3 WebServer 模拟对象
    protected MockWebServer server;
    protected WebClient webClient;

    @BeforeEach
    void startServer() {
        System.out.println("start mock server ...");
        this.server = new MockWebServer();
        this.webClient = WebClient
                .builder()
                //.clientConnector(connector)
                .baseUrl(this.server.url("/").toString())
                .build();
    }

    @AfterEach
    void shutdown() throws IOException {
        System.out.println("shutdown mock server ...");
        if (server != null) {
            this.server.shutdown();
        }
    }
}
