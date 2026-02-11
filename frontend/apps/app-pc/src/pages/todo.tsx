import React from "react";
import {done, list, todo} from "@/api/record.ts";
import {
    type ActionType,
    ApprovalPanelDrawer,
    Table,
    type TableProps,
    WorkflowSelectModal
} from "@flow-engine/flow-design";
import {Button, Space, Tabs, type TabsProps} from "antd";

const TodoPage: React.FC = () => {

    const actionAll = React.useRef<ActionType>(null);
    const actionTodo = React.useRef<ActionType>(null);
    const actionDone = React.useRef<ActionType>(null);

    const [selectVisible, setSelectVisible] = React.useState(false);
    const [approvalVisible, setApprovalVisible] = React.useState(false);
    const [workflowCode, setWorkflowCode] = React.useState<string>('');

    const columns: TableProps<any>['columns'] = [
        {
            dataIndex: 'id',
            title: '编号',
        },
        {
            dataIndex: 'title',
            title: '流程名称',
        },
        {
            dataIndex: 'readTime',
            title: '读取状态',
        },
        {
            dataIndex: 'createTime',
            title: '创建时间',
        },
        {
            dataIndex: 'currentOperatorId',
            title: '审批人',
        },
        {
            dataIndex: 'recordState',
            title: '状态',
        },
        {
            dataIndex: 'option',
            title: '操作',
            render: (value, record) => {
                return (
                    <Space>
                        <a onClick={() => {

                        }}>办理</a>
                    </Space>
                )
            }
        }
    ];

    const items: TabsProps['items'] = [
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
        }
    ]

    return (
        <div>
            <Tabs
                items={items}
                centered={true}
                onChange={(currentKey) => {
                    if (currentKey === 'all') {
                        actionAll.current?.reload();
                    }
                    if (currentKey === 'done') {
                        actionDone.current?.reload();
                    }
                    if (currentKey === 'todo') {
                        actionTodo.current?.reload();
                    }
                }}
                tabBarExtraContent={{
                    right: (
                        <Button
                            key={"create"}
                            type={'primary'}
                            onClick={() => {
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
                onClose={() => {
                    setApprovalVisible(false);
                }}
            />
        </div>
    )
}

export default TodoPage;