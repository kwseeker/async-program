package top.kwseeker.reactive.projectreactor;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PublisherSelfDefinedSequenceTest {

    @Test
    public void testGenerate() {
        final AtomicInteger count = new AtomicInteger(1);
        Flux.generate(sink -> {
                    sink.next(count.get() + " : " + new Date());
                    ThreadUtil.sleep(100);
                    //sink.next(count.getAndIncrement() + " : " + new Date());    //尝试再次调用，会发现会报异常 java.lang.IllegalStateException: More than one call to onNext
                    //ThreadUtil.sleep(100);
                    if (count.getAndIncrement() >= 5) {
                        sink.complete();
                    }
                })
                //.subscribeOn(Schedulers.parallel())   //generate()只能同步执行，加上这个也无效
                .log()
                .subscribe(System.out::println);
    }

    @Test
    public void testCreate() {
        final AtomicInteger count = new AtomicInteger(1);
        Flux<Object> objectFlux = Flux.create(sink -> {
                    while (count.getAndIncrement() < 5) {
                        sink.next(count.get() + " : " + new Date());
                        ThreadUtil.sleep(100);
                    }
                    sink.complete();
                })
                .subscribeOn(Schedulers.parallel()) //可同步可异步
                .onBackpressureError()              //也支持背压
                .log();
        objectFlux.subscribe(System.out::println);

        ThreadUtil.sleep(1000);
    }

    @Test
    public void testCreateReceiveOuterEvent() {
        OuterEventSource eventSource = new OuterEventSource();
        Flux.create(sink -> {
            eventSource.register(new OuterEventListener() {
                @Override
                public void onNewEvent(Event event) {
                    if (!sink.isCancelled()) {
                        sink.next(event);
                    }
                }

                @Override
                public void onEventStopped() {
                    if (!sink.isCancelled()) {
                        sink.complete();
                    }
                }
            });
        }).subscribe(event -> {
            Event e = (Event) event;
            System.out.println(e.getTimestamp() + " " + e.getMessage());
        });

        for (int i = 0; i < 5; i++) {
            eventSource.newEvent(new Event(new Date(), "Event-" + i));
            ThreadUtil.sleep(100);
        }
        eventSource.eventStopped();

        ThreadUtil.sleep(1000);
    }

    @Test
    public void testPush() {
        //Flux<Integer> created = Flux.push(s -> {
        //    s.onRequest(n -> {
        //        onRequest.incrementAndGet();
        //        assertThat(n).isEqualTo(Long.MAX_VALUE);
        //        for (int i = 0; i < 5; i++) {
        //            s.next(index.getAndIncrement());
        //        }
        //        s.complete();
        //    });
        //}, FluxSink.OverflowStrategy.BUFFER);
    }

    //@Test
    //public void testUsing() {
    //    Flux.using()
    //}


    static class OuterEventSource {
        private final List<OuterEventListener> listeners;

        public OuterEventSource() {
            this.listeners = new ArrayList<>();
        }

        public void register(OuterEventListener listener) {
            listeners.add(listener);
        }

        public void newEvent(Event event) {
            for (OuterEventListener listener : listeners) {
                listener.onNewEvent(event);
            }
        }

        public void eventStopped() {
            for (OuterEventListener listener : listeners) {
                listener.onEventStopped();
            }
        }
    }

    static class Event {
        private Date timestamp;
        private String message;

        public Event() {
        }

        public Event(Date timestamp, String message) {
            this.timestamp = timestamp;
            this.message = message;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public String getMessage() {
            return message;
        }
    }

    interface OuterEventListener {
        void onNewEvent(Event event);
        void onEventStopped();
    }
}
