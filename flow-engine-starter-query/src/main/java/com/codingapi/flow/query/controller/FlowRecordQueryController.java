package com.codingapi.flow.query.controller;

import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.mock.MockInstance;
import com.codingapi.flow.mock.MockInstanceFactory;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.operator.OperatorIdentity;
import com.codingapi.flow.pojo.response.FlowRecordContent;
import com.codingapi.flow.query.FlowRecordQueryService;
import com.codingapi.flow.query.pojo.FlowInstanceQueryRequest;
import com.codingapi.springboot.framework.dto.request.SearchRequest;
import com.codingapi.springboot.framework.dto.response.MultiResponse;
import com.codingapi.springboot.framework.user.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@RestController
@RequestMapping("/api/query/record")
@AllArgsConstructor
public class FlowRecordQueryController {

    private final FlowRecordQueryService flowRecordQueryService;

    private FlowRecordQueryService loadFlowService(){
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String key = request.getParameter("mockKey");
        if(StringUtils.hasText(key)) {
            MockInstance mockInstance = MockInstanceFactory.getInstance().getMockInstance(key);
            if(mockInstance!=null){
                return mockInstance.getFlowRecordQueryService();
            }
        }
        return this.flowRecordQueryService;
    }

    private long loadCurrentOperatorId(){
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String key = request.getParameter("operatorId");
        if(StringUtils.hasText(key)) {
            return Long.parseLong(key);
        }else {
            IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
            return current.getUserId();
        }
    }

    /**
     * 当前操作人身份标识（issue #288）。
     *
     * <p>优先取请求参数 {@code operatorIdentity}（宿主可通过它声明「我以哪个身份查看」），
     * 其次取当前登录操作者自身的身份，都为空时表示默认身份。
     */
    private String loadCurrentOperatorIdentity(){
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String key = request.getParameter("operatorIdentity");
        if(request.getParameterMap().containsKey("operatorIdentity")) {
            return OperatorIdentity.normalize(key);
        }
        IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
        return current == null ? OperatorIdentity.DEFAULT : OperatorIdentity.of(current);
    }


    /**
     * 全部流程记录接口
     */
    @GetMapping("/list")
    public MultiResponse<FlowRecordContent> list(SearchRequest request) {
        FlowRecordQueryService flowRecordQueryService = loadFlowService();
        PageRequest pageRequest = PageRequest.of(request.getCurrent(), request.getPageSize()).withSort(Sort.by("id").descending());
        return MultiResponse.of(flowRecordQueryService.findAll(pageRequest));
    }


    /**
     * 我的待办记录
     */
    @GetMapping("/todo")
    public MultiResponse<FlowRecordContent> todo(SearchRequest request) {
        long operatorId = loadCurrentOperatorId();
        String operatorIdentity = loadCurrentOperatorIdentity();
        FlowRecordQueryService flowRecordQueryService = loadFlowService();
        PageRequest pageRequest = PageRequest.of(request.getCurrent(), request.getPageSize()).withSort(Sort.by("id").descending());
        return MultiResponse.of(flowRecordQueryService.findTodoRecordPage(operatorId,operatorIdentity,pageRequest));
    }

    /**
     * 我的抄送记录
     */
    @GetMapping("/notify")
    public MultiResponse<FlowRecordContent> notify(SearchRequest request) {
        long operatorId = loadCurrentOperatorId();
        String operatorIdentity = loadCurrentOperatorIdentity();
        FlowRecordQueryService flowRecordQueryService = loadFlowService();
        PageRequest pageRequest = PageRequest.of(request.getCurrent(), request.getPageSize()).withSort(Sort.by("id").descending());
        return MultiResponse.of(flowRecordQueryService.findNotifyRecordPage(operatorId,operatorIdentity,pageRequest));
    }


    /**
     * 我的已办记录
     */
    @GetMapping("/done")
    public MultiResponse<FlowRecordContent> done(SearchRequest request) {
        long operatorId = loadCurrentOperatorId();
        String operatorIdentity = loadCurrentOperatorIdentity();
        FlowRecordQueryService flowRecordQueryService = loadFlowService();
        PageRequest pageRequest = PageRequest.of(request.getCurrent(), request.getPageSize()).withSort(Sort.by("id").descending());
        return MultiResponse.of(flowRecordQueryService.findDoneRecordPage(operatorId,operatorIdentity,pageRequest));
    }

    /**
     * 指定流程编码下运行中的流程实例（用于强制同步时选择范围，issue #281）
     */
    @GetMapping("/running")
    public MultiResponse<FlowRecordContent> running(FlowInstanceQueryRequest request) {
        if (!StringUtils.hasText(request.getWorkCode())) {
            throw FlowValidationException.required("workCode");
        }
        FlowRecordQueryService flowRecordQueryService = loadFlowService();
        PageRequest pageRequest = PageRequest.of(Math.max(request.getCurrent() - 1, 0), request.getPageSize());
        return MultiResponse.of(flowRecordQueryService.findRunningInstancePage(
                request.getWorkCode(), request.getTitle(), pageRequest));
    }
}
