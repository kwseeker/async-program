# Executor 族线程池

**线程池相关类**：

![线程池相关类](../img/executor-class-uml.png)

**线程池相关类方法**：![线程池相关类方法](../img/executor-class-uml-with-method.png)

Java并发包线程池包含下面实现：

```java
ThreadPoolExecutor
	ScheduledThreadPoolExecutor
ForkJoinPool
DelegatedExecutorService in Executors		//线程池代理,并没有创建线程池
	DelegatedScheduledExecutorService in Executors
	FinalizableDelegatedExecutorService in Executors
```



## ThreadPoolExecutor

### 接口方法

#### ExecutorService

 



### ScheduledThreadPoolExecutor



## ForkJoinPool

原理是将任务层层拆分成不再可分割的小任务，在单独的线程中执行各个小任务，最后结果归并。

适合用于计算密集型任务，如果存在I/O，线程间同步，sleep() 等会造成线程长时间阻塞的情况时，最好配合使用 ManagedBlocker。



## 拓展

### 动态化配置的线程池

看到《美团技术团队》有一个在ThreadPoolExecutor基础上开发的动态化配置的线程池，

技术架构：

![](../img/meituan-dynamic-configurable-threadpool.png)

业务架构：

![](../img/meituan-dynamic-configurable-threadpool-biz-arch.png)

好奇**配置修改**那块是怎么做的？
是对已处于工作状态的线程池的配置进行修改？
还是会用新的配置创建新的线程池，关闭旧的线程池，然后把任务切换到新的线程池？

前者可能需要对ThreadPoolExecutor每个实现细节都有清晰的认识，可能需要进行封装，但是看到原文章说“**JDK允许线程池使用方通过ThreadPoolExecutor的实例来动态设置线程池的核心策略**”，可能不需要额外的封装；而且API确实提供了对应配置参数的setter方法（除了没有修改任务队列的setter方法），应该确实是支持运行中动态配置的。

```java
//除了修改任务队列的其他的参数都支持动态修改
void	setCorePoolSize(int corePoolSize)
void	setKeepAliveTime(long time, TimeUnit unit)
void	setMaximumPoolSize(int maximumPoolSize)
void	setRejectedExecutionHandler(RejectedExecutionHandler handler)
void	setThreadFactory(ThreadFactory threadFactory)
```

后者实现较简单。

> 遗留问题：
>
> 线程级联监控是什么意思？类似日志链路跟踪么？监控线程调用链路？
>
> 队列长度修改？各种阻塞队列（任务队列）本身都不支持修改队列长度，需要思考下怎么实现。
>
> 其他部分没有太大难度，可以自己尝试实现下。



