# Reactor Addons

[github](https://github.com/reactor/reactor-addons)

是Reactor项目的拓展，包含两个模块：

+ reactor-adapter

  用于适配其他响应式框架的接口，比如RxJava 2 Observable、Completable、Flowable、Single、 Maybe、 Scheduler 以及 SWT Scheduler、Akka Scheduler ...

+ reactor-extra

  拓展的操作和处理器，包括从数值源计算总和、平均值、最小值或最大值的数学运算。

  

## 中间件中的应用

### CacheFlux

Spring Cloud LoadBalancer  CachingServiceInstanceListSupplier 中有使用 CacheFlux 用于实现”先尝试从缓存中获取服务实例列表，缓存没有再从后面的ServiceInstanceListSupplier获取，最后将获取的服务实例列表写入缓存”的逻辑。

```java
Flux<List<ServiceInstance>> serviceInstances = CacheFlux
    //1 从缓存中查询
    .lookup(key -> {
        //原来的代码使用Spring CacheManager 做了一层适配，这里省略直接用 Caffeine 的接口
        //Cache cache = cacheManager.getCache(SERVICE_INSTANCE_CACHE_NAME);
        List<ServiceInstance> list = cache.getIfPresent(key);
        if (list == null || list.isEmpty()) {
            System.out.println("list is null");
            return Mono.empty();
        }
        return Flux.just(list).materialize().collectList();
    }, serviceId)
    //2 缓存没有再从后面的ServiceInstanceListSupplier获取
    //.onCacheMissResume(delegate.get().take(1))
    .onCacheMissResume(() -> {
        //这里是简化的逻辑
        System.out.println("call onCacheMissResume, get ServiceInstances from DiscoveryService");
        List<ServiceInstance> serviceInstancesFromDiscoveryService = Arrays.asList(
            new ServiceInstance("1", serviceId, "192.168.1.1", 8080),
            new ServiceInstance("2", serviceId, "192.168.1.2", 8080));
        return Flux.just(serviceInstancesFromDiscoveryService);
    })
    //3 再写入缓存
    .andWriteWith((key, signals) -> Flux.fromIterable(signals).dematerialize().
                  doOnNext(instances -> {
                      cache.put(key, (List<ServiceInstance>) instances);
                  }).then());
```

参考：java-async/async-reactive CacheFluxLoadBalancerTest.java

