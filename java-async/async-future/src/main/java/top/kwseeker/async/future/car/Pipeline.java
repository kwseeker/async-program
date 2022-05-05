package top.kwseeker.async.future.car;

import net.sf.cglib.proxy.Enhancer;

import java.nio.channels.Pipe;

//汽车制造流水线
public class Pipeline {

    //private String frame;
    //private String wheel;
    //private String engine;

    public static Pipeline getProxy() {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(Pipeline.class);
        enhancer.setCallback(new TimeRecordInterceptor());
        return  (Pipeline)enhancer.create();
    }

    @TimeRecord
    public String buildFrame() {
        System.out.print("build frame ... [" + Thread.currentThread().getName() + "]");
        TimeUtil.sleep(1000);
        return "arch V1.0";
    }

    @TimeRecord
    public boolean testFrame(String frame) {
        TimeUtil.sleep(2000);
        System.out.println("test frame ... [" + Thread.currentThread().getName() + "]");
        return true;
    }

    @TimeRecord
    public String buildWheel() {
        TimeUtil.sleep(500);
        System.out.println("build wheel ... [" + Thread.currentThread().getName() + "]");
        return "wheel V1.0";
    }

    @TimeRecord
    public boolean testWheel(String wheel) {
        TimeUtil.sleep(1000);
        System.out.println("test wheel ... [" + Thread.currentThread().getName() + "]");
        return true;
    }

    @TimeRecord
    public String buildEngine() {
        TimeUtil.sleep(2000);
        System.out.println("build engine ... [" + Thread.currentThread().getName() + "]");
        return "engine V1.0";
    }

    @TimeRecord
    public boolean testEngine(String engine) {
        TimeUtil.sleep(2500);
        System.out.println("test engine ... [" + Thread.currentThread().getName() + "]");
        return true;
    }

    @TimeRecord
    public String buildCar() {
        TimeUtil.sleep(500);
        System.out.println("build car ... [" + Thread.currentThread().getName() + "]");
        return "BMW";
    }

    // 车辆测试

    @TimeRecord
    public boolean testArchitecture(String car) {
        TimeUtil.sleep(1500);
        System.out.println("test architecture ... [" + Thread.currentThread().getName() + "]");
        return true;
    }

    @TimeRecord
    public boolean testSecurity(String car) {
        TimeUtil.sleep(1500);
        System.out.println("test security ... [" + Thread.currentThread().getName() + "]");
        return true;
    }

    @TimeRecord
    public boolean testPerformance(String car) {
        TimeUtil.sleep(1500);
        System.out.println("test performance ... [" + Thread.currentThread().getName() + "]");
        return true;
    }
}
