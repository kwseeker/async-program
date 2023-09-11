package top.kwseeker.reactor.netty.webflux;

import top.kwseeker.reactor.netty.webflux.web.embedded.netty.NettyReactiveWebServerFactory;
import top.kwseeker.reactor.netty.webflux.web.reactive.DispatcherHandler;
import top.kwseeker.reactor.netty.webflux.web.reactive.context.ServerManager;
import top.kwseeker.reactor.netty.webflux.web.reactive.server.ReactiveWebServerFactory;
import top.kwseeker.reactor.netty.webflux.web.server.HttpWebHandlerAdapter;

public class App {

    public static void main(String[] args) {
        ReactiveWebServerFactory webServerFactory = new NettyReactiveWebServerFactory();
        ServerManager serverManager = ServerManager.get(webServerFactory);
        HttpWebHandlerAdapter httpHandler = new HttpWebHandlerAdapter(new DispatcherHandler());
        ServerManager.start(serverManager, httpHandler);
    }
}
