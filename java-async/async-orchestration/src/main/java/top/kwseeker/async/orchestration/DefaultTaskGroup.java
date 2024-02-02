package top.kwseeker.async.orchestration;

import top.kwseeker.async.orchestration.pool.ThreadPoolManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * 任务组
 */
public class DefaultTaskGroup implements TaskGroup {

    private static final int TIMEOUT_NO_LIMIT = 0;

    // 头部任务，即没有依赖的一组任务
    private final Map<String, Task> heads = new HashMap<>();
    // 尾部任务，依赖链中最后的一组任务, 如果一个任务不被其他任务依赖，它就是最后的任务
    private final Map<String, Task> tails = new HashMap<>();
    // 任务组中所有任务Map，任务名 -> 任务
    private final Map<String, Task> taskMap = new HashMap<>();
    // 超时时间
    private final int executeTimeoutMs;

    public DefaultTaskGroup(int executeTimeoutMs) {
        if (executeTimeoutMs < TIMEOUT_NO_LIMIT) {
            throw new RuntimeException("invalid execution timeout setting");
        }
        this.executeTimeoutMs = executeTimeoutMs;
    }

    @Override
    public void appendTask(Task preTask, Task newTask) {
        appendTask(preTask.getName(), newTask);
    }

    @Override
    public void appendTask(String preTaskName, Task newTask) {
        boolean exist = false;
        if (taskMap.containsKey(newTask.getName())) {
            //1 newTask如果已经存在，可能是newTask依赖多个前置任务
            if (taskMap.get(newTask.getName()) != newTask) {
                throw new RuntimeException("Task named " + newTask.getName() + " already appended");
            }
            exist = true;
        } else {
            taskMap.put(newTask.getName(), newTask);
            tails.put(newTask.getName(), newTask);
            if (preTaskName == null) {
                heads.put(newTask.getName(), newTask);
            }
        }

        if (exist) {    //任务之前就存在，再调用此方法是为了增加前置依赖任务
            if (preTaskName == null) {
                throw new RuntimeException("Task named " + newTask.getName() + " already appended，cannot set as heads");
            }
            heads.remove(newTask.getName());
        }

        if (preTaskName != null) {
            tails.remove(preTaskName);
            Task preTask = taskMap.get(preTaskName);
            if (preTask == null) {
                throw new RuntimeException("Task named " + preTaskName + " not exist");
            }
            preTask.appendPostTask(newTask);
            newTask.appendPrevTask(preTask);
        }
    }

    @Override
    public void start() {
        //弱依赖额外的处理 TODO

        //将头部任务全部丢给线程池执行，每执行完一个任务在任务完成时以回调的方式提交后置任务
        for (Task head : heads.values()) {
            head.trySubmit(null);
        }
    }

    @Override
    public void cancel() {
        //通过Future#cancel()方法取消任务
        for (Task value : taskMap.values()) {
            value.tryCancel();
        }
    }

    //同步等待执行完毕
    public boolean awaitTimeout() {
        return awaitTimeout(executeTimeoutMs);
    }

    //也可以用CountDownLatch、Condition、信号量等方式
    public boolean awaitTimeout(int timeout) {
        Set<String> tailTaskNames = new HashSet<>(tails.keySet());
        long deadline = System.currentTimeMillis() + timeout;
        while(System.currentTimeMillis() < deadline) {
            tailTaskNames.removeIf(tailTaskName -> tails.get(tailTaskName).finished());
            if (tailTaskNames.isEmpty()) {
                return true;
            }
            Thread.yield();
        }

        //超时，给所有还在执行的线程发中断信号
        System.out.println("execute timeout, now cancel unfinished task");
        cancel();
        return false;
    }

    @Override
    public boolean finished() {
        for (Task task : tails.values()) {
            if (!task.finished()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ExecutorService getThreadPool() {
        return ThreadPoolManager.INSTANCE.getThreadPool();
    }
}
