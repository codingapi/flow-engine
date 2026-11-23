package com.codingapi.flow.operator;

import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.springboot.framework.user.IUser;

import java.util.Map;

/**
 * 流程参与用户
 *
 * <p>支持同一用户以多种身份参与流程（issue #288）：身份标识由下游业务自定义，
 * 框架不解释其含义，仅做透传、存储与比较。
 */
public interface IFlowOperator extends IUser {

    /**
     * 获取用户ID
     *
     * @return ID
     */
    long getUserId();


    /**
     * 获取用户名称
     *
     * @return 名称
     */
    String getName();


    /**
     * 获取身份标识
     *
     * <p>身份标识的取值语义由下游自定义（部门 id、岗位编码或复合标识均可），
     * 框架只做透传、存储与比较，不做任何解释。
     * <ul>
     *     <li>返回空串或 null 表示「默认身份」，与未启用多身份的历史行为完全一致；</li>
     *     <li>同一用户的不同身份在流程中被视为不同的参与人。</li>
     * </ul>
     *
     * @return 身份标识，默认返回空串
     * @see OperatorIdentity
     */
    default String getIdentity() {
        return OperatorIdentity.DEFAULT;
    }


    /**
     * 获取身份附加属性
     *
     * <p>供 Groovy 脚本与前端展示使用的动态字段（如部门名称、岗位名称），
     * 下游可自由定义 key/value。框架只透传，不落库、不参与任何引擎判定。
     *
     * @return 身份附加属性，默认返回空集合
     */
    default Map<String, String> getIdentityAttributes() {
        return Map.of();
    }


    /**
     * 是否流程管理员
     * 流程管理员可以强制干预流程
     */
    boolean isFlowManager();


    /**
     * 转交审批人
     * 转交审批人不为空时，当前操作者将由转交审批人操作者执行
     *
     * @param request 流程会话上下文，可用于根据表单数据、当前节点等条件动态决定转交人
     * @return 转交后的审批人，如果无需转交返回null
     */
    IFlowOperator forwardOperator(GroovyScriptRequest request);


}

