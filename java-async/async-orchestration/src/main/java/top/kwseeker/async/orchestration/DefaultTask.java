package top.kwseeker.async.orchestration;

import java.util.function.Function;

public class DefaultTask extends AbstractTask {

    public DefaultTask(String name, Function<Object, Object> func) {
        this(name, false, func, null);
    }

    public DefaultTask(String name, Function<Object, Object> func, Object param) {
        this(name, false, func, param);
    }

    public DefaultTask(String name, boolean weakDependency, Function<Object, Object> func) {
        this(name, weakDependency, func, null);
    }

    public DefaultTask(String name, boolean weakDependency, Function<Object, Object> func, Object param) {
        super(name, weakDependency, func, param);
    }
}
