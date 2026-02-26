import React from "react";
import {useApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";
import {ProcessNode} from "@flow-engine/flow-types";
import {Timeline, Tag, Empty, Typography} from "antd";
import {CheckCircleFilled, ClockCircleOutlined, SyncOutlined} from "@ant-design/icons";

const {Text} = Typography;

export interface FlowNodeHistoryAction{
    refresh:()=>void;
}

interface FlowNodeHistoryProps{
    actionRef?:React.Ref<FlowNodeHistoryAction>;
}

// 获取节点状态
const getNodeStatus = (node: ProcessNode): 'completed' | 'current' | 'pending' => {
    if (node.state===-1) {
        return 'completed';
    }
    // 非历史节点，检查是否有审批人
    if (node.state === 0) {
        return 'current';
    }
    return 'pending';
};

// 获取状态配置
const getStatusConfig = (status: 'completed' | 'current' | 'pending') => {
    switch (status) {
        case 'completed':
            return {
                color: 'success',
                label: '已通过',
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
                <Timeline items={processNodes.map(node => ({
                    icon: getStatusConfig(getNodeStatus(node)).icon,
                    content: (
                        <div style={{display: 'flex', flexDirection: 'column', gap: 4, width: '100%'}}>
                            <div style={{display: 'flex', alignItems: 'center', gap: 8}}>
                                <Text strong style={{fontSize: 14}}>{node.nodeName}</Text>
                                <Tag color={getStatusConfig(getNodeStatus(node)).color} style={{margin: 0}}>
                                    {getStatusConfig(getNodeStatus(node)).label}
                                </Tag>
                            </div>
                            {node.state===-1 && node.operators?.[0] && (
                                <>
                                    <Text type="secondary" style={{fontSize: 12}}>
                                        审批人: {node.operators[0].flowOperator.name}
                                    </Text>
                                    {node.operators[0].approveTime > 0 && (
                                        <Text type="secondary" style={{fontSize: 12}}>
                                            {new Date(node.operators[0].approveTime).toLocaleString('zh-CN', {
                                                year: 'numeric',
                                                month: '2-digit',
                                                day: '2-digit',
                                                hour: '2-digit',
                                                minute: '2-digit',
                                            })}
                                        </Text>
                                    )}
                                    {node.operators[0].advice && (
                                        <div style={{
                                            padding: 8,
                                            backgroundColor: '#fafafa',
                                            borderRadius: 4,
                                            marginTop: 4
                                        }}>
                                            <Text type="secondary" style={{fontSize: 12}}>
                                                {node.operators[0].advice}
                                            </Text>
                                        </div>
                                    )}
                                </>
                            )}
                            {node.state!==-1 && node.operators?.[0] && (
                                <Text type="secondary" style={{fontSize: 12}}>
                                    待审批人: {node.operators[0].flowOperator.name}
                                </Text>
                            )}
                        </div>
                    )
                }))} />
            ) : (
                <Empty description="暂无审批流程记录" />
            )}
        </>
    )
}