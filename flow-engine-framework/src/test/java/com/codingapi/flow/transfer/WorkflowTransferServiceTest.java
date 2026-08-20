package com.codingapi.flow.transfer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.ConditionBranchNode;
import com.codingapi.flow.node.nodes.ConditionElseBranchNode;
import com.codingapi.flow.node.nodes.ConditionNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.HandleNode;
import com.codingapi.flow.node.nodes.ParallelBranchNode;
import com.codingapi.flow.node.nodes.ParallelNode;
import com.codingapi.flow.node.nodes.RouterNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.node.nodes.SubProcessNode;
import com.codingapi.flow.node.nodes.TriggerNode;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import com.codingapi.flow.workflow.WorkflowVersion;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;
import com.codingapi.springboot.script.GroovyScript;
import com.codingapi.springboot.script.cache.GroovyScriptCacheContext;
import com.codingapi.springboot.script.scanner.GroovyScriptAnnotationScannerUtils;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 流程迁移复杂场景测试。
 *
 * <p>测试数据包含三个历史版本、条件嵌套、并行分支、审批、办理、路由、触发器、
 * 子流程以及自定义动作，覆盖全部12类Groovy脚本引用，不使用简单的单版本线性流程。</p>
 */
class WorkflowTransferServiceTest {

    private static final String WORK_CODE = "complex-transfer-workflow";

    /**
     * 测试目标：验证复杂多版本流程导出后通过增量模式完整复制。
     * 前置条件：源流程包含三个结构逐步扩展的版本以及全部脚本类型。
     * 执行步骤：导出V1迁移包，再以增量模式导入同一仓储。
     * 期望断言：流程身份重置，配置ID保持，版本与脚本正文完整，脚本执行元数据按类型正确重建。
     */
    @Test
    void shouldExportAndIncrementallyImportComplexMultiVersionWorkflow() {
        MyFlowServiceFactory factory = new MyFlowServiceFactory();
        User operator = saveOperator(factory, 1001, "迁移管理员");
        Workflow source = createComplexThreeVersionWorkflow(factory, operator, WORK_CODE);

        String exported = factory.workflowService.exportWorkflow(source.getId());
        JSONObject sourcePackage = JSON.parseObject(exported);
        JSONObject sourceScripts = sourcePackage.getJSONObject("groovyScripts");
        JSONArray sourceVersions = sourcePackage.getJSONArray("versions");

        assertAll("V1复杂迁移包结构",
                () -> assertEquals("flow-engine-workflow", sourcePackage.getString("format")),
                () -> assertEquals(1, sourcePackage.getIntValue("schemaVersion")),
                () -> assertEquals(3, sourceVersions.size()),
                () -> assertEquals("v3.0", sourcePackage.getJSONObject("workflow").getString("currentVersion")),
                () -> assertTrue(sourcePackage.getJSONObject("workflow").getBooleanValue("enable")),
                () -> assertTrue(sourceScripts.size() >= 40,
                        "三个复杂版本应导出大量独立脚本，而不是少量简单脚本"),
                () -> assertTrue(collectConfigurationIds(sourceVersions).size() >= 35,
                        "复杂流程应包含足够多的节点、动作和表单字段ID"));

        String importedId = factory.workflowService.importWorkflow(
                toDataUrl(exported), operator, WorkflowImportMode.INCREMENTAL);
        Workflow imported = factory.workflowService.getWorkflowById(importedId);
        String reExported = factory.workflowService.exportWorkflow(importedId);
        JSONObject importedPackage = JSON.parseObject(reExported);

        Set<String> sourceKeys = sourceScripts.keySet();
        Set<String> importedKeys = importedPackage.getJSONObject("groovyScripts").keySet();
        Set<String> importedScriptTypes = importedKeys.stream()
                .map(GroovyScriptCacheContext.getInstance()::getGroovyScript)
                .filter(script -> script != null && script.getTypeTwo() != null)
                .map(GroovyScript::getTypeTwo)
                .collect(Collectors.toSet());

        assertAll("增量导入结果",
                () -> assertNotEquals(source.getId(), imported.getId(), "增量导入必须生成新workId"),
                () -> assertNotEquals(source.getCode(), imported.getCode(), "增量导入必须生成新workCode"),
                () -> assertEquals(3, factory.workflowVersionRepository.findVersion(importedId).size()),
                () -> assertEquals(versionNames(sourcePackage), versionNames(importedPackage)),
                () -> assertEquals(versionStates(sourcePackage), versionStates(importedPackage),
                        "当前版本标记和启用状态必须保持不变"),
                () -> assertTrue(imported.isEnable(), "流程启用状态必须保持不变"),
                () -> assertEquals(collectConfigurationIds(sourceVersions),
                        collectConfigurationIds(importedPackage.getJSONArray("versions")),
                        "节点、分支、动作和表单字段ID必须保持不变"),
                () -> assertTrue(disjoint(sourceKeys, importedKeys), "导入脚本必须全部生成新key"),
                () -> assertEquals(scriptContentFrequency(sourceScripts),
                        scriptContentFrequency(importedPackage.getJSONObject("groovyScripts")),
                        "新旧脚本key不同，但全部正文及重复次数必须一致"),
                () -> assertTrue(importedScriptTypes.containsAll(Set.of(
                                "router-script", "node-title", "condition", "trigger",
                                "sub-process", "sub-process-result", "operator-load",
                                "operator-match", "error-trigger", "action-display",
                                "action-custom", "action-reject")),
                        "正文Schema虽不携带脚本元数据，导入器仍须重建全部脚本类型"));

        for (String key : importedKeys) {
            GroovyScript script = GroovyScriptCacheContext.getInstance().getGroovyScript(key);
            assertNotNull(script, "每个新key都必须有持久化脚本对象");
            assertNotNull(script.getMethod(), "脚本执行方法元数据必须被重建");
            assertNotNull(script.getReturnType(), "脚本返回类型元数据必须被重建");
        }
    }

