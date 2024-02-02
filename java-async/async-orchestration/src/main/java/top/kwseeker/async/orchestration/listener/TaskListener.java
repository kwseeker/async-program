package top.kwseeker.async.orchestration.listener;

public interface TaskListener {

    /**
     * 任务完成监听
     */
    void onFinish(TaskFinishedEvent event);
}
