package top.kwseeker.reactor.netty.webflux.web.reactive.server;

import top.kwseeker.reactor.netty.webflux.web.server.AbstractConfigurableWebServerFactory;

/**
 * 并没有拓展Web服务器异步方面的默认实现，这个类可以去掉，原代码估计是为了后面可能有什么拓展的东西而保留的
 */
public abstract class AbstractReactiveWebServerFactory extends AbstractConfigurableWebServerFactory
        implements ConfigurableReactiveWebServerFactory {

    public AbstractReactiveWebServerFactory() {
    }

    public AbstractReactiveWebServerFactory(int port) {
        super(port);
    }
}
