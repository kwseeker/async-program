package top.kwseeker.async.orchestration;

import java.util.concurrent.ExecutorService;

public interface TaskGroup {

    default void appendTask(Task newTask) {
        appendTask((String) null, newTask);
    }

    /**
     * 追加任务，即任务编排接口
     * 如果preTask为空，则作为头部任务
     *
     * @param preTaskName 前置任务名
     * @param newTask     新增任务
     */
    void appendTask(String preTaskName, Task newTask);

    void appendTask(Task preTask, Task newTask);

    /**
     * 任务组启动，从头部任务开始加入线程池执行
     */
    void start();

    /**
     * 取消任务组任务
     */
    void cancel();

    boolean awaitTimeout();

    /**
     * 同步等待执行完成
     * @param timeout   超时时间ms
     * @return          是否正常退出
     */
    boolean awaitTimeout(int timeout);

    /**
     * 所有尾部任务都结束才算是任务组执行结束
     */
    boolean finished();

    ExecutorService getThreadPool();
}
