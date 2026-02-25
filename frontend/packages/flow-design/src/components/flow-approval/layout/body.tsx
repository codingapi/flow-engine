import React, {useState} from "react";
import {FormViewComponent} from "@/components/flow-approval/components/form-view-component";
import {FlowNodeHistory, FlowNodeHistoryAction} from "@/components/flow-approval/components/flow-node-history";
import {
    ApprovalContentPaddingV,
    ApprovalContentPaddingH,
    ApprovalSidebarWidth,
    ApprovalSidebarCollapsedWidth,
    ApprovalLayoutHeight,
} from "@/components/flow-approval/typings";
import {Layout, Card, Button, Typography, Flex} from "antd";

const {Sider, Content} = Layout;
const {Title} = Typography;

export const Body = () => {
    const [collapsed, setCollapsed] = useState(false);
    const flowNodeHistoryAction = React.useRef<FlowNodeHistoryAction>(null);

    const handleValuesChange = (values:any) => {
        flowNodeHistoryAction.current?.refresh();
    }

    return (
        <Layout style={{
            padding: `${ApprovalContentPaddingV}px ${ApprovalContentPaddingH}px`,
            backgroundColor: '#fff',
            height: `calc(100vh - ${ApprovalLayoutHeight}px)`,
        }}>
            <Content style={{overflow: 'auto'}}>
                <Card
                    title={<Title level={4} style={{margin: 0}}>流程表单</Title>}
                    bordered={true}
                    style={{height: '100%'}}
                >
                    <FormViewComponent
                        onValuesChange={handleValuesChange}
                    />
                </Card>
            </Content>

            <Sider
                width={ApprovalSidebarWidth}
                collapsedWidth={ApprovalSidebarCollapsedWidth}
                collapsible
                collapsed={collapsed}
                onCollapse={setCollapsed}
                trigger={null}
                style={{
                    backgroundColor: '#fff',
                    borderLeft: '1px solid #f0f0f0',
                }}
            >
                {!collapsed && (
                    <Flex vertical style={{height: '100%'}}>
                        <Flex
                            justify="space-between"
                            align="center"
                            style={{padding: '16px 24px', borderBottom: '1px solid #f0f0f0'}}
                        >
                            <Title level={5} style={{margin: 0}}>审批节点历史记录</Title>
                            <Button
                                type="text"
                                size="small"
                                icon={<span>◀</span>}
                                onClick={() => setCollapsed(true)}
                            />
                        </Flex>
                        <div style={{padding: 16, flex: 1, overflow: 'auto'}}>
                            <FlowNodeHistory actionRef={flowNodeHistoryAction}/>
                        </div>
                    </Flex>
                )}
                {collapsed && (
                    <Flex
                        vertical
                        align="center"
                        justify="center"
                        gap={16}
                        style={{height: '100%', padding: '12px 8px'}}
                    >
                        <Button
                            type="text"
                            size="small"
                            icon={<span>▶</span>}
                            onClick={() => setCollapsed(false)}
                        />
                        <div style={{
                            writingMode: 'vertical-rl',
                            textOrientation: 'mixed',
                            letterSpacing: 4,
                            fontSize: 14,
                            fontWeight: 600,
                        }}>
                            审批历史
                        </div>
                    </Flex>
                )}
            </Sider>
        </Layout>
    )
}