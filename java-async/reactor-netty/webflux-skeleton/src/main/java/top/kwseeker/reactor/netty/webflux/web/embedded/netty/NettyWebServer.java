package top.kwseeker.reactor.netty.webflux.web.embedded.netty;

import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import top.kwseeker.reactor.netty.webflux.http.server.reactive.ReactorHttpHandlerAdapter;
import top.kwseeker.reactor.netty.webflux.web.server.WebServer;
import top.kwseeker.reactor.netty.webflux.web.server.WebServerException;

import java.time.Duration;

/**
 * Reactor-Netty 的 HttpServer + 响应式请求处理器 HttpHandler + 路由配置
 */
public class NettyWebServer implements WebServer {

    private final HttpServer httpServer;
    //webflux提供了两种路由定义方式：
    //1 ReactorHttpHandlerAdapter 通过 HttpServer#handle() 方法绑定
    //2 NettyRouteProvider 通过 HttpServer#routes() 方法绑定
    //默认使用的这种方式, ReactorHttpHandlerAdapter 是入口类，实际处理器是 ServerManger中的 handler (HttpWebHandlerAdapter)
    private final ReactorHttpHandlerAdapter handlerAdapter;
    //这种方式默认并没有使用
    //private List<NettyRouteProvider> routeProviders = Collections.emptyList();
    private final Duration lifecycleTimeout ;

    private DisposableServer disposableServer;

    public NettyWebServer(HttpServer httpServer, ReactorHttpHandlerAdapter handlerAdapter, Duration lifecycleTimeout) {
        this.httpServer = httpServer;
        this.handlerAdapter = handlerAdapter;
        this.lifecycleTimeout = lifecycleTimeout;
    }

    @Override
    public void start() throws WebServerException {
        if (this.disposableServer == null) {
            try {
                HttpServer server = this.httpServer
                        .handle(this.handlerAdapter);
                if (this.lifecycleTimeout != null) {
                    this.disposableServer = server.bindNow(this.lifecycleTimeout);
                } else {
                    this.disposableServer = server.bindNow();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new WebServerException("Unable to start Netty", e);
            }
            System.out.println("Netty started on port(s): " + getPort());
            //webflux 额外开一个线程是因为主线程后面还有很多事要处理，不能阻塞到这里，
            //用非守护线程是避免主线程执行完毕退出后子线程也退出
            startDaemonAwaitThread(disposableServer);
        }
    }

    private void startDaemonAwaitThread(DisposableServer disposableServer) {
        Thread awaitThread = new Thread("server") {
            @Override
            public void run() {
                disposableServer.onDispose().block();
            }
        };
        awaitThread.setContextClassLoader(getClass().getClassLoader());
        awaitThread.setDaemon(false);
        awaitThread.start();
    }

    @Override
    public void stop() throws WebServerException {
        if (this.disposableServer != null) {
            if (this.lifecycleTimeout != null) {
                this.disposableServer.disposeNow(this.lifecycleTimeout);
            }
            else {
                this.disposableServer.disposeNow();
            }
            this.disposableServer = null;
        }
    }

    @Override
    public int getPort() {
        if (this.disposableServer != null) {
            return this.disposableServer.port();
        }
        return 0;
    }
}
