import {CheckCircleFilled, ClockCircleOutlined, SyncOutlined} from "@ant-design/icons";
import {Tag, Typography} from "antd";
import React from "react";
import {FlowApprovalOperator, ProcessNode} from "@flow-engine/flow-types";
import dayjs from "dayjs";

const {Text} = Typography;

// 获取状态配置
export const getStatusConfig = (status: 'completed' | 'current' | 'pending') => {
    switch (status) {
        case 'completed':
            return {
                color: 'success',
                label: '已审批',
                icon: <CheckCircleFilled style={{color: '#52c41a', fontSize: 16}}/>
            };
        case 'current':
            return {
                color: 'processing',
                label: '待审批',
                icon: <SyncOutlined spin style={{color: '#1890ff', fontSize: 16}}/>
            };
        case 'pending':
            return {
                color: 'default',
                label: '未执行',
                icon: <ClockCircleOutlined style={{color: '#d9d9d9', fontSize: 16}}/>
            };
    }
};


// 获取节点状态
export const getNodeStatus = (node: ProcessNode): 'completed' | 'current' | 'pending' => {
    if (node.state === -1) {
        return 'completed';
    }
    // 非历史节点，检查是否有审批人
    if (node.state === 0) {
        return 'current';
    }
    return 'pending';
};

interface FlowTimeNodeProps {
    node: ProcessNode;
}


interface FlowOperatorItemProps {
    operator: FlowApprovalOperator;
    state: number;
}

const FlowOperatorItem: React.FC<FlowOperatorItemProps> = (props) => {
    const operator = props.operator;
    const state = props.state;

    if (state === -1) {
        return (
            <>
                <Text type="secondary" style={{fontSize: 12}}>
                    审批人: {operator.flowOperator.name}
                </Text>
                {operator.approveTime > 0 && (
                    <Text type="secondary" style={{fontSize: 12}}>
                        {dayjs(operator.approveTime).format('YYYY-MM-DD HH:mm:ss')}  {operator.actionName}
                    </Text>
                )}
                {operator.advice && (
                    <div style={{
                        padding: 8,
                        backgroundColor: '#fafafa',
                        borderRadius: 4,
                        marginTop: 4
                    }}>
                        <Text type="secondary" style={{fontSize: 12}}>
                            {operator.advice}
                        </Text>
                    </div>
                )}
            </>
        )
    } else {
        return (
            <Text type="secondary" style={{fontSize: 12}}>
                待审批人: {operator.flowOperator.name}
            </Text>
        )
    }
}

export const FlowTimeNode: React.FC<FlowTimeNodeProps> = (props) => {
    const node = props.node;
    const operators = node.operators || [];
    return (
        <div style={{display: 'flex', flexDirection: 'column', gap: 4, width: '100%'}}>
            <div style={{display: 'flex', alignItems: 'center', gap: 8}}>
                <Text strong style={{fontSize: 14}}>{node.nodeName}</Text>
                <Tag color={getStatusConfig(getNodeStatus(node)).color} style={{margin: 0}}>
                    {getStatusConfig(getNodeStatus(node)).label}
                </Tag>
            </div>
            {operators.map(operator => {
                return (
                    <FlowOperatorItem operator={operator} state={node.state}/>
                )
            })}
        </div>
    )
}