# [Reactor Netty](https://projectreactor.io/docs/netty/release/reference/index.html)

> 这里介绍的是 `projectreactor` 的子项目 `reactor-netty`，是一个基于Netty实现的响应式通信框架，不是按Reactor服务器模型实现的Netty服务器。

提供了3种协议的服务器实现 TCP、Http、UDP。



## HTTP

+ **服务端**

  + 启动关闭

    + 设置 host 和 port

  + 饿汉式初始化

  + HTTP 路由

    + GET/POST

    + WebSocket

    + SSE (Server-Sent Events)

      > [SSE](https://link.zhihu.com/?target=https%3A//developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events) 是一种基于 HTTP 连接的服务器推送技术，客户端与服务器初始化好连接后，服务器可以随时向客户端发送内容更新。SSE 是对 HTTP 协议很好的补充，可以轻松实现客户端与服务器双向通信。
      >
      > 貌似当前版本的ApiFox不支持SSE，可以用Postman或浏览器测试。

    + 静态资源

  + 写响应数据 

    通过响应对象 [`HttpServerResponse`](https://projectreactor.io/docs/netty/1.1.10/api/reactor/netty/http/server/HttpServerResponse.html)写响应数据。

    + 元数据

      + Headers - header()
      + Cookies - addCookie()
      + Status - status()
      + ...

    + 数据压缩

      使用compress()等方法。有3种开启压缩策略：true 和 false 制定是否开启压缩、数据量超过阈值开启压缩、自定义开启压缩（实现`BiPredicate<HttpServerRequest, HttpServerResponse>`， predicate返回true就开启压缩）。

  + 读请求数据

    通过请求对象[`HttpServerRequest`](https://projectreactor.io/docs/netty/1.1.10/api/reactor/netty/http/server/HttpServerRequest.html)读请求数据。

    + 元数据

      + Headers

      + URI

    + 请求数据

      + Post Form
      + Multipart
      + Remote Address
      + ...

    + 请求解码器配置

      主要是请求数据长度限制。一个请求的请求数据量很大的话一定会分包发送。

  + 生命周期回调

    可以做些拓展或者添加日志。

    + doOnBInd
    + doOnBound
    + doOnChannelInit
    + doOnConnection
    + doOnUnbound

  + TCP层配置

    指修改TCPServer对象的配置。参考[TCP Server](https://projectreactor.io/docs/netty/release/reference/index.html#tcp-server)。

    + wire logger
    + ...

  + SSL 和 TLS

  + HTTP访问日志

    可以通过编程方式和配置方式开启访问日志。

    + 日志格式控制
    + 日志过滤

  + HTTP/2

    默认`HTTP server`支持HTTP/1.1。 

    ```java
    HttpServer.protocol(HttpProtocol.H2);
    ```

    + HTTP协议选择

      + HTTP11 (默认)
      + H2
      + H2C

      > 文档中有一句描述：
      >
      > In addition to the protocol configuration, if you need H2 but not H2C (cleartext), you must also configure SSL.
      >
      > 即使用HTTP2协议需要配置SSL，查资料发现HTTP2协议是基于HTTPS

  + Metrics

    HTTP服务器支持Micrometer的内置集成。

  + 链路追踪

  + Unix Domain Sockets

  + 超时配置

    + 请求超时
    + 连接超时
    + SSL/TLS超时

+ **客户端**

  + 连接服务端
  + 饿汉式初始化
  + 写请求数据
  + 读响应数据
  + 生命周期回调
  + TCP层配置
  + 连接池
  + SSL 和 TLS
  + 重试策略
  + HTTP/2
  + 代理支持
  + Metrics
  + 链路追踪
  + Unix Domain Sockets
  + Host Name Resolution
  + 超时配置