package top.kwseeker.async.orchestration;

import top.kwseeker.async.orchestration.listener.TaskFinishedEvent;
import top.kwseeker.async.orchestration.listener.TaskListener;
import top.kwseeker.async.orchestration.listener.TaskTriggerListener;
import top.kwseeker.async.orchestration.pool.ThreadPoolManager;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public abstract class AbstractTask implements Task {

    //任务名，通过这个字段唯一代表一个任务
    private final String name;
    //前置依赖任务，当前任务依赖的任务
    private final Map<String, Task> prevTasks = new HashMap<>();
    //后置依赖任务，当前任务被依赖
    private final Map<String, Task> postTasks = new HashMap<>();
    //当前任务是否弱依赖于prevTasks,默认强依赖
    private final boolean weakDependency;
    private final AtomicReference<State> state = new AtomicReference<>(State.CREATED);

    //当前任务逻辑
    private final Function<Object, Object> func;
    private Object param;
    private Object result;
    private Future<Object> future;

    private final List<TaskListener> listeners = new ArrayList<>();

    public AbstractTask(String name, boolean weakDependency, Function<Object, Object> func) {
        this(name, weakDependency, func, null);
    }

    public AbstractTask(String name, boolean weakDependency, Function<Object, Object> func, Object param) {
        this.name = name;
        this.weakDependency = weakDependency;
        this.func = func;
        this.param = param;
        listeners.add(new TaskTriggerListener(this));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void appendPrevTask(Task prevTask) {
        appendTask(prevTasks, prevTask);
    }

    @Override
    public void appendPostTask(Task postTask) {
        appendTask(postTasks, postTask);
    }

    private void appendTask(Map<String, Task> tasks, Task task) {
        if (tasks == null) {
            tasks = new HashMap<>();
        }
        if (tasks.containsKey(task.getName())) {
            if (tasks.get(task.getName()) == task) {
                return;
            }
            throw new RuntimeException("append prevTask with same name:{" + task.getName() + "} but different instance to task:{" + name + "}");
        } else {
            tasks.put(task.getName(), task);
        }
    }

    @Override
    public void run() {
        try {
            if (!state.compareAndSet(State.CREATED, State.RUNNING)) {
                return;
            }
            //执行当前任务逻辑
            result = func.apply(param);
            System.out.printf("execute task: %s, param: %s, result: %s in thread: %s\n",
                    name, param, result, Thread.currentThread().getName());
        } finally {
            state.compareAndSet(State.RUNNING, State.FINISHED);

            //通过监听器回调
            for (TaskListener listener : listeners) {
                listener.onFinish(new TaskFinishedEvent(result));
            }
        }
    }

    @Override
    public void setFuture(Future<?> future) {
        this.future = (Future<Object>) future;
    }

    @Override
    public void addListener(TaskListener listener) {
        listeners.add(listener);
    }

    @Override
    public boolean started() {
        return state.get().getState() > State.CREATED.getState();
    }

    @Override
    public boolean finished() {
        return state.get() == State.FINISHED;
    }

    @Override
    public void triggerPostTasks(TaskFinishedEvent event) {
        postTasks.forEach((name, task) -> {
            task.trySubmit(event);
        });
    }

    @Override
    public void trySubmit(TaskFinishedEvent event) {
        //检查任务是否已经开始或完成, 可能任务已经在其他的依赖路径中已经被触发了
        if (started()) {
            return;
        }

        if (weakDependency) {
            //需要额外检查: TODO 如果其依赖链中所有尾部任务已经执行完毕，不需要提交

            //弱依赖直接提交
            //TODO 参数传递可能需要根据实际需要重新实现
            if (event != null) {
                param = event.getResult();
            }
            future = (Future<Object>) ThreadPoolManager.submit(this);
        } else {
            //强依赖，判断所有前置依赖任务是否已经执行完毕
            for (Task task : prevTasks.values()) {
                if (!task.finished()) {
                    //任意一个未完成都不提交
                    return;
                }
            }
            //TODO 可能任务依赖所有前置任务的执行结果还需要包装一下
            if (event != null) {
                param = event.getResult();
            }
            future = (Future<Object>) ThreadPoolManager.submit(this);
        }
    }

    @Override
    public void tryCancel() {
        if (future != null && state.get() == State.RUNNING) {
            future.cancel(true);
            System.out.println("cancel task: " + name);
        }
    }

    enum State {
        CREATED(1),
        RUNNING(2),
        FINISHED(3),
        CANCELED(4);

        private final int state;

        State(int state) {
            this.state = state;
        }

        public int getState() {
            return state;
        }
    }
}
