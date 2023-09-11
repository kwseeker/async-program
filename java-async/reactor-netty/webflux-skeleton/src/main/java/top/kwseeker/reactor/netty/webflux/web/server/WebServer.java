package top.kwseeker.reactor.netty.webflux.web.server;

/**
 * 通用的 WebServer 接口定义
 */
public interface WebServer {

    void start() throws WebServerException;

    void stop() throws WebServerException;

    int getPort();
}
