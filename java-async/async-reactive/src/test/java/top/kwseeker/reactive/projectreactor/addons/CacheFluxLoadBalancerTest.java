package top.kwseeker.reactive.projectreactor.addons;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.Test;
import reactor.cache.CacheFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.util.Arrays;
import java.util.List;

public class CacheFluxLoadBalancerTest {

    //public static final String SERVICE_INSTANCE_CACHE_NAME = "CachingServiceInstanceListSupplierCache";

    @Test
    public void testMockLoadBalancerCacheFlux() {
        String serviceId = "some-service";
        Cache<String, List<ServiceInstance>> cache = Caffeine.newBuilder()
                .maximumSize(100) // 设置最大缓存大小
                .build();

        //CacheFlux 已经 Deprecated
        Flux<List<ServiceInstance>> serviceInstances = CacheFlux
                .lookup(key -> {
                    //原来的代码使用Spring CacheManager 做了一层适配，这里省略直接用 Caffeine 的接口
                    //Cache cache = cacheManager.getCache(SERVICE_INSTANCE_CACHE_NAME);
                    List<ServiceInstance> list = cache.getIfPresent(key);
                    if (list == null || list.isEmpty()) {
                        System.out.println("list is null");
                        return Mono.empty();
                    }
                    //materialize() 方法，可以获得更丰富的事件响应信息，包括事件的类型和错误信息，从而更好地处理事件流的状态和错误情况
                    return Flux.just(list).materialize().collectList();
                }, serviceId)
                //.onCacheMissResume(delegate.get().take(1))
                .onCacheMissResume(() -> {
                    System.out.println("call onCacheMissResume, get ServiceInstances from DiscoveryService");
                    List<ServiceInstance> serviceInstancesFromDiscoveryService = Arrays.asList(
                            new ServiceInstance("1", serviceId, "192.168.1.1", 8080),
                            new ServiceInstance("2", serviceId, "192.168.1.2", 8080));
                    return Flux.just(serviceInstancesFromDiscoveryService);
                })
                .andWriteWith((key, signals) -> Flux.fromIterable(signals).dematerialize().
                        doOnNext(instances -> {
                            System.out.println("key -> " + key);
                            cache.put(key, (List<ServiceInstance>) instances);
                        }).then());

        Mono<List<ServiceInstance>> next = serviceInstances.next().map(serviceInstances1 -> {
            for (ServiceInstance serviceInstance : serviceInstances1) {
                System.out.println(serviceInstance);
            }
            return serviceInstances1;
        });
        next.block();

        //再执行一次
        System.out.println("retry ---------------------");
        next = serviceInstances.next().map(serviceInstances1 -> {
            for (ServiceInstance serviceInstance : serviceInstances1) {
                System.out.println(serviceInstance);
            }
            return serviceInstances1;
        });
        next.block();
    }

    @Test
    public void testMaterialize() {
        Flux<Integer> flux = Flux.just(1, 2, 3);
        flux.materialize().subscribe(signal -> {
                    if (signal.getType() == SignalType.ON_NEXT) {
                        System.out.println("Next signal: " + signal.get());
                    } else if (signal.getType() == SignalType.ON_ERROR) {
                        System.out.println("Error signal: " + signal.getThrowable());
                    } else {
                        System.out.println("Complete signal");
                    }
                });
    }

    @Data
    @AllArgsConstructor
    static class ServiceInstance {
        private String instanceId;
        private String serviceId;
        private String host;
        private int port;
    }
}
