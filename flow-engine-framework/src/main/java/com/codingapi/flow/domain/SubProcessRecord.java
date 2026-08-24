package com.codingapi.flow.domain;

import com.codingapi.flow.record.FlowRecord;
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
     * 是否已被重置取代。
     * <p>子流程数据重置时旧聚合组被标记为已取代（状态保持不变以保留审计语义），
     * 新聚合组取代其成为当前有效数据；已取代的组不再参与结果判定、脚本查询与重置。</p>
     */
    private boolean superseded;

    /**
     * 全字段构造（持久化转换使用）。
     *
     * @param id                  记录id
     * @param groupId             本次子流程执行的分组id
     * @param parentProcessId     父流程（主流程）的流程id
     * @param parentRecordId      父流程中子流程节点的执行记录id
     * @param parentWorkRuntimeId 父流程（主流程）的运行实例id
     * @param nodeId              父流程中子流程节点的节点id
     * @param totalCount          本次创建的子流程实例总数
     * @param instances           本次创建的全部子流程实例
     * @param state               本次子流程执行的聚合状态
     * @param createTime          创建时间（毫秒时间戳）
     * @param finishTime          结束时间（毫秒时间戳），未结束时为 0
     */
    public SubProcessRecord(long id,
                            String groupId,
                            String parentProcessId,
                            long parentRecordId,
                            long parentWorkRuntimeId,
                            String nodeId,
                            int totalCount,
                            List<Instance> instances,
                            State state,
                            long createTime,
                            long finishTime) {
        this(id, groupId, parentProcessId, parentRecordId, parentWorkRuntimeId, nodeId,
                totalCount, instances, state, createTime, finishTime, false);
    }

    /**
     * 全字段构造（含已取代标记）。
     *
     * @param superseded 是否已被重置取代
     */
    public SubProcessRecord(long id,
                            String groupId,
                            String parentProcessId,
                            long parentRecordId,
                            long parentWorkRuntimeId,
                            String nodeId,
                            int totalCount,
                            List<Instance> instances,
                            State state,
                            long createTime,
                            long finishTime,
                            boolean superseded) {
        this.id = id;
        this.groupId = groupId;
        this.parentProcessId = parentProcessId;
        this.parentRecordId = parentRecordId;
        this.parentWorkRuntimeId = parentWorkRuntimeId;
        this.nodeId = nodeId;
        this.totalCount = totalCount;
        this.instances = instances;
        this.state = state;
        this.createTime = createTime;
        this.finishTime = finishTime;
        this.superseded = superseded;
    }

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
     * 标记本次执行已被重置取代。
     * <p>聚合状态保持不变（保留审计语义），仅置位已取代标记。</p>
     */
    public void supersede() {
        this.superseded = true;
    }

    /**
     * 是否已被重置取代。
     */
    public boolean isSuperseded() {
        return superseded;
    }

    /**
     * 生成本记录的不可变快照（供事件携带，避免异步消费时被后续状态变更污染）。
     *
     * @return 字段相同的副本，实例列表不可变
     */
    public SubProcessRecord snapshot() {
        return new SubProcessRecord(id, groupId, parentProcessId, parentRecordId, parentWorkRuntimeId,
                nodeId, totalCount, List.copyOf(instances), state, createTime, finishTime, superseded);
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
         * 是否为继承实例：子流程数据重置时未被选中重置的实例直接沿用原结果，
         * 不再重新执行，仅在新聚合组中继承其最终状态。
         */
        private boolean inherited;

        /**
         * 重建实例替换的旧实例流程id：重置时重建的实例记录其取代的旧实例，
         * 供订阅方完成旧 → 新流程id映射；继承实例与常规实例为 null。
         */
        private String sourceProcessId;

        /**
         * 全字段构造（持久化转换与测试使用）。
         *
         * @param startRecordId  子流程开始节点（发起）的执行记录id
         * @param processId      子流程的流程id
         * @param workTitle      子流程的流程名称
         * @param finishRecordId 子流程最终执行记录id，未结束时为 0
         * @param state          子流程实例运行状态
         * @param finishTime     结束时间（毫秒时间戳），未结束时为 0
         */
        public Instance(long startRecordId,
                        String processId,
                        String workTitle,
                        long finishRecordId,
                        InstanceState state,
                        long finishTime) {
            this(startRecordId, processId, workTitle, finishRecordId, state, finishTime, false, null);
        }

        /**
         * 全字段构造（含重置标记）。
         *
         * @param inherited       是否为继承实例
         * @param sourceProcessId 重建实例替换的旧实例流程id（继承实例与常规实例为 null）
         */
        public Instance(long startRecordId,
                        String processId,
                        String workTitle,
                        long finishRecordId,
                        InstanceState state,
                        long finishTime,
                        boolean inherited,
                        String sourceProcessId) {
            this.startRecordId = startRecordId;
            this.processId = processId;
            this.workTitle = workTitle;
            this.finishRecordId = finishRecordId;
            this.state = state;
            this.finishTime = finishTime;
            this.inherited = inherited;
            this.sourceProcessId = sourceProcessId;
        }

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
         * 创建重置重建的子流程实例，初始状态为运行中。
         *
         * @param startRecord     重建子流程开始节点（发起）的执行记录
         * @param sourceProcessId 被替换的旧实例流程id
         */
        public static Instance rebuiltFrom(FlowRecord startRecord, String sourceProcessId) {
            return new Instance(startRecord.getId(), startRecord.getProcessId(), startRecord.getWorkTitle(),
                    0, InstanceState.RUNNING, 0, false, sourceProcessId);
        }

        /**
         * 创建重置继承的子流程实例：完整沿用原实例的最终状态，不再重新执行。
         *
         * @param source 被取代聚合组中的原实例
         */
        public static Instance inheritFrom(Instance source) {
            return new Instance(source.getStartRecordId(), source.getProcessId(), source.getWorkTitle(),
                    source.getFinishRecordId(), source.getState(), source.getFinishTime(), true, null);
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