    /**
     * 测试目标：验证替换导入只保留目标流程身份，其余版本配置来自导入包。
     * 前置条件：目标环境存在同workCode但结构完全不同的单版本流程。
     * 执行步骤：将复杂三版本迁移包以REPLACE方式导入目标环境。
     * 期望断言：目标workId/workCode不变，旧脚本清理，复杂版本和配置ID完整落入。
     */
    @Test
    void shouldReplaceExistingWorkflowAndRemoveOldVersionScripts() {
        MyFlowServiceFactory sourceFactory = new MyFlowServiceFactory();
        User sourceOperator = saveOperator(sourceFactory, 2001, "源环境管理员");
        Workflow source = createComplexThreeVersionWorkflow(sourceFactory, sourceOperator, WORK_CODE);
        String exported = sourceFactory.workflowService.exportWorkflow(source.getId());
        JSONObject sourcePackage = JSON.parseObject(exported);

        MyFlowServiceFactory targetFactory = new MyFlowServiceFactory();
        User targetOperator = saveOperator(targetFactory, 3001, "目标环境管理员");
        Workflow target = createReplacementTarget(targetFactory, targetOperator, WORK_CODE);
        String targetId = target.getId();
        Set<String> oldTargetScriptKeys = workflowScriptKeys(
                targetFactory.workflowVersionRepository.findVersion(targetId));

        String importedId = targetFactory.workflowService.importWorkflow(
                toDataUrl(exported), targetOperator, WorkflowImportMode.REPLACE);
        Workflow replaced = targetFactory.workflowService.getWorkflowById(importedId);
        JSONObject replacedPackage = JSON.parseObject(
                targetFactory.workflowService.exportWorkflow(importedId));

        assertAll("替换导入结果",
                () -> assertEquals(targetId, importedId, "替换导入必须保留目标workId"),
                () -> assertEquals(WORK_CODE, replaced.getCode(), "替换导入必须保留workCode"),
                () -> assertEquals("复杂迁移流程", replaced.getTitle()),
                () -> assertEquals(3, targetFactory.workflowVersionRepository.findVersion(targetId).size()),
                () -> assertEquals(versionNames(sourcePackage), versionNames(replacedPackage)),
                () -> assertEquals(collectConfigurationIds(sourcePackage.getJSONArray("versions")),
                        collectConfigurationIds(replacedPackage.getJSONArray("versions"))),
                () -> assertTrue(oldTargetScriptKeys.stream()
                        .allMatch(key -> GroovyScriptCacheContext.getInstance().getGroovyScript(key) == null),
                        "被替换版本的旧脚本必须从仓储和缓存清理"));
    }

