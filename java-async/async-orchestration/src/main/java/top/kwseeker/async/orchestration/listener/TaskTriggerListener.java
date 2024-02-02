package top.kwseeker.async.orchestration.listener;

import top.kwseeker.async.orchestration.Task;

public class TaskTriggerListener implements TaskListener {

    //监听的任务
    Task task;

    public TaskTriggerListener(Task task) {
        this.task = task;
    }

    /**
     * 用于触发下一个任务执行
     * 当前任务的后置任务可能有0、1或多个，后置任务的状态可能是强依赖、弱依赖
     */
    @Override
    public void onFinish(TaskFinishedEvent event) {

        task.triggerPostTasks(event);
    }
}
