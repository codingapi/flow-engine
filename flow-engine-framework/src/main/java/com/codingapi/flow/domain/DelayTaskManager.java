package com.codingapi.flow.domain;

import com.codingapi.flow.context.RepositoryHolderContext;
import com.codingapi.flow.service.impl.FlowDelayTriggerService;
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
    public void start() {
        RepositoryHolderContext.getInstance().verify();
        List<DelayTask> delayTasks = RepositoryHolderContext.getInstance().findDelayTasks();
        if (delayTasks != null && !delayTasks.isEmpty()) {
            for (DelayTask delayTask : delayTasks) {
                this.addTask(delayTask);
            }
        }
    }


    /**
     * 关闭全部任务
     */
    public void close() {
        for (DelayJob delayJob : delayJobs) {
            delayJob.close();
        }
        this.delayJobs.clear();
    }

    /**
     * 添加任务队列
     */
    public void addTask(DelayTask task) {
        RepositoryHolderContext.getInstance().saveDelayTask(task);
        this.delayJobs.add(new DelayJob(task));
    }

    /**
     * 延期任务队列
     */
    private static class DelayJob {
        private final DelayTask task;
        private final Timer timer;

        public DelayJob(DelayTask task) {
            this.task = task;
            this.timer = new Timer();
            this.start();
        }

        public void close() {
            this.timer.cancel();
        }

        private void start() {
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {

                    FlowDelayTriggerService flowDelayTriggerService = RepositoryHolderContext.getInstance().createDelayTriggerService(task);

                    flowDelayTriggerService.trigger();

                    RepositoryHolderContext.getInstance().deleteDelayTask(task);
                }
            }, new Date(task.getTriggerTime()));
        }
    }
}
