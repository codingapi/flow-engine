package com.codingapi.flow.query.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 运行中流程实例分页查询请求（issue #281）
 */
@Data
@NoArgsConstructor
public class FlowInstanceQueryRequest {

    /**
     * 流程编码
     */
    private String workCode;

    /**
     * 流程标题（模糊匹配，可为空）
     */
    private String title;

    /**
     * 页码（从1开始，与前端分页组件保持一致）
     */
    private int current = 1;

    /**
     * 每页数量
     */
    private int pageSize = 20;
}