    /**
     * 测试目标：验证管理入口导入在源编码未占用时保留编码。
     * 前置条件：源环境有复杂三版本流程，目标环境不存在同编码流程。
     * 执行步骤：在目标环境执行增量导入。
     * 期望断言：生成新的流程ID，但保留文件中的流程编码和全部三个版本。
     */
    @Test
    void shouldKeepSourceCodeWhenIncrementalImportHasNoConflict() {
        MyFlowServiceFactory sourceFactory = new MyFlowServiceFactory();
        User sourceOperator = saveOperator(sourceFactory, 3501, "无冲突源环境管理员");
        Workflow source = createComplexThreeVersionWorkflow(sourceFactory, sourceOperator, WORK_CODE);
        String exported = sourceFactory.workflowService.exportWorkflow(source.getId());

        MyFlowServiceFactory targetFactory = new MyFlowServiceFactory();
        User targetOperator = saveOperator(targetFactory, 3502, "无冲突目标环境管理员");

        String importedId = targetFactory.workflowService.importWorkflow(
                toDataUrl(exported), targetOperator, WorkflowImportMode.INCREMENTAL);
        Workflow imported = targetFactory.workflowRepository.getById(importedId);

        assertAll("无编码冲突的增量导入",
                () -> assertNotEquals(source.getId(), importedId),
                () -> assertEquals(WORK_CODE, imported.getCode()),
                () -> assertEquals(3, targetFactory.workflowVersionRepository.findVersion(importedId).size()));
    }

    /**
     * 测试目标：验证Schema策略严格匹配版本。
     * 前置条件：导入文件声明尚未支持的schemaVersion。
     * 执行步骤：以增量模式导入。
     * 期望断言：导入在创建任何流程版本前失败。
     */
    @Test
    void shouldRejectUnsupportedSchemaVersionBeforePersistence() {
        MyFlowServiceFactory factory = new MyFlowServiceFactory();
        User operator = saveOperator(factory, 4001, "版本校验管理员");
        String unsupported = """
                {
                  "format": "flow-engine-workflow",
                  "schemaVersion": 99,
                  "workflow": {},
                  "versions": [],
                  "groovyScripts": {}
                }
                """;

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> factory.workflowService.importWorkflow(
                        toDataUrl(unsupported), operator, WorkflowImportMode.INCREMENTAL));

