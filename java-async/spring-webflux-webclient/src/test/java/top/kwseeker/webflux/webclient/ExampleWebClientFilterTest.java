package top.kwseeker.webflux.webclient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class ExampleWebClientFilterTest extends BaseWebClientTest {

    @Test
    public void testSimpleFilter() throws InterruptedException {
        MockResponse response = new MockResponse()
                .setHeader("Content-Type", "text/plain")
                .setBody("Hello Spring!");
        this.server.enqueue(response);

        WebClient filteredClient = this.webClient.mutate()
                .filter((request, next) -> {
                    ClientRequest filteredRequest = ClientRequest.from(request)
                            .header("foo", "bar")
                            .build();
                    return next.exchange(filteredRequest);
                })
                .build();

        Mono<String> resultMono = filteredClient.get()
                //.uri("/greeting")
                .retrieve()
                .bodyToMono(String.class);
        String result = resultMono.block();
        System.out.println(result);

        RecordedRequest recordedRequest = this.server.takeRequest();
        Assertions.assertEquals("bar", recordedRequest.getHeader("foo"));
    }
}
