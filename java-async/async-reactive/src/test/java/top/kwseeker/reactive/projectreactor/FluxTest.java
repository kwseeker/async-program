package top.kwseeker.reactive.projectreactor;

import org.junit.Test;
import reactor.core.publisher.Flux;

public class FluxTest {

    @Test
    public void testFluxCollect() {
        Flux.just(1,2,3,4,5)
                .collectList()  //将Flux所有发送的元素搜集起来（存到List），当Flux结束时使用这些数据创建Mono
                .map(list -> {
                    DataBuffer dataBuffer = new DataBuffer(list.size());
                    for (int i = 0; i < list.size(); i++) {
                        dataBuffer.add(i, list.get(i));
                    }
                    return dataBuffer;
                })
                .flux() //将当前Mono转成Flux
                .subscribe(dataBuffer -> System.out.println(dataBuffer.getArr().length));
    }

    static class DataBuffer {
        Integer[] arr;

        public DataBuffer(int size) {
            this.arr = new Integer[size];
        }

        public void add(int index, Integer value) {
            this.arr[index] = value;
        }

        public Integer[] getArr() {
            return arr;
        }
    }
}
