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
import {LeftOutlined, RightOutlined} from "@ant-design/icons";

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
            backgroundColor: '#f5f5f5',
            width: '100%',
            height: `calc(100vh - ${ApprovalLayoutHeight}px)`,
        }}>
            <Content
                style={{
                    overflow: 'auto',
                    width: '100%',
            }}>
                <Card
                    title={<Title level={4} style={{margin: 0}}>流程表单</Title>}
                    style={{height: '100%', borderRadius: 8}}
                    styles={{body: {padding: 24}}}
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
                    marginLeft: "8px",
                    backgroundColor: '#fff',
                    borderRadius: 8,
                    overflow: 'hidden',
                }}
            >
                {!collapsed && (
                    <Flex vertical style={{height: '100%'}}>
                        <Flex
                            justify="space-between"
                            align="center"
                            style={{
                                padding: '16px 24px',
                                borderBottom: '1px solid #f0f0f0',
                                backgroundColor: '#fafafa'
                            }}
                        >
                            <Title level={5} style={{margin: 0, fontSize: 15}}>流程记录</Title>
                            <Button
                                type="text"
                                size="small"
                                icon={<LeftOutlined style={{fontSize: 12}}/>}
                                onClick={() => setCollapsed(true)}
                                style={{color: '#8c8c8c'}}
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
                        style={{height: '100%', padding: '12px 8px', backgroundColor: '#fafafa'}}
                    >
                        <Button
                            type="text"
                            size="small"
                            icon={<RightOutlined style={{fontSize: 12}}/>}
                            onClick={() => setCollapsed(false)}
                            style={{color: '#8c8c8c'}}
                        />
                        <div style={{
                            writingMode: 'vertical-rl',
                            textOrientation: 'mixed',
                            letterSpacing: 4,
                            fontSize: 14,
                            fontWeight: 500,
                            color: '#595959',
                        }}>
                            流程记录
                        </div>
                    </Flex>
                )}
            </Sider>
        </Layout>
    )
}