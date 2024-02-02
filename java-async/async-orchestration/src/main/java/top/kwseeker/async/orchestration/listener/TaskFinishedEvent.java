package top.kwseeker.async.orchestration.listener;

public class TaskFinishedEvent extends TaskEvent {

    private final Object result;

    public TaskFinishedEvent(Object result) {
        this.result = result;
    }

    public Object getResult() {
        return result;
    }
}
