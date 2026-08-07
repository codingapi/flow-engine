package com.codingapi.flow.domain;

import com.codingapi.flow.record.FlowRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 子流程节点的一次执行记录（聚合根）。
 *
 * <p>一个子流程节点（SUB_PROCESS）每次执行时创建一条记录，内部实例列表 {@link #instances}
 * 记录本次创建的全部子流程实例。当全部实例结束且结果脚本确认通过时置为 {@link State#PASSED}
 * 并恢复主流程；全部实例结束但结果未通过时置为 {@link State#ERROR} 并跳转异常节点。</p>
 */
@Getter
@AllArgsConstructor
public class SubProcessRecord {

    /**
     * 记录id（持久化后由数据库生成）
     */
    @Setter
    private long id;

    /**
     * 本次子流程执行的分组id，同一组记录在该字段上唯一
     */
    private final String groupId;

    /**
     * 父流程（主流程）的流程id
     */
    private final String parentProcessId;

    /**
     * 父流程中子流程节点的执行记录id
     */
    private final long parentRecordId;

    /**
     * 父流程（主流程）的运行实例id，用于恢复父流程运行时上下文
     */
    private final long parentWorkRuntimeId;

    /**
     * 父流程中子流程节点的节点id
     */
    private final String nodeId;

    /**
     * 本次创建的子流程实例总数
     */
    private final int totalCount;

    /**
     * 本次创建的全部子流程实例
     */
    private final List<Instance> instances;

    /**
     * 本次子流程执行的聚合状态
     */
    private State state;

    /**
     * 创建时间（毫秒时间戳）
     */
    private final long createTime;

    /**
     * 结束时间（毫秒时间戳），未结束时为 0
     */
    private long finishTime;

    /**
     * 创建子流程执行记录，初始状态为等待子流程结果。
     *
     * @param groupId      本次子流程执行的分组id
     * @param parentRecord 父流程中子流程节点的执行记录
     * @param nodeId       父流程中子流程节点的节点id
     * @param instances    本次创建的子流程实例
     */
    public SubProcessRecord(String groupId,
                            FlowRecord parentRecord,
                            String nodeId,
                            List<Instance> instances) {
        this(0,
                groupId,
                parentRecord.getProcessId(),
                parentRecord.getId(),
                parentRecord.getWorkRuntimeId(),
                nodeId,
                instances.size(),
                new ArrayList<>(instances),
                State.WAITING,
                System.currentTimeMillis(),
                0);
    }

    /**
     * 判断本次执行是否包含指定子流程。
     *
     * @param processId 子流程的流程id
     * @return 包含返回 true；否则返回 false
     */
    public boolean containsChildProcess(String processId) {
        return instances.stream().anyMatch(instance -> instance.getProcessId().equals(processId));
    }

    /**
     * 完成一个子流程实例。
     * <p>将指定子流程实例标记为最终状态（正常结束或终止），并记录结束时间。
     *
     * @param finalRecord 子流程的最终执行记录
     * @return 实例存在且处于运行中并完成时返回 true；否则返回 false
     */
    public boolean complete(FlowRecord finalRecord) {
        for (Instance instance : instances) {
            if (instance.getProcessId().equals(finalRecord.getProcessId()) && instance.isRunning()) {
                instance.complete(finalRecord);
                return true;
            }
        }
        return false;
    }

    /**
     * 是否全部子流程实例已结束（正常结束或终止）。
     */
    public boolean isAllFinished() {
        return instances.stream().allMatch(Instance::isFinished);
    }

    /**
     * 是否仍处于等待子流程结果状态。
     */
    public boolean isWaiting() {
        return state == State.WAITING;
    }

    /**
     * 是否已通过（结果脚本确认通过，主流程已恢复）。
     */
    public boolean isPassed() {
        return state == State.PASSED;
    }

    /**
     * 是否已异常（结果未通过，已跳转异常节点）。
     */
    public boolean isError() {
        return state == State.ERROR;
    }

    /**
     * 标记本次执行通过：结果脚本确认后恢复主流程。
     */
    public void pass() {
        this.state = State.PASSED;
        this.finishTime = System.currentTimeMillis();
    }

    /**
     * 标记本次执行异常：全部实例结束但结果未通过时跳转异常节点。
     */
    public void error() {
        this.state = State.ERROR;
        this.finishTime = System.currentTimeMillis();
    }

    /**
     * 查找全部已结束实例的最终记录id。
     *
     * @return 已结束实例的最终记录id列表
     */
    public List<Long> findFinishedRecordIds() {
        return instances.stream()
                .filter(Instance::isFinished)
                .map(Instance::getFinishRecordId)
                .toList();
    }

    /**
     * 子流程节点执行的聚合状态。
     */
    public enum State {
        /**
         * 等待子流程结果：实例创建后、结果确认前
         */
        WAITING,
        /**
         * 已通过：结果脚本确认通过，主流程已恢复
         */
        PASSED,
        /**
         * 已异常：全部实例结束但结果未通过，跳转异常节点
         */
        ERROR
    }

    /**
     * 子流程实例。
     */
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Instance {

        /**
         * 子流程开始节点（发起）的执行记录id
         */
        private long startRecordId;

        /**
         * 子流程的流程id
         */
        private String processId;

        /**
         * 子流程的流程名称；历史数据可能缺省，展示时回退加载
         */
        private String workTitle;

        /**
         * 子流程最终执行记录id，未结束时为 0
         */
        private long finishRecordId;

        /**
         * 子流程实例运行状态
         */
        private InstanceState state;

        /**
         * 结束时间（毫秒时间戳），未结束时为 0
         */
        private long finishTime;

        /**
         * 创建子流程实例，初始状态为运行中。
         *
         * @param startRecord 子流程开始节点（发起）的执行记录
         */
        public Instance(FlowRecord startRecord) {
            this(startRecord.getId(), startRecord.getProcessId(), startRecord.getWorkTitle(),
                    0, InstanceState.RUNNING, 0);
        }

        /**
         * 完成子流程实例：依据最终记录的主流程状态标记为已完成或已终止。
         *
         * @param finalRecord 子流程的最终执行记录
         */
        private void complete(FlowRecord finalRecord) {
            this.finishRecordId = finalRecord.getId();
            this.state = finalRecord.getFlowState() == FlowRecord.SATE_FLOW_FINISH
                    ? InstanceState.FINISHED
                    : InstanceState.TERMINATED;
            this.finishTime = System.currentTimeMillis();
        }

        /**
         * 是否运行中。
         */
        public boolean isRunning() {
            return state == InstanceState.RUNNING;
        }

        /**
         * 是否已结束（正常结束或终止）。
         */
        public boolean isFinished() {
            return state == InstanceState.FINISHED || state == InstanceState.TERMINATED;
        }
    }

    /**
     * 子流程实例状态。
     */
    public enum InstanceState {
        /**
         * 运行中
         */
        RUNNING,
        /**
         * 已完成：子流程以正常结束状态收尾
         */
        FINISHED,
        /**
         * 已终止：子流程非正常结束（如发起人撤销）
         */
        TERMINATED
    }
}