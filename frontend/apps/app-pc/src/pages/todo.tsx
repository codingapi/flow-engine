import React from "react";
import {done, list, notify, todo} from "@/api/record.ts";
import {WorkflowSelectModal} from "@flow-engine/flow-pc-design";
import {Table, type TableProps} from "@flow-engine/flow-pc-ui";
import {Button, Space, Tabs, type TabsProps} from "antd";
import dayjs from "dayjs";
import {ApprovalPanelDrawer,FlowTitle} from "@flow-engine/flow-pc-approval";
import type { ActionType } from "@flow-engine/flow-core";

const TodoPage: React.FC = () => {

    const actionAll = React.useRef<ActionType>(null);
    const actionTodo = React.useRef<ActionType>(null);
    const actionDone = React.useRef<ActionType>(null);
    const actionNotify = React.useRef<ActionType>(null);

    const [selectVisible, setSelectVisible] = React.useState(false);
    const [approvalVisible, setApprovalVisible] = React.useState(false);
    const [workflowCode, setWorkflowCode] = React.useState<string>('');
    const [currentRecordId, setCurrentRecordId] = React.useState<string>('');
    const [currentTab, setCurrentTab] = React.useState<string>('todo');

    const columns: TableProps<any>['columns'] = [
        {
            dataIndex: 'recordId',
            title: '编号',
        },
        {
            dataIndex: 'title',
            title: '流程名称',
            render:(value)=>{
                return <FlowTitle title={value}/>
            }
        },
        {
            dataIndex: 'readTime',
            title: '读取状态',
            render: (value) => {
                return value ? '已读' : '未读';
            }
        },
        {
            dataIndex: 'nodeName',
            title: '节点名称',
        },
        {
            dataIndex: 'createTime',
            title: '创建时间',
            render: (text) => {
                return dayjs(text).format('YYYY-MM-DD HH:mm:ss');
            }
        },
        {
            dataIndex: 'currentOperatorId',
            title: '审批人',
            hidden: true,
        },
        {
            dataIndex: 'currentOperatorName',
            title: '审批人',
        },
        {
            dataIndex: 'recordState',
            title: '状态',
            render: (text) => {
                return text ? '已办' : '待办';
            }
        },
        {
            dataIndex: 'option',
            title: '操作',
            render: (_, record) => {
                if(currentTab==='todo'){
                    return (
                        <Space>
                            <a
                                onClick={() => {
                                    setCurrentRecordId(record.recordId);
                                    setApprovalVisible(true);
                                }}
                            >办理</a>
                        </Space>
                    )
                }

            }
        }
    ];

    const items: TabsProps['items'] = [
        {
            key: 'todo',
            label: '我的待办',
            children: (
                <Table
                    rowKey={"id"}
                    actionType={actionTodo}
                    columns={columns}
                    request={(request) => {
                        return todo(request);
                    }}
                />
            )
        },
        {
            key: 'done',
            label: '我的已办',
            children: (
                <Table
                    rowKey={"id"}
                    actionType={actionDone}
                    columns={columns}
                    request={(request) => {
                        return done(request);
                    }}
                />
            )
        },
        {
            key: 'notify',
            label: '我的抄送',
            children: (
                <Table
                    rowKey={"id"}
                    actionType={actionNotify}
                    columns={columns}
                    request={(request) => {
                        return notify(request);
                    }}
                />
            )
        },
        {
            key: 'all',
            label: '全部流程',
            children: (
                <Table
                    rowKey={"id"}
                    actionType={actionAll}
                    columns={columns}
                    request={(request) => {
                        return list(request);
                    }}
                />
            )
        },
    ];

    const reloadCurrentTab = () => {
        if (currentTab === 'all') {
            actionAll.current?.reload();
        }
        if (currentTab === 'done') {
            actionDone.current?.reload();
        }
        if (currentTab === 'todo') {
            actionTodo.current?.reload();
        }
        if (currentTab === 'notify') {
            actionNotify.current?.reload();
        }
    }

    React.useEffect(() => {
        reloadCurrentTab();
    },[currentTab]);

    return (
        <div>
            <Tabs
                items={items}
                centered={true}
                defaultActiveKey={currentTab}
                onChange={(currentKey) => {
                   setCurrentTab(currentKey);
                }}
                tabBarExtraContent={{
                    right: (
                        <Button
                            key={"create"}
                            type={'primary'}
                            onClick={() => {
                                setCurrentRecordId('');
                                setSelectVisible(true);
                            }}>发起流程</Button>
                    )
                }}
            />

            <WorkflowSelectModal
                open={selectVisible}
                onSelect={(code) => {
                    setWorkflowCode(code);
                    setSelectVisible(false);
                    setApprovalVisible(true);
                }}
                onClose={() => {
                    setSelectVisible(false);
                }}
            />

            <ApprovalPanelDrawer
                workflowCode={workflowCode}
                open={approvalVisible}
                recordId={currentRecordId}
                onClose={() => {
                    setApprovalVisible(false);
                    reloadCurrentTab();
                }}
            />
        </div>
    )
}

export default TodoPage;