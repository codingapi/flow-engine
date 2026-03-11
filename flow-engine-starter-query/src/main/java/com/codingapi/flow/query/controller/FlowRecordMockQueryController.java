package com.codingapi.flow.query.controller;

import com.codingapi.flow.infra.entity.FlowRecordEntity;
import com.codingapi.flow.mock.FlowQueryMockService;
import com.codingapi.flow.mock.FlowServiceMockFactory;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.response.FlowRecordContent;
import com.codingapi.springboot.framework.dto.request.Relation;
import com.codingapi.springboot.framework.dto.request.SearchRequest;
import com.codingapi.springboot.framework.dto.response.MultiResponse;
import com.codingapi.springboot.framework.user.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@RestController
@RequestMapping("/api/query/record-mock")
@AllArgsConstructor
public class FlowRecordMockQueryController {

    private FlowQueryMockService loadFlowService(){
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String key = request.getParameter("mockKey");
        if(StringUtils.hasText(key)) {
            return FlowServiceMockFactory.getInstance().getFlowQueryService(key);
        }else {
            return null;
        }
    }
    /**
     * 全部流程记录接口
     */
    @GetMapping("/list")
    public MultiResponse<FlowRecordContent> list(SearchRequest request) {
        FlowQueryMockService flowQueryMockService = this.loadFlowService();
        if(flowQueryMockService==null){
            return MultiResponse.empty();
        }
        PageRequest pageRequest = PageRequest.of(request.getCurrent(),request.getPageSize());
        return MultiResponse.of(flowQueryMockService.findAll(pageRequest));
    }


    /**
     * 我的待办记录
     */
    @GetMapping("/todo")
    public MultiResponse<FlowRecordContent> todo(SearchRequest request) {
        FlowQueryMockService flowQueryMockService = this.loadFlowService();
        if(flowQueryMockService==null){
            return MultiResponse.empty();
        }
        IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
        PageRequest pageRequest = request.toPageRequest(FlowRecordEntity.class);
        return MultiResponse.of(flowQueryMockService.findTodoRecordPage(current.getUserId(),pageRequest.withSort(Sort.by("id").descending())));
    }

    /**
     * 我的抄送记录
     */
    @GetMapping("/notify")
    public MultiResponse<FlowRecordContent> notify(SearchRequest request) {
        FlowQueryMockService flowQueryMockService = this.loadFlowService();
        if(flowQueryMockService==null){
            return MultiResponse.empty();
        }
        IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
        PageRequest pageRequest = request.toPageRequest(FlowRecordEntity.class);
        return MultiResponse.of(flowQueryMockService.findNotifyRecordPage(current.getUserId(),pageRequest.withSort(Sort.by("id").descending())));
    }


    /**
     * 我的已办记录
     */
    @GetMapping("/done")
    public MultiResponse<FlowRecordContent> done(SearchRequest request) {
        FlowQueryMockService flowQueryMockService = this.loadFlowService();
        if(flowQueryMockService==null){
            return MultiResponse.empty();
        }
        IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
        PageRequest pageRequest = request.toPageRequest(FlowRecordEntity.class);
        return MultiResponse.of(flowQueryMockService.findDoneRecordPage(current.getUserId(),pageRequest.withSort(Sort.by("id").descending())));
    }
}