        assertTrue(exception.getMessage().contains("schemaVersion=99"));
        assertNull(factory.workflowRepository.getByCode(WORK_CODE));
    }

    /**
     * 测试目标：验证删除复杂多版本流程会物理清理全部设计态脚本，但保留独立运行时快照。
     * 前置条件：流程包含三个版本、40个以上脚本，并已生成持久化运行时快照。
     * 执行步骤：删除整个流程。
     * 期望断言：流程和版本消失，所有版本脚本从仓储及缓存删除，运行时及其脚本正文保持不变。
     */
    @Test
    void shouldDeleteAllVersionScriptsAndKeepRuntimeSnapshot() {
        MyFlowServiceFactory factory = new MyFlowServiceFactory();
        User operator = saveOperator(factory, 5001, "删除验证管理员");
        Workflow workflow = createComplexThreeVersionWorkflow(factory, operator, WORK_CODE);
        List<WorkflowVersion> versions = factory.workflowVersionRepository.findVersion(workflow.getId());

        Set<String> designScriptKeys = workflowScriptKeys(versions);
        designScriptKeys.addAll(
                GroovyScriptAnnotationScannerUtils.findGroovyScriptFields(workflow).getKeys());
        WorkflowRuntime runtime = new WorkflowRuntime(workflow);
        factory.workflowService.saveWorkflowRuntime(runtime);
        Map<String, String> runtimeScripts = new HashMap<>(runtime.getScripts());

        assertTrue(designScriptKeys.size() >= 40, "删除测试必须覆盖复杂多版本脚本集合");
        assertTrue(runtimeScripts.size() >= 10, "当前运行时必须固化完整流程脚本快照");

        factory.workflowService.delete(workflow.getId());

        WorkflowRuntime retainedRuntime = factory.workflowRuntimeRepository.get(runtime.getId());
        assertAll("流程删除后的设计态与运行态数据",
                () -> assertNull(factory.workflowRepository.getById(workflow.getId())),
                () -> assertTrue(factory.workflowVersionRepository.findVersion(workflow.getId()).isEmpty()),
                () -> assertTrue(designScriptKeys.stream()
                                .allMatch(key -> GroovyScriptCacheContext.getInstance().getGroovyScript(key) == null),
                        "所有历史版本和主流程的Groovy脚本都必须物理删除并清理缓存"),
                () -> assertNotNull(retainedRuntime, "删除流程不能联动删除运行时"),
                () -> assertEquals(runtimeScripts, retainedRuntime.getScripts(),
                        "运行时独立脚本快照不能受设计态脚本删除影响"),
                () -> assertEquals(workflow.getTitle(), retainedRuntime.toWorkflow().getTitle()));
    }

    private Workflow createComplexThreeVersionWorkflow(MyFlowServiceFactory factory,
                                                       User operator,
                                                       String workCode) {
        FlowForm form = FlowFormBuilder.builder()
                .name("复杂迁移表单")
                .code("complex-transfer-form")
                .addField("申请人", "applicant", DataType.STRING)
                .addField("金额", "amount", DataType.DOUBLE)
                .addField("紧急", "urgent", DataType.BOOLEAN)
                .addField("说明", "description", DataType.STRING)
                .build();

        StartNode start = StartNode.builder().id("node-start-fixed").name("开始").build();
        ApprovalNode firstApproval = approvalNodeWithCustomAction(
                "node-approval-first", "部门审批", "action-custom-fixed");

        ConditionBranchNode highAmount = ConditionBranchNode.builder()
                .id("branch-high-amount")
                .name("高金额分支")
                .conditionScript(FlowGroovyScriptFactory.createConditionScript(
                        "def run(request) { return request.getFormData('amount') != null }").getKey())
                .blocks(
                        ApprovalNode.builder().id("node-finance-approval").name("财务审批").build(),
                        RouterNode.builder().id("node-router").name("路由判断").build(),
                        TriggerNode.builder().id("node-trigger").name("触发归档").build())
                .build();
        ConditionBranchNode urgent = ConditionBranchNode.builder()
                .id("branch-urgent")
                .name("紧急分支")
                .conditionScript(FlowGroovyScriptFactory.createConditionScript(
                        "def run(request) { return request.getFormData('urgent') == true }").getKey())
                .blocks(
                        HandleNode.builder().id("node-urgent-handle").name("紧急办理").build(),
                        SubProcessNode.builder().id("node-sub-process-v1").name("通知子流程").build())
                .build();
        ConditionElseBranchNode other = ConditionElseBranchNode.builder()
                .id("branch-other")
                .name("普通分支")
                .blocks(HandleNode.builder().id("node-normal-handle").name("普通办理").build())
                .build();
        ConditionNode condition = ConditionNode.builder()
                .id("node-condition")
                .name("金额与紧急程度判断")
                .blocks(highAmount, urgent, other)
                .build();
        EndNode end = EndNode.builder().id("node-end-fixed").name("结束").build();

        Workflow workflow = WorkflowBuilder.builder()
                .code(workCode)
                .title("复杂迁移流程")
                .description("覆盖多版本、嵌套节点和全部脚本类型")
                .createdOperator(operator)
                .form(form)
                .addNode(start)
                .addNode(firstApproval)
                .addNode(condition)
                .addNode(end)
                .maxNestDepth(12)
                .build();

        saveVersion(factory, workflow, "v1.0", false);

        Workflow versionTwo = cloneWorkflow(factory.workflowRepository.getById(workflow.getId()));
        ParallelBranchNode firstParallel = ParallelBranchNode.builder()
                .id("parallel-branch-a")
                .name("并行审批A")
                .blocks(ApprovalNode.builder().id("node-parallel-approval-a").name("并行审批A").build())
                .build();
        ParallelBranchNode secondParallel = ParallelBranchNode.builder()
                .id("parallel-branch-b")
                .name("并行审批B")
                .blocks(HandleNode.builder().id("node-parallel-handle-b").name("并行办理B").build())
                .build();
        ParallelNode parallel = ParallelNode.builder()
                .id("node-parallel")
                .name("并行处理")
                .blocks(firstParallel, secondParallel)
                .build();
        versionTwo.getNodes().add(versionTwo.getNodes().size() - 1, parallel);
        versionTwo.enable();
        saveVersion(factory, versionTwo, "v2.0", true);

        Workflow versionThree = cloneWorkflow(factory.workflowRepository.getById(workflow.getId()));
        versionThree.getNodes().add(versionThree.getNodes().size() - 1,
                SubProcessNode.builder().id("node-sub-process-v3").name("最终子流程").build());
        versionThree.getNodes().add(versionThree.getNodes().size() - 1,
                TriggerNode.builder().id("node-final-trigger").name("最终触发器").build());
        versionThree.enable();
        saveVersion(factory, versionThree, "v3.0", true);

        return factory.workflowRepository.getById(workflow.getId());
    }

    private ApprovalNode approvalNodeWithCustomAction(String nodeId, String name, String actionId) {
        ApprovalNode approvalNode = ApprovalNode.builder().id(nodeId).name(name).build();
        List<IFlowAction> actions = new ArrayList<>(approvalNode.getActions());
        CustomAction customAction = CustomAction.defaultAction();
        customAction.setId(actionId);
        actions.add(customAction);
        approvalNode.setActions(actions);
        return approvalNode;
    }

    private Workflow createReplacementTarget(MyFlowServiceFactory factory, User operator, String workCode) {
        FlowForm form = FlowFormBuilder.builder()
                .name("待替换表单")
                .code("replacement-target-form")
                .addField("旧字段", "legacy", DataType.STRING)
                .build();
        Workflow target = WorkflowBuilder.builder()
                .code(workCode)
                .title("待替换流程")
                .createdOperator(operator)
                .form(form)
                .addNode(StartNode.builder().id("old-start").name("旧开始").build())
                .addNode(ApprovalNode.builder().id("old-approval").name("旧审批").build())
                .addNode(EndNode.builder().id("old-end").name("旧结束").build())
                .build(false);
        saveVersion(factory, target, "legacy-v1", false);
        return factory.workflowRepository.getById(target.getId());
    }

    private void saveVersion(MyFlowServiceFactory factory,
                             Workflow workflow,
                             String versionName,
                             boolean creatable) {
        WorkflowVersion version = new WorkflowVersion(workflow);
        version.setVersionName(versionName);
        factory.workflowService.saveWorkflowVersion(version, creatable, false);
    }

    private Workflow cloneWorkflow(Workflow workflow) {
        return Workflow.formJson(workflow.toJson());
    }

    private User saveOperator(MyFlowServiceFactory factory, long id, String name) {
        User user = new User(id, name);
        factory.userGateway.save(user);
        return user;
    }

    private Set<String> workflowScriptKeys(List<WorkflowVersion> versions) {
        Set<String> keys = new HashSet<>();
        versions.forEach(version -> keys.addAll(
                GroovyScriptAnnotationScannerUtils.findGroovyScriptFields(version).getKeys()));
        return keys;
    }

    private List<String> versionNames(JSONObject workflowPackage) {
        return workflowPackage.getJSONArray("versions").stream()
                .map(JSONObject.class::cast)
                .map(version -> version.getString("versionName"))
                .toList();
    }

    private List<String> versionStates(JSONObject workflowPackage) {
        return workflowPackage.getJSONArray("versions").stream()
                .map(JSONObject.class::cast)
                .map(version -> String.format("%s:%s:%s",
                        version.getString("versionName"),
                        version.getBooleanValue("current"),
                        version.getBooleanValue("enable")))
                .toList();
    }

    private Set<String> collectConfigurationIds(JSONArray versions) {
        Set<String> ids = new HashSet<>();
        for (Object item : versions) {
            JSONObject version = (JSONObject) item;
            collectIds(version.get("form"), ids);
            collectIds(version.get("nodes"), ids);
        }
        return ids;
    }

    private void collectIds(Object value, Set<String> ids) {
        if (value instanceof Map<?, ?> map) {
            Object id = map.get("id");
            if (id != null) {
                ids.add(id.toString());
            }
            map.values().forEach(item -> collectIds(item, ids));
        } else if (value instanceof Collection<?> collection) {
            collection.forEach(item -> collectIds(item, ids));
        }
    }

    private Map<String, Long> scriptContentFrequency(JSONObject scripts) {
        Map<String, Long> frequency = new HashMap<>();
        scripts.values().forEach(value -> frequency.merge(value.toString(), 1L, Long::sum));
        return frequency;
    }

    private boolean disjoint(Set<String> left, Set<String> right) {
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        return intersection.isEmpty();
    }

    private String toDataUrl(String json) {
        return "data:application/json;base64," + Base64.getEncoder()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
