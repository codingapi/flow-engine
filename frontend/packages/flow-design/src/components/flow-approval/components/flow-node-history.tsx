import React from "react";
import {useApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";
import {ProcessNode} from "@/components/flow-approval/typings";
import {Timeline, Tag, Badge, Empty, Typography, Space} from "antd";
import {CheckCircleFilled, ClockCircleOutlined, LoadingOutlined} from "@ant-design/icons";

const {Text, Paragraph} = Typography;

export interface FlowNodeHistoryAction{
    refresh:()=>void;
}

interface FlowNodeHistoryProps{
    actionRef?:React.Ref<FlowNodeHistoryAction>;
}

// 获取节点状态
const getNodeStatus = (node: ProcessNode): 'completed' | 'current' | 'pending' => {
    if (node.operators && node.operators.length > 0) {
        return 'completed';
    }
    return 'pending';
};

// 获取状态配置
const getStatusConfig = (status: 'completed' | 'current' | 'pending') => {
    switch (status) {
        case 'completed':
            return {
                color: 'success',
                label: '通过',
                icon: <CheckCircleFilled style={{color: '#52c41a', fontSize: 16}}/>
            };
        case 'current':
            return {
                color: 'error',
                label: '待审批',
                icon: <LoadingOutlined style={{color: '#ff4d4f', fontSize: 16}}/>
            };
        case 'pending':
            return {
                color: 'default',
                label: '待处理',
                icon: <ClockCircleOutlined style={{color: '#d9d9d9', fontSize: 16}}/>
            };
    }
};

// 单个审批节点项组件
const ApprovalNodeItem: React.FC<{node: ProcessNode}> = ({node}) => {
    const status = getNodeStatus(node);
    const firstOperator = node.operators?.[0];
    const statusConfig = getStatusConfig(status);

    return (
        <Timeline.Item dot={statusConfig.icon}>
            <Space direction="vertical" size={8} style={{width: '100%'}}>
                <Space size={12}>
                    <Text strong>{node.nodeName}</Text>
                    <Tag color={statusConfig.color}>{statusConfig.label}</Tag>
                </Space>

                {firstOperator && (
                    <Text type="secondary">审批人: {firstOperator.flowOperator.name}</Text>
                )}

                {firstOperator?.approveTime && (
                    <Text type="secondary" style={{fontSize: 12}}>
                        {new Date(firstOperator.approveTime).toLocaleString('zh-CN', {
                            year: 'numeric',
                            month: '2-digit',
                            day: '2-digit',
                            hour: '2-digit',
                            minute: '2-digit',
                        })}
                    </Text>
                )}

                {firstOperator?.advice && (
                    <div style={{marginTop: 4}}>
                        <Text type="secondary">审批意见</Text>
                        <div style={{
                            padding: 12,
                            backgroundColor: '#fafafa',
                            borderRadius: 4,
                            marginTop: 6
                        }}>
                            <Text>{firstOperator.advice}</Text>
                        </div>
                    </div>
                )}
            </Space>
        </Timeline.Item>
    );
};

export const FlowNodeHistory: React.FC<FlowNodeHistoryProps> = (props) => {
    const {context} = useApprovalContext();
    const [processNodes, setProcessNodes] = React.useState<ProcessNode[]>([]);

    const triggerProcessNodes = () => {
        context.getPresenter().processNodes().then(nodes => {
            setProcessNodes(nodes);
        });
    }

    React.useEffect(()=>{
        triggerProcessNodes();
    },[]);

    React.useImperativeHandle(props.actionRef,()=>{
        return {
            refresh:()=>{
                triggerProcessNodes();
            }
        }
    },[]);

    return (
        <>
            {processNodes.length > 0 ? (
                <Timeline>
                    {processNodes.map(node => (
                        <ApprovalNodeItem key={node.nodeId} node={node}/>
                    ))}
                </Timeline>
            ) : (
                <Empty description="暂无审批历史记录" />
            )}
        </>
    )
}