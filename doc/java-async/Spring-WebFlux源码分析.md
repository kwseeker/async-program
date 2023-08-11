# Spring WebFlux 源码分析

这里基于 spring-boot-starter-webflux:2.2.1.RELEASE分析。



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



