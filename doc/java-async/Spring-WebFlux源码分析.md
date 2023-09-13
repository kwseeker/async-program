# Spring WebFlux 源码分析

这里基于 spring-boot-starter-webflux:2.2.1.RELEASE分析, 主要参考drawio流程图，此文档只是对流程图的补充。



## WebFlux 依赖

SpringBoot 开发 WebFlux应用只需要引入依赖`spring-boot-starter-webflux`。

此依赖依赖的包：

```
org.springframework.boot:spring-boot-starter-webflux:2.2.1.RELEASE
    org.springframework.boot:spring-boot-starter:2.2.1.RELEASE
    	org.springframework.boot:spring-boot-autoconfigure:2.2.1.RELEASE
    org.springframework.boot:spring-boot-starter-json:2.2.1.RELEASE
    org.springframework.boot:spring-boot-starter-reactor-netty:2.2.1.RELEASE
    	io.projectreactor.netty:reactor-netty:0.9.1.RELEASE
    org.springframework.boot:spring-boot-starter-validation:2.2.1.RELEASE
    org.springframework:spring-web:5.2.1.RELEASE
    org.springframework:spring-webflux:5.2.1.RELEASE
    org.synchronoss.cloud:nio-multipart-parser:1.1.0
```

其中最关键的是 `spring-boot-starter-reactor-netty` 和 `spring-webflux`。

 

## WebFlux 初始化流程

先看 `spring-boot-starter-webflux`中的自动配置类。发现没有自动配置类。这个starter和其他组件的starter还不一样。



## HTTP请求处理相关

请求处理主流程看流程图。

这里只是补充一些小知识点。

### Preflight请求

在CORS请求处理代码中碰到的，之前也遇到过但没仔细研究，MDN上的介绍：[Preflight request](https://developer.mozilla.org/zh-CN/docs/Glossary/Preflight_request)。

Preflight请求是一个**CORS 预检请求**是用于检查服务器是否支持 CORS 即跨域资源共享。一般是用了以下几个 HTTP 请求首部的 OPTIONS 请求：Access-Control-Request-Method 和 Access-Control-Request-Headers，以及一个 Origin 首部。

> preflight: 起飞前准备，预检。
>
> Preflight请求其实很早就遇到过了，肯定看到过请求前面总是带着个OPTIONS请求的场景。
>
> [CORS跨源资源共享](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/CORS)
>
> [OPTIONS](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Methods/OPTIONS)请求两种用途：
>
> + 检测服务器所支持的请求方法
> + CORS 中的预检请求

由浏览器在发出**非简单请求的CORS请求**前自动发出，不需要开发者发起请求。关于[简单请求](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/CORS#%E7%AE%80%E5%8D%95%E8%AF%B7%E6%B1%82)参考上面MDN的CORS链接。

比如服务器响应预检请求的报文头部：

```yaml
# 请求
OPTIONS /resources/post-here/ HTTP/1.1
Host: bar.example
Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
Accept-Language: en-us,en;q=0.5
Accept-Encoding: gzip,deflate
Connection: keep-alive
Origin: https://foo.example
Access-Control-Request-Method: POST
Access-Control-Request-Headers: X-PINGOTHER, Content-Type
# 响应
HTTP/1.1 200 OK
Date: Mon, 01 Dec 2008 01:15:39 GMT
Server: Apache/2.0.61 (Unix)
Access-Control-Allow-Origin: https://foo.example
Access-Control-Allow-Methods: POST, GET, OPTIONS
Access-Control-Allow-Headers: X-PINGOTHER, Content-Type
Access-Control-Max-Age: 86400
Vary: Accept-Encoding, Origin
Keep-Alive: timeout=2, max=100
Connection: Keep-Alive
```

主要是设置下面四个头信息（像SpringBoot中就是写的这四个头信息）：

+ [`Access-Control-Allow-Origin`](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Headers/Access-Control-Allow-Origin)

  `https://foo.example` 源被允许通过以下方式请求 `bar.example/resources/post-here/` URL：

+ [`Access-Control-Allow-Methods`](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Headers/Access-Control-Allow-Methods)

  [`POST`](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Methods/POST)、[`GET`](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Methods/GET) 和 `OPTIONS` 是该 URL 允许的方法（该标头类似于 [`Allow`](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Headers/Allow) 响应标头，但只用于 [CORS](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/CORS)）。

+ [`Access-Control-Allow-Headers`](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Headers/Access-Control-Allow-Headers)

  `X-PINGOTHER` 和 `Content-Type` 是该 URL 允许的请求标头。

+ [`Access-Control-Max-Age`](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Headers/Access-Control-Max-Age)

  以上权限可以缓存 86400 秒（1 天）。

#### OPTIONS请求优化

+ 将请求改为简单请求
+ 设置OPTIONS请求缓存，即设置Access-Control-Max-Age的缓存时间。

### SSLSession

同HTTP连接会话一样，SSL握手后也需要会话保存一些属性来维持客户端与服务器的持续关系。

除具有的标准会话属性外，SSLSession还公开这些只读属性：

同伴身份。 会话位于特定客户端和特定服务器之间。 可以将对等体的身份建立为会话设置的一部分。 对等方通常由X.509证书链标识。
密码套件名称。 密码套件描述了特定会话中连接使用的加密保护类型。
同伴主持人。 会话中的所有连接都在同一个主机之间。 可以使用连接另一端的主机地址。

### Cookie 安全性

Cookie除了key、value还有6个属性：Max-Age（Expires）、Domain、Path、Secure、HttpOnly、SameSite。

参考 [Set-Cookie](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Headers/Set-Cookie#samesitesamesite-value)、 [零基礎資安系列（三）-網站安全三本柱（Secure & SameSite & HttpOnly）](https://tech-blog.cymetrics.io/posts/jo/zerobased-secure-samesite-httponly/)。

Cookie的安全性保证通过后面3个属性实现。

+ Secure

  为true时只能通过HTTPS传输Cookie。阻止[中间人](https://developer.mozilla.org/zh-CN/docs/Glossary/MitM)攻击。

+ HttpOnly

  为true时不可以通过JS document.cookie 读取 Cookie的值。用于防范跨站脚本攻击（[XSS](https://developer.mozilla.org/zh-CN/docs/Glossary/Cross-site_scripting)）。

+ SameSite 

  允许服务器设定一则 cookie 不随着跨站请求一起发送，这样可以在一定程度上防范跨站请求伪造攻击（[CSRF](https://developer.mozilla.org/zh-CN/docs/Glossary/CSRF)）。

  可选属性：

  - Strict

    这意味浏览器仅对同一站点的请求发送 `cookie`，即请求来自设置 cookie 的站点。如果请求来自不同的域或协议（即使是相同域），则携带有 `SameSite=Strict` 属性的 cookie 将不会被发送。

  - Lax

    这意味着 cookie 不会在跨站请求中被发送，如：加载图像或 frame 的请求。但 cookie 在用户从外部站点导航到源站时，cookie 也将被发送（例如，跟随一个链接）。这是 `SameSite` 属性未被设置时的默认行为。

  - None

    这意味着浏览器会在跨站和同站请求中均发送 cookie。在设置这一属性值时，必须同时设置 `Secure` 属性，就像这样：`SameSite=None; Secure`。

  
