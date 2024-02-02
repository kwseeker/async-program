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

    //获取状态
    boolean started();
    boolean finished();

    //添加监听器
    void addListener(TaskListener listener);

    /**
     * 尝试提交任务到线程池，
     * 如果任务是强依赖需要判断前置任务是否都执行，都执行了才提交
     * 如果是弱依赖，可以直接提交到线程池
     */
    void trySubmit(TaskFinishedEvent event);

    void tryCancel();

    //打印任务流图
}
