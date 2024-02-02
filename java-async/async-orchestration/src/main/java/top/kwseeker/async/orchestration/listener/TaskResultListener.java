package top.kwseeker.async.orchestration.listener;

public class TaskResultListener implements TaskListener {

    @Override
    public void onFinish(TaskFinishedEvent event) {
        //仅仅打印结果，实际业务请求需要重新定制
        System.out.println("TaskResultListener onFinish, result: " + event.getResult());
    }
}
