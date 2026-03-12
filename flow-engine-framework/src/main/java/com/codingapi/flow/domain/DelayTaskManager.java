package com.codingapi.flow.domain;

import com.codingapi.flow.service.impl.FlowDelayTriggerService;
import com.codingapi.flow.session.IRepositoryHolder;
import lombok.Getter;

import java.util.*;

/**
 * 延迟任务上下文管理对象
 * <p>
 * 在分布式模式若都在启动延迟任务时，会导致延迟任务重复执行
 */
public class DelayTaskManager {

    @Getter
    private final static DelayTaskManager instance = new DelayTaskManager();

    private final List<DelayJob> delayJobs;

    private DelayTaskManager() {
        this.delayJobs = new ArrayList<>();
    }

    /**
     * 加载任务并执行
     */
    public void start(IRepositoryHolder repositoryHolder) {
        System.out.println("flow delay task starting...");
        List<DelayTask> delayTasks = repositoryHolder.findDelayTasks();
        if (delayTasks != null && !delayTasks.isEmpty()) {
            for (DelayTask delayTask : delayTasks) {
                this.addTask(delayTask,repositoryHolder);
            }
        }
        System.out.println("flow delay task started");
    }


    /**
     * 关闭全部任务
     */
    public void close() {
        System.out.println("flow delay task closing...");
        for (DelayJob delayJob : delayJobs) {
            delayJob.close();
        }
        this.delayJobs.clear();
        System.out.println("flow delay task closed");
    }

    /**
     * 添加任务队列
     */
    public void addTask(DelayTask task,IRepositoryHolder repositoryHolder) {
        repositoryHolder.saveDelayTask(task);
        this.delayJobs.add(new DelayJob(task,repositoryHolder));
    }

    /**
     * 延期任务队列
     */
    private static class DelayJob {
        private final DelayTask task;
        private final Timer timer;

        public DelayJob(DelayTask task,IRepositoryHolder repositoryHolder) {
            this.task = task;
            this.timer = new Timer();
            this.start(repositoryHolder);
        }

        public void close() {
            this.timer.cancel();
        }

        private void start(IRepositoryHolder repositoryHolder) {
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {

                    FlowDelayTriggerService flowDelayTriggerService = repositoryHolder.createDelayTriggerService(task);

                    flowDelayTriggerService.trigger();

                    repositoryHolder.deleteDelayTask(task);
                }
            }, new Date(task.getTriggerTime()));
        }
    }
}
