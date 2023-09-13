# Reactor Netty 源码分析

这里基于 v0.9.1.RELEASE分析, 主要参考drawio流程图，此文档只是对流程图的补充。



## reactor-netty 依赖

v0.9.1.RELEASE只是一个单模块项目和最新版本2.x.x差异还挺大的，代码量也差了一倍。

```
//新版本
com.google.code.findbugs:jsr305:3.0.2
io.micrometer:context-propagation:1.0.0
io.micrometer:micrometer-core:1.10.0
io.micrometer:micrometer-tracing:1.0.0
io.netty.incubator:netty-incubator-transport-native-io_uring:0.0.22.Final
io.netty:netty-codec-haproxy:4.1.97.Final
io.netty:netty-transport-native-kqueue:4.1.97.Final
io.projectreactor.addons:reactor-pool:1.0.2-SNAPSHOT
project reactor-netty-core
project reactor-netty-http
project reactor-netty-incubator-quic
```

可能由于 v0.9.1.RELEASE 版本太老了，下载依赖会报错：`Could not resolve org.springframework.build.gradle:propdeps-plugin:0.0.7`。`spring-io-plugin` 下载也报错，浏览器打开路径发现确实没有，但是看 SNAPSHOT 仓库还有，就添加 SNAPSHOT 仓库。

```groovy
buildscript {
  repositories {
    maven { url "https://repo.spring.io/plugins-snapshot" }	//添加这行
  	maven { url "https://repo.spring.io/plugins-release" }
  }
  dependencies {
	classpath 'org.springframework.build.gradle:propdeps-plugin:0.0.7-SNAPSHOT',	//修改版本号
			'io.spring.gradle:spring-io-plugin:0.0.4.BUILD-SNAPSHOT',				//修改版本号
			'com.google.gradle:osdetector-gradle-plugin:1.4.0'
  }
}
```

IDEA配置 Gradle JVM 修改：

修改成本地安装的JDK的版本。



### 启动流程

#### 服务器端初始化

主要就是创建Netty的ServerBootstrap实例，并初始化属性配置。

```java
ServerBootstrap createServerBootstrap() {
    return new ServerBootstrap()
        .option(ChannelOption.SO_REUSEADDR, true)
        .childOption(ChannelOption.AUTO_READ, false)
        .childOption(ChannelOption.TCP_NODELAY, true)
        .localAddress(new InetSocketAddress(DEFAULT_PORT));
}
```

**Netty ChannelOption 选项**（参考《UNIX网络编程》C7.5 通用套接字选项）：

