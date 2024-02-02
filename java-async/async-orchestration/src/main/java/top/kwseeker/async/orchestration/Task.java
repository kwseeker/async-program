package top.kwseeker.async.orchestration;

import top.kwseeker.async.orchestration.listener.TaskFinishedEvent;
import top.kwseeker.async.orchestration.listener.TaskListener;

import java.util.concurrent.Future;

/**
 * 任务接口
 */
public interface Task extends Runnable {

    String getName();

    //添加前置任务
    void appendPrevTask(Task prevTask);

    //添加后置任务
    void appendPostTask(Task postTask);

    void setFuture(Future<?> future);

    boolean started();
    boolean finished();

    /**
     * 添加监听器
     */
    void addListener(TaskListener listener);

    /**
     * 触发后置任务执行
     */
    void triggerPostTasks(TaskFinishedEvent event);

    void trySubmit(TaskFinishedEvent event);

    void tryCancel();

    //打印任务流图
}
