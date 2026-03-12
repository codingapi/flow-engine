package com.codingapi.flow.api.controller;

import com.alibaba.fastjson.JSONObject;
import com.codingapi.flow.api.pojo.NodeCreateRequest;
import com.codingapi.flow.api.pojo.WorkflowUpdateVersionNameRequest;
import com.codingapi.flow.exception.FlowPermissionException;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.mock.MockInstance;
import com.codingapi.flow.mock.MockInstanceFactory;
import com.codingapi.flow.node.IBlockNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.node.factory.NodeFactory;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.repository.WorkflowRepository;
import com.codingapi.flow.service.WorkflowService;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import com.codingapi.flow.workflow.WorkflowVersion;
import com.codingapi.springboot.framework.dto.request.IdRequest;
import com.codingapi.springboot.framework.dto.response.Response;
import com.codingapi.springboot.framework.dto.response.SingleResponse;
import com.codingapi.springboot.framework.exception.LocaleMessageException;
import com.codingapi.springboot.framework.user.UserContext;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;


@RestController
@RequestMapping("/api/cmd/workflow")
@AllArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowRepository workflowRepository;
    private final FlowOperatorGateway flowOperatorGateway;

    @PostMapping("/remove")
    public Response remove(@RequestBody IdRequest request) {
        workflowService.delete(request.getStringId());
        return Response.buildSuccess();
    }

    @PostMapping("/updateVersionName")
    public Response updateVersionName(@RequestBody WorkflowUpdateVersionNameRequest request) {
        workflowService.updateVersionName(request.getId(), request.getVersionName());
        return Response.buildSuccess();
    }


    @PostMapping("/changeVersion")
    public Response changeVersion(@RequestBody IdRequest request) {
        workflowService.changeVersion(request.getLongId());
        return Response.buildSuccess();
    }


    @PostMapping("/deleteVersion")
    public Response deleteVersion(@RequestBody IdRequest request) {
        workflowService.deleteVersion(request.getLongId());
        return Response.buildSuccess();
    }


    @PostMapping("/changeState")
    public Response changeState(@RequestBody IdRequest request) {
        Workflow workflow = workflowService.getWorkflow(request.getStringId());
        if (workflow.isDisable()) {
            workflow.enable();
        } else {
            workflow.disable();
        }
        workflowService.saveWorkflow(workflow);
        return Response.buildSuccess();
    }

    @PostMapping("/mock")
    public SingleResponse<String> mock() {
        IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
        if (current != null) {
            if (!current.isFlowManager()) {
                throw FlowPermissionException.accessDenied(current.getName());
            }
        }
        MockInstance mockInstance = MockInstanceFactory.getInstance().create(flowOperatorGateway, workflowRepository);
        return SingleResponse.of(mockInstance.getMockKey());
    }

    @PostMapping("/cleanMock")
    public Response mock(@RequestBody IdRequest request) {
        MockInstanceFactory.getInstance().clear(request.getStringId());
        return Response.buildSuccess();
    }

    @PostMapping("/create")
    public SingleResponse<JSONObject> create() {
        Workflow workflow = WorkflowBuilder.builder()
                .build(false);
        workflow.addDefaultNodesAndEdges();
        JSONObject jsonObject = JSONObject.parseObject(workflow.toJson());
        return SingleResponse.of(jsonObject);
    }

    @PostMapping("/import")
    public SingleResponse<String> importWorkflow(@RequestBody JSONObject body) {
        String workId = workflowService.importWorkflow(body.getString("file"));
        return SingleResponse.of(workId);
    }

    @GetMapping("/export")
    public void export(IdRequest request, HttpServletResponse response) {
        Workflow workflow = workflowService.getWorkflow(request.getStringId());
        JSONObject jsonObject = JSONObject.parseObject(workflow.toJson());
        try {
            response.setContentType("application/json;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            String fileName = URLEncoder.encode("workflow_" + request.getStringId() + ".json", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            response.getWriter().write(jsonObject.toJSONString());
            response.getWriter().flush();
        } catch (Exception e) {
            throw new LocaleMessageException("export.error", e);
        }
    }

    @PostMapping("/create-node")
    public SingleResponse<Map<String, Object>> createNode(@RequestBody NodeCreateRequest request) {
        NodeType type = NodeType.valueOf(request.getType());
        IFlowNode node = NodeFactory.getInstance().createNode(type);
        if (node instanceof IBlockNode blockNode) {
            blockNode.addDefaultBranch(2);
        }
        return SingleResponse.of(node.toMap());
    }

    @PostMapping("/save")
    public Response save(@RequestBody JSONObject request) {
        IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
        if (current != null) {
            request.put("createdOperator", String.valueOf(current.getUserId()));
        }
        Workflow workflow = Workflow.formJson(request.toJSONString());
        workflow.updateTime();
        String versionName = request.getString("versionName");
        if (StringUtils.hasText(versionName)) {
            WorkflowVersion workflowVersion = new WorkflowVersion(workflow);
            workflowVersion.setVersionName(versionName);
            workflowService.saveWorkflowVersion(workflowVersion, true);
        } else {
            workflowService.saveWorkflow(workflow);
        }
        return Response.buildSuccess();
    }

    @GetMapping("/load")
    public SingleResponse<JSONObject> load(String id) {
        Workflow workflow = workflowService.getWorkflow(id);
        JSONObject jsonObject = JSONObject.parseObject(workflow.toJson());
        return SingleResponse.of(jsonObject);
    }

}