```java
public static final ChannelOption<ByteBufAllocator> ALLOCATOR = valueOf("ALLOCATOR");
public static final ChannelOption<RecvByteBufAllocator> RCVBUF_ALLOCATOR = valueOf("RCVBUF_ALLOCATOR");
public static final ChannelOption<MessageSizeEstimator> MESSAGE_SIZE_ESTIMATOR = valueOf("MESSAGE_SIZE_ESTIMATOR");

public static final ChannelOption<Integer> CONNECT_TIMEOUT_MILLIS = valueOf("CONNECT_TIMEOUT_MILLIS");
@Deprecated
public static final ChannelOption<Integer> MAX_MESSAGES_PER_READ = valueOf("MAX_MESSAGES_PER_READ");
public static final ChannelOption<Integer> WRITE_SPIN_COUNT = valueOf("WRITE_SPIN_COUNT");
@Deprecated
public static final ChannelOption<Integer> WRITE_BUFFER_HIGH_WATER_MARK = valueOf("WRITE_BUFFER_HIGH_WATER_MARK");
@Deprecated
public static final ChannelOption<Integer> WRITE_BUFFER_LOW_WATER_MARK = valueOf("WRITE_BUFFER_LOW_WATER_MARK");
public static final ChannelOption<WriteBufferWaterMark> WRITE_BUFFER_WATER_MARK =
        valueOf("WRITE_BUFFER_WATER_MARK");

public static final ChannelOption<Boolean> ALLOW_HALF_CLOSURE = valueOf("ALLOW_HALF_CLOSURE");
public static final ChannelOption<Boolean> AUTO_READ = valueOf("AUTO_READ");

public static final ChannelOption<Boolean> AUTO_CLOSE = valueOf("AUTO_CLOSE");

public static final ChannelOption<Boolean> SO_BROADCAST = valueOf("SO_BROADCAST");
public static final ChannelOption<Boolean> SO_KEEPALIVE = valueOf("SO_KEEPALIVE");
public static final ChannelOption<Integer> SO_SNDBUF = valueOf("SO_SNDBUF");
public static final ChannelOption<Integer> SO_RCVBUF = valueOf("SO_RCVBUF");
//SO_REUSEADDR
//1 支持已存在连接的情况下重启服务器（主要用途）
//2 支持通过IP别名技术在同一端口上启动同一服务器的多个实例，比如用来实现托管多个HTTP服务器的网点，其他实例的请求最终都会转发给与主IP绑定的实例
//3 用于实现单个进程捆绑同一端口到多个套接字，TCP协议需要指定绑定不同的本地IP地址（即本地需要有多个IP地址）
//4 UDP协议下此选项可以支持同一IP同一端口捆绑多个套接字
public static final ChannelOption<Boolean> SO_REUSEADDR = valueOf("SO_REUSEADDR");
public static final ChannelOption<Integer> SO_LINGER = valueOf("SO_LINGER");
public static final ChannelOption<Integer> SO_BACKLOG = valueOf("SO_BACKLOG");
public static final ChannelOption<Integer> SO_TIMEOUT = valueOf("SO_TIMEOUT");

public static final ChannelOption<Integer> IP_TOS = valueOf("IP_TOS");
public static final ChannelOption<InetAddress> IP_MULTICAST_ADDR = valueOf("IP_MULTICAST_ADDR");
public static final ChannelOption<NetworkInterface> IP_MULTICAST_IF = valueOf("IP_MULTICAST_IF");
public static final ChannelOption<Integer> IP_MULTICAST_TTL = valueOf("IP_MULTICAST_TTL");
public static final ChannelOption<Boolean> IP_MULTICAST_LOOP_DISABLED =valueOf("IP_MULTICAST_LOOP_DISABLED");
//TCP套接字选项
//TCP_NODELAY: 开启会禁止TCP的Nagle算法，确保响应不会因为存在待确认数据而出现延迟, 参考：《UNIX网络编程》C7.9.2 的案例
public static final ChannelOption<Boolean> TCP_NODELAY = valueOf("TCP_NODELAY");

@Deprecated
public static final ChannelOption<Boolean> DATAGRAM_CHANNEL_ACTIVE_ON_REGISTRATION =
        valueOf("DATAGRAM_CHANNEL_ACTIVE_ON_REGISTRATION");
public static final ChannelOption<Boolean> SINGLE_EVENTEXECUTOR_PER_GROUP =
        valueOf("SINGLE_EVENTEXECUTOR_PER_GROUP");
```

> Nagle算法的目的在于减少广域网(WAN)上小分组的数目。该算法指出:如果某个给定连接上有待确认数据(outstanding data),
> 那么原本应该作为用户写操作之响应的在该连接上立即发送相应小分组的行为就不会发生,直到现有数据被确认为止。这里“小”分组的定义就是小于MSS的任何分组。TCP总是尽可能地发送最大大小的分组,Nagle算法的目的在于防止一个连接在任何时刻有多个小分组待确认。
>
> MSS（Maximum Segment Size，最大报文长度），是TCP协议定义的一个选项，MSS选项用于在TCP连接建立时，收发双方协商通信时每一个报文段所能承载的最大数据长度。
>
> MTU 最大传输单元（Maximum Transmission(传输) Unit，MTU）用来通知对方所能接受数据服务单元的最大尺寸，说明发送方能够接受的有效载荷大小。

#### 端口绑定与启动

#### 阻塞等待服务器关闭



### 请求处理流程



