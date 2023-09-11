package top.kwseeker.reactor.netty.webflux.web.server;

import java.net.InetAddress;

/**
 * 包含通用配置的WebServer抽象工厂
 * 基于不同服务器的特异化的配置在embedded包定义
 */
public abstract class AbstractConfigurableWebServerFactory implements ConfigurableWebServerFactory {

    private int port = 8080;
    private InetAddress address;
    //是否支持HTTP2协议
    private Http2 http2;
    private Ssl ssl;

    public AbstractConfigurableWebServerFactory() {
    }

    public AbstractConfigurableWebServerFactory(int port) {
        this.port = port;
    }


    public int getPort() {
        return port;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    public InetAddress getAddress() {
        return address;
    }

    @Override
    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public Http2 getHttp2() {
        return http2;
    }

    @Override
    public void setHttp2(Http2 http2) {
        this.http2 = http2;
    }

    public Ssl getSsl() {
        return this.ssl;
    }

    @Override
    public void setSsl(Ssl ssl) {
        this.ssl = ssl;
    }
}
