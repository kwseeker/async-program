# Spring WebFlux WebClient 

官方文档：https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html

Spring框架提供的**响应式HTTP客户端**，是Spring WebFlux的一部分。

模块：spring-framework/spring-webflux;
包： org.springframework.web.reactive.function.client;
接口类：WebClient;

与RestTemplate相比，WebClient支持以下特点：

+ 非阻塞 I/O
+ 响应流背压
+ 具有高并发性，硬件资源消耗更少
+ 流畅的API设计
+ 同步和异步交互
+ 流式传输支持

WebClient 可以理解为是一个响应式代理，本身并没有实现HTTP请求的逻辑，需要指定HTTP客户端库执行真正的请求，内置支持：

- [Reactor Netty](https://github.com/reactor/reactor-netty) [HttpClient](https://projectreactor.io/docs/netty/release/reference/index.html#http-client)
- [JDK HttpClient](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html)
- [Jetty Reactive HttpClient](https://github.com/jetty-project/jetty-reactive-httpclient)
- [Apache HttpComponents](https://hc.apache.org/index.html)
- Others can be plugged via `ClientHttpConnector`.

内置HTTP客户端的自动选择：通过类加载器判断引入的是哪个HTTP客户端依赖而决定。

```java
// DefaultWebClientBuilder.java
ClassLoader loader = DefaultWebClientBuilder.class.getClassLoader();
reactorClientPresent = ClassUtils.isPresent("reactor.netty.http.client.HttpClient", loader);
jettyClientPresent = ClassUtils.isPresent("org.eclipse.jetty.client.HttpClient", loader);
```

使用方法详细看官方文档就行。源码也不多，直接看源码也可以。

测试Module：java-async/spring-webflux-webclient 

**WebClientIntegrationTests** 是 Spring Framework 中提供的对WebClient的测试用例，可以作为使用参考（官方的测试代码才是最好的使用说明，代码直接拷贝过来无法直接运行，按报错对应修改即可，现在已经可以执行）。

> WebClientIntegrationTests 依赖 JUnit5 执行测试 和 Okhttp3 MockWebServer 模拟Web服务器。
>
> + JUnit5  [user-guide](https://junit.org/junit5/docs/current/user-guide/) [github](https://github.com/junit-team/junit5/)
>
>   可以顺便研究下人家官方的测试都是怎么写的。
>
> + Okhttp3 MockWebServer [README](https://github.com/square/okhttp/blob/master/mockwebserver/README.md)



## 项目依赖

SpringBoot WebFlux 项目中只需要 spring-boot-starter-webflux。

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

非SpringBoot项目经测试只需要 spring-webflux reactor-netty（如果选择Reactor-Netty的HttpClient做实际的客户端的话）。

```xml
<!-- spring-webflux 和 reactor-netty 版本尽量和 SpringBoot Starter 中依赖的版本一致， 
 比如 SpringBoot 2.2.1.RELEASE 对应的 spring-webflux 5.2.1.RELEASE 和 reactor-netty 0.9.1.RELEASE
	SpringBoot 2.7.16 (支持JDK8最后一个版本) 对应的 spring-webflux 5.3.30 和 reactor-netty 1.0.36
-->

<dependencies>
    <!-- WebClient依赖 -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-webflux</artifactId>
        <!--<version>5.2.1.RELEASE</version>-->
        <version>5.3.30</version>
    </dependency>
    <dependency>
        <groupId>io.projectreactor.netty</groupId>
        <artifactId>reactor-netty</artifactId>
        <!--<version>0.9.1.RELEASE</version>-->
        <version>1.0.36</version>
    </dependency>
    <!--<dependency>-->
    <!--    <groupId>org.springframework.boot</groupId>-->
    <!--    <artifactId>spring-boot-starter-webflux</artifactId>-->
    <!--    <version>2.2.1.RELEASE</version>-->
    <!--    <version>2.7.16</version>-->
    <!--</dependency>-->
    <!-- 测试依赖，为了跑官方的测试用例需要加上下面依赖 -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>mockwebserver</artifactId>
        <version>4.11.0</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-test</artifactId>
        <version>3.4.32</version>
    </dependency>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.8.2</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>3.22.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```



## [配置-Configuration](https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-builder.html)

创建WebClient的两种方式：

+ WebClient.create()

  使用默认的DefaultWebClientBuilder直接build() 返回 WebClient 对象。

+ WebClient.builder()

  返回DefaultWebClientBuilder对象，然后可以做些定制选项，最后build()生成WebClient对象。

DefaultWebClientBuilder 可选配置：

```java
// DefaultWebClientBuilder.java

// 请求方法中也可以设置URL和这个baseUrl有什么区别和关系？
// 这里的baseUrl用于与请求方法中的url拼接生成完整的URL
@Nullable
private String baseUrl;
// 扩展URI模板时使用的默认值
@Nullable
private Map<String, ?> defaultUriVariables;
// 用于定制baseURL
@Nullable
private UriBuilderFactory uriBuilderFactory;
// 设置每个请求的默认Header
@Nullable
private HttpHeaders defaultHeaders;
// 设置每个请求的默认Cookie
@Nullable
private MultiValueMap<String, String> defaultCookies;
// 设置每个消费者自定义请求
@Nullable
private Consumer<WebClient.RequestHeadersSpec<?>> defaultRequest;
// 添加请求过滤器
@Nullable
private List<ExchangeFilterFunction> filters;
// 用于指定可选的HTTP客户端库，默认内置支持的有4种
@Nullable
private ClientHttpConnector connector;
// HTTP消息读取器/写入器自定义策略
private ExchangeStrategies exchangeStrategies;
@Nullable
private ExchangeFunction exchangeFunction;
```

WebClient 是不可变对象。如果需要修改设置需要先克隆，然后重新build。

```java
WebClient newClient = originalClient.mutate()
	//配置...
	.build();
```

> 这里设计成不可变的应该也是为了安全性。



## [retrieve() 提取响应](https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-retrieve.html)

提取的数据都是写入Reactor响应式发布器，如`Mono<T>`, `Flux<T>`。

 ```java
 //WebClient$ResponseSpec
 
 <T> Mono<T> bodyToMono(ParameterizedTypeReference<T> elementTypeRef);
 <T> Flux<T> bodyToFlux(Class<T> elementClass);
 <T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> elementTypeRef);
 <T> Mono<ResponseEntity<T>> toEntity(Class<T> bodyClass);
 <T> Mono<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> bodyTypeReference);
 <T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> elementClass);
 <T> Mono<ResponseEntity<List<T>>> toEntityList(ParameterizedTypeReference<T> elementTypeRef);
 Mono<ResponseEntity<Void>> toBodilessEntity();
 ```



## [Exchange方法](https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-exchange.html)

相对于retrieve()方法，对需要更多控制的更高级情况非常有用，例如根据响应状态以不同方式解码响应。



## [Request Body 设置](https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-body.html)

+ JSON
  + MediaType.APPLICATION_JSON
  + MediaType.APPLICATION_STREAM_JSON

+ 表单
  + application/x-www-form-urlencoded
    + FormHttpMessageWriter
    + BodyInserters

+ Multipart
  + MultipartBodyBuilder
  + PartEvent



## [过滤器 Filters](https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-filter.html)

可以通过 WebClient.Builder 注册客户端过滤器（ExchangeFilterFunction），以便拦截和修改请求。

场景：

+ 认证
+ 负载均衡
+ CORS
+ ...

### WebClient 负载均衡

WebClient 借助 LoadBalancer 在过滤器中实现负载均衡。





## [属性 Attributes](https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-attributes.html)

用于请求处理过程中的传参，仅用于当前请求，应该也是用Map容器存储的。



## [Reactor Context](https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-context.html)

属性提供了一种将信息传递到过滤器链的便捷方法，但它们仅影响当前请求。如果您想传递传播到嵌套的其他请求的信息，例如通过 flatMap，或之后执行，例如通过 concatMap，那么你需要使用 Reactor Context。

就是一个**可以被嵌套的请求继承**的数据容器。

反应器上下文需要填充在反应链的末尾才能应用于所有操作。

```java
WebClient client = WebClient.builder()
		.filter((request, next) ->
				Mono.deferContextual(contextView -> {
					String value = contextView.get("foo");
					// ...
				}))
		.build();

client.get().uri("https://example.org/")
		.retrieve()
		.bodyToMono(String.class)
		.flatMap(body -> {
				// perform nested request (context propagates automatically)...
		})
		.contextWrite(context -> context.put("foo", ...));
```



## [同步阻塞获取响应结果](https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-synchronous.html)

支持等待多个响应的组合结果。基本仅用于测试。



## [测试](https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-testing.html)

要测试使用 WebClient 的代码，您可以使用模拟 Web 服务器，例如 [OkHttp MockWebServer](https://github.com/square/okhttp#mockwebserver)。要查看其使用示例，请查看 Spring Framework 测试套件中的 [WebClientIntegrationTests](https://github.com/spring-projects/spring-framework/blob/main/spring-webflux/src/test/java/org/springframework/web/reactive/function/client/WebClientIntegrationTests.java) 或 OkHttp 存储库中的 [static-serversample](https://github.com/square/okhttp/tree/master/samples/static-server)。
