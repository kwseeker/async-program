# [Spring WebFlux](https://docs.spring.io/spring-framework/reference/web/webflux.html) 应用开发

Spring WebFlux  响应式编程框架到现在也没流行起来，主要还是整个生态对响应式编程的支持还不够完整。

> 如果项目要使用WebFlux，需要确保项目中用到的组件都提供了响应式编程支持。
>
> 如果“前端“Web服务（几个节点）使用响应式编程、后端对接的服务（也是几个节点）使用命令式编程，这种情况下前端的Web服务使用响应式编程通常是没有意义的（支持的并发体量是不对等的，因为木桶效应并不会对项目并发支持体量有所提高）。
>
> 但比较特殊的一个场景是**网关**，网关（几个节点）后面可能对接了成百上千个服务节点（每个服务多个节点），后端服务并发体量加起来本身就很大，所以网关通常基于响应式编程或基于IO多路复用（可以参考各网关底层实现对比）。

响应式编程是异步的基于事件驱动的。

Spring WebFlux 底层实现基于 Spring Reactor, Spring Reactor又基于Reactor(io.projectreactor)、Netty按照响应式编程规范实现。

> 猜测是用了Reactor的接口定义（Reactor接口定义遵循Reactive Steams编程规范），使用Netty重新实现接口。

<img src="../img/spring-webflux.png" style="zoom:50%;" />



## WebFlux 应用实现

详细使用方法参考官方文档。

### 请求处理器定义

从前面架构简图可以看到WebFlux支持像WebMVC一样使用 @Controller @RequestMapping 定义请求处理器。另外还提供了RouterFunction方式定义请求处理器。

#### @Controller 注解

#### RouterFunction 函数式接口

### 统一异常处理

### ...



## Spring Cloud Gateway 是如何实现WebFlux应用的

