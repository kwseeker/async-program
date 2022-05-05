package top.kwseeker.async.future.car;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.time.LocalTime;

public class TimeRecordInterceptor implements MethodInterceptor {

    @Override
    public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        if (!method.isAnnotationPresent(TimeRecord.class)) {
            return method.invoke(o, args);
        }
        LocalTime begin = LocalTime.now();
        System.out.println("--------> begin:\t" + begin.toString() + "[" + Thread.currentThread().getName() + "]");
        Object ret = methodProxy.invokeSuper(o, args);
        LocalTime end = LocalTime.now();
        System.out.println("--------> end:\t" + end.toString() + "[" + Thread.currentThread().getName() + "]");
        return ret;
    }
}