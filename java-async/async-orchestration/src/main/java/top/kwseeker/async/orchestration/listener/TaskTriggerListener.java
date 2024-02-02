package top.kwseeker.async.orchestration.listener;

import top.kwseeker.async.orchestration.Task;

import java.util.Map;

public class TaskTriggerListener implements TaskListener {

    //后置任务
    Map<String, Task> postTasks;

    public TaskTriggerListener(Map<String, Task> postTasks) {
        this.postTasks = postTasks;
    }

    /**
     * 用于触发下一个任务执行
     * 当前任务的后置任务可能有0、1或多个，后置任务的状态可能是强依赖、弱依赖
     */
    @Override
    public void onFinish(TaskFinishedEvent event) {
        postTasks.forEach((name, task) -> {
            //尝试提交任务到线程池，因为如果任务是强依赖需要判断前置任务是否都执行，都执行了才提交
            //如果是弱依赖，可以直接提交到线程池
            task.trySubmit(event);
        });
    }
}
