package com.codingapi.flow.api.pojo;

import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.actions.SaveAction;
import com.codingapi.flow.workflow.WorkflowBuilder;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.workflow.Workflow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 流程元数据动作过滤测试（issue #183）。
 *
 * <p>子流程配置通过 /api/cmd/workflow/meta 获取目标流程开始节点的动作列表，
 * 获取到的流程节点操作应该都是非 disable 的动作按钮，禁用的动作不应出现在候选列表中。
 */
class WorkflowMetaTest {

    private Workflow buildWorkflow(StartNode startNode) {
        return WorkflowBuilder.builder()
                .id("wf-1")
                .code("test-flow")
                .title("测试流程")
                .addNode(startNode)
                .addNode(EndNode.builder().build())
                .build(false);
    }

    /**
     * 开始节点存在禁用动作时，meta 只返回启用的动作。
     */
    @Test
    void metaActions_shouldExcludeDisabledActions() {
        // given - 开始节点默认动作：通过(PASS)启用、保存(SAVE)禁用
        StartNode startNode = StartNode.builder().build();
        SaveAction saveAction = (SaveAction) startNode.actionManager().getActionByType(ActionType.SAVE.name());
        saveAction.setEnable(false);

        // when - 构建流程元数据
        WorkflowMeta meta = new WorkflowMeta(buildWorkflow(startNode));

        // then - 仅返回启用的动作
        List<WorkflowMeta.ActionOption> actions = meta.getActions();
        assertEquals(1, actions.size(), "禁用的动作不应返回给子流程配置");
        assertEquals(ActionType.PASS.name(), actions.get(0).getType());
    }

    /**
     * 开始节点动作全部启用时，meta 全部返回。
     */
    @Test
    void metaActions_shouldReturnAllEnabledActions() {
        // given - 开始节点默认动作均为启用（通过 + 保存）
        StartNode startNode = StartNode.builder().build();

        // when
        WorkflowMeta meta = new WorkflowMeta(buildWorkflow(startNode));

        // then
        List<WorkflowMeta.ActionOption> actions = meta.getActions();
        assertEquals(2, actions.size());
        assertTrue(actions.stream().anyMatch(action -> ActionType.PASS.name().equals(action.getType())));
        assertTrue(actions.stream().anyMatch(action -> ActionType.SAVE.name().equals(action.getType())));
    }
}
