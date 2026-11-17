package com.codingapi.flow.workflow.runtime;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程结构差异（issue #281）。
 *
 * <p>记录旧运行时快照与最新流程设计之间的结构差异：节点缺失、节点新增、
 * 节点类型/层级/顺序变化，以及旧快照已引用的按钮动作缺失。任意一项非空即视为结构不一致，
 * 不允许对运行中实例执行强制同步。</p>
 */
@Getter
public class WorkflowStructureDiff {

    /**
     * 旧快照中存在、最新设计中已被删除的节点id
     */
    private final List<String> missingNodeIds = new ArrayList<>();

    /**
     * 最新设计中新增、旧快照中不存在的节点id
     */
    private final List<String> extraNodeIds = new ArrayList<>();

    /**
     * 节点类型、父子层级或同层顺序发生变化的节点id
     */
    private final List<String> changedNodeIds = new ArrayList<>();

    /**
     * 旧快照中已存在、最新设计中缺失（或类型变化）的按钮动作id
     */
    private final List<String> missingActionIds = new ArrayList<>();

    /**
     * 结构是否与最新设计完全一致
     *
     * @return true 表示允许强制同步
     */
    public boolean isConsistent() {
        return missingNodeIds.isEmpty()
                && extraNodeIds.isEmpty()
                && changedNodeIds.isEmpty()
                && missingActionIds.isEmpty();
    }

    void addMissingNode(String nodeId) {
        this.missingNodeIds.add(nodeId);
    }

    void addExtraNode(String nodeId) {
        this.extraNodeIds.add(nodeId);
    }

    void addChangedNode(String nodeId) {
        this.changedNodeIds.add(nodeId);
    }

    void addMissingAction(String actionId) {
        this.missingActionIds.add(actionId);
    }

    /**
     * 输出差异明细，用于异常提示与前端展示
     *
     * @return 差异描述
     */
    public String describe() {
        List<String> messages = new ArrayList<>();
        if (!missingNodeIds.isEmpty()) {
            messages.add("removed nodes " + missingNodeIds);
        }
        if (!extraNodeIds.isEmpty()) {
            messages.add("added nodes " + extraNodeIds);
        }
        if (!changedNodeIds.isEmpty()) {
            messages.add("nodes with changed type/hierarchy/order " + changedNodeIds);
        }
        if (!missingActionIds.isEmpty()) {
            messages.add("removed actions " + missingActionIds);
        }
        return String.join(", ", messages);
    }
}
