package top.kwseeker.reactor.netty.webflux.web.server;

import top.kwseeker.reactor.netty.webflux.web.reactive.server.ReactiveWebServerFactory;

import java.net.InetAddress;

/**
 * WebServer 配置信息拓展接口
 */
public interface ConfigurableWebServerFactory extends ReactiveWebServerFactory {

    void setPort(int port);

    void setAddress(InetAddress address);

    //异常默认返回的错误页静态资源、SSL、HTTP2、数据压缩、头信息 等...

    void setSsl(Ssl ssl);

    void setHttp2(Http2 http2);
}
