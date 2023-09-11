package top.kwseeker.reactor.netty.webflux.web.reactive.context;

import org.junit.Test;
import top.kwseeker.reactor.netty.webflux.web.embedded.netty.NettyReactiveWebServerFactory;
import top.kwseeker.reactor.netty.webflux.web.reactive.DispatcherHandler;
import top.kwseeker.reactor.netty.webflux.web.reactive.server.ReactiveWebServerFactory;
import top.kwseeker.reactor.netty.webflux.web.server.HttpWebHandlerAdapter;

public class ServerManagerTest {

    @Test
    public void testStartReactiveWebServer() {
        //启动服务器
        ReactiveWebServerFactory webServerFactory = new NettyReactiveWebServerFactory();
        ServerManager serverManager = ServerManager.get(webServerFactory);
        HttpWebHandlerAdapter httpHandler = new HttpWebHandlerAdapter(new DispatcherHandler());
        ServerManager.start(serverManager, httpHandler);
    }
}