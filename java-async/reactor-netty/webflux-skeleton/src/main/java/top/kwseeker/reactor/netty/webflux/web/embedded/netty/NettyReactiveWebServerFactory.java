package top.kwseeker.reactor.netty.webflux.web.embedded.netty;


import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;
import top.kwseeker.reactor.netty.webflux.http.server.reactive.HttpHandler;
import top.kwseeker.reactor.netty.webflux.http.server.reactive.ReactorHttpHandlerAdapter;
import top.kwseeker.reactor.netty.webflux.web.reactive.server.AbstractReactiveWebServerFactory;
import top.kwseeker.reactor.netty.webflux.web.server.WebServer;

import java.time.Duration;

public class NettyReactiveWebServerFactory extends AbstractReactiveWebServerFactory {

    private Duration lifecycleTimeout;

    public NettyReactiveWebServerFactory() {
    }

    public NettyReactiveWebServerFactory(int port) {
        super(port);
    }

    /**
     * 核心方法
     * 基于 reactor-netty HTTP服务器类 HttpServer
     * 1 创建HttpServer实例
     * 2 初始化配置(ip 端口 路由注册 响应头 编解码器配置 生命周期回调 TCP层面配置 SSL/TLS HTTP2 监控 追踪 超时配置 等等)
     */
    @Override
    public WebServer getWebServer(HttpHandler httpHandler) {
        HttpServer httpServer = createHttpServer();
        ReactorHttpHandlerAdapter handlerAdapter = new ReactorHttpHandlerAdapter(httpHandler);
        NettyWebServer webServer = new NettyWebServer(httpServer, handlerAdapter, this.lifecycleTimeout);
        return webServer;
    }


    private HttpServer createHttpServer() {
        //1 创建HttpServer实例
        HttpServer server = HttpServer.create();
        //2 初始化配置(ip 端口 路由注册 响应头 编解码器配置 生命周期回调 TCP层面配置 SSL/TLS HTTP2 监控 追踪 超时配置 等等)
        //ip 端口 之前的版本难道是通过 TCP-level 的方法配置的？这方法新版本都废弃了
        //server = server.tcpConfiguration(tcpServer -> tcpServer.bindAddress(() -> {
        //    if (getAddress() != null) {
        //        return new InetSocketAddress(getAddress().getHostAddress(), getPort());
        //    }
        //    return new InetSocketAddress(getPort());
        //}));
        if (getAddress() != null) {
            //因为每次配置都会重新拷贝一个新的对象再设置配置，所以需要重新赋值
            server = server.host(getAddress().getHostAddress());
        }
        return server.port(getPort())
                .protocol(listProtocols());
    }

    private HttpProtocol[] listProtocols() {
        //In addition to the protocol configuration, if you need H2 but not H2C (cleartext), you must also configure SSL.
        //HTTP2协议需要配置SSL
        if (getHttp2() != null && getHttp2().isEnabled() && getSsl() != null && getSsl().isEnabled()) {
            return new HttpProtocol[] { HttpProtocol.H2, HttpProtocol.HTTP11 };
        }
        return new HttpProtocol[] { HttpProtocol.HTTP11 };
    }


    public void setLifecycleTimeout(Duration lifecycleTimeout) {
        this.lifecycleTimeout = lifecycleTimeout;
    }
}
