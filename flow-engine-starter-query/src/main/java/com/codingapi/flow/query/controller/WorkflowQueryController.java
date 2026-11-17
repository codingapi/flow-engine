package com.codingapi.flow.query.controller;

import com.codingapi.flow.infra.entity.WorkflowEntity;
import com.codingapi.flow.infra.jpa.WorkflowEntityRepository;
import com.codingapi.flow.infra.jpa.WorkflowVersionEntityRepository;
import com.codingapi.flow.infra.pojo.WorkflowOption;
import com.codingapi.flow.infra.pojo.WorkflowVersionOption;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.springboot.framework.dto.request.IdRequest;
import com.codingapi.springboot.framework.dto.request.SearchRequest;
import com.codingapi.springboot.framework.dto.response.MultiResponse;
import com.codingapi.springboot.framework.dto.response.SingleResponse;
import com.codingapi.springboot.framework.user.UserContext;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/query/workflow")
@AllArgsConstructor
public class WorkflowQueryController {

    private final WorkflowEntityRepository workflowEntityRepository;

    private final WorkflowVersionEntityRepository workflowVersionEntityRepository;

    @GetMapping("/list")
    public MultiResponse<WorkflowEntity> list(SearchRequest searchRequest) {
        return MultiResponse.of(workflowEntityRepository.searchRequest(searchRequest));
    }

    @GetMapping("/options")
    public MultiResponse<WorkflowOption> options() {
        return MultiResponse.of(workflowEntityRepository.options());
    }

    @GetMapping("/versions")
    public MultiResponse<WorkflowVersionOption> versions(IdRequest request) {
        return MultiResponse.of(workflowVersionEntityRepository.versions(request.getStringId()));
    }

    /**
     * 当前用户是否可执行「强制同步运行中实例」（仅流程管理员可操作）
     */
    @GetMapping("/permission/forceSync")
    public SingleResponse<Boolean> forceSyncPermission() {
        return SingleResponse.of(isFlowManager());
    }

    private boolean isFlowManager() {
        try {
            Object current = UserContext.getInstance().current();
            return current instanceof IFlowOperator operator && operator.isFlowManager();
        } catch (Exception e) {
            // 未登录或无用户上下文时视为无权限
            return false;
        }
    }

}
