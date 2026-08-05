package com.codingapi.flow.domain;

import com.codingapi.flow.record.FlowRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 子流程节点的一次执行记录。
 *
 * <p>一个主流程节点执行对应一条记录，内部实例列表记录本次创建的全部子流程。</p>
 */
@Getter
@AllArgsConstructor
public class SubProcessRecord {

    @Setter
    private long id;
    private final String groupId;
    private final String parentProcessId;
    private final long parentRecordId;
    private final long parentWorkRuntimeId;
    private final String nodeId;
    private final int totalCount;
    private final List<Instance> instances;
    private State state;
    private final long createTime;
    private long finishTime;

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

    public boolean containsChildProcess(String processId) {
        return instances.stream().anyMatch(instance -> instance.getProcessId().equals(processId));
    }

    public boolean complete(FlowRecord finalRecord) {
        for (Instance instance : instances) {
            if (instance.getProcessId().equals(finalRecord.getProcessId()) && instance.isRunning()) {
                instance.complete(finalRecord);
                return true;
            }
        }
        return false;
    }

    public boolean isAllFinished() {
        return instances.stream().allMatch(Instance::isFinished);
    }

    public boolean isWaiting() {
        return state == State.WAITING;
    }

    public boolean isPassed() {
        return state == State.PASSED;
    }

    public boolean isError() {
        return state == State.ERROR;
    }

    public void pass() {
        this.state = State.PASSED;
        this.finishTime = System.currentTimeMillis();
    }

    public void error() {
        this.state = State.ERROR;
        this.finishTime = System.currentTimeMillis();
    }

    public List<Long> findFinishedRecordIds() {
        return instances.stream()
                .filter(Instance::isFinished)
                .map(Instance::getFinishRecordId)
                .toList();
    }

    public enum State {
        WAITING,
        PASSED,
        ERROR
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Instance {

        private long startRecordId;
        private String processId;
        private long finishRecordId;
        private InstanceState state;
        private long finishTime;

        public Instance(FlowRecord startRecord) {
            this(startRecord.getId(), startRecord.getProcessId(), 0, InstanceState.RUNNING, 0);
        }

        private void complete(FlowRecord finalRecord) {
            this.finishRecordId = finalRecord.getId();
            this.state = finalRecord.getFlowState() == FlowRecord.SATE_FLOW_FINISH
                    ? InstanceState.FINISHED
                    : InstanceState.TERMINATED;
            this.finishTime = System.currentTimeMillis();
        }

        public boolean isRunning() {
            return state == InstanceState.RUNNING;
        }

        public boolean isFinished() {
            return state == InstanceState.FINISHED || state == InstanceState.TERMINATED;
        }
    }

    public enum InstanceState {
        RUNNING,
        FINISHED,
        TERMINATED
    }
}
