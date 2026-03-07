import React from "react";
import {Table} from "@flow-engine/flow-pc-ui";
import {Button, Popconfirm, Space} from "antd";
import {PlusOutlined} from "@ant-design/icons";
import {useConditionContext} from "../../hooks/use-condition-context";
import {ConditionItemView} from "./condition";
import {ConditionTypeView} from "./type";

/**
 * @constructor
 */
export const Group = () => {

    const {state, context} = useConditionContext();
    const presenter = context.getPresenter().getConditionGroupPresenter();

    return (
        <div style={{
            backgroundColor: '#fff',
            color: 'rgba(0,0,0,0.88)',
            minHeight: '60px',
        }}>
            <Table
                toolBarRender={() => {
                    return [
                        <Button
                            icon={<PlusOutlined/>}
                            onClick={() => {
                                presenter.addCondition();
                            }}
                        >添加条件</Button>
                    ]
                }}
                columns={[
                    {
                        key: 'id',
                        title: 'id',
                        hidden: true,
                        dataIndex: 'id',
                    },
                    {
                        key: 'left',
                        title: '左侧参数',
                        dataIndex: 'left',
                        render: (_, record) => {
                            return (<ConditionItemView
                                id={record.id}
                                location={'left'}
                                data={record.left}
                            />)
                        }
                    },
                    {
                        key: 'type',
                        title: '条件关系',
                        dataIndex: 'type',
                        render: (_, record) => {
                            return (<ConditionTypeView
                                type={record.type}
                                id={record.id}
                            />)
                        }
                    },
                    {
                        key: 'right',
                        title: '右侧参数',
                        dataIndex: 'right',
                        render: (_, record) => {
                            return (<ConditionItemView
                                id={record.id}
                                location={'right'}
                                data={record.right}
                            />)
                        }
                    },
                    {
                        key: 'option',
                        title: '操作',
                        render: (text, record) => {
                            return (
                                <Space>
                                    <a
                                        onClick={() => {
                                            presenter.switchCondition(record.id);
                                        }}
                                    >互换</a>
                                    <Popconfirm
                                        title={"确认要删除吗？"}
                                        onConfirm={() => {
                                            presenter.removeCondition(record.id);
                                        }}
                                    >
                                        <a>删除</a>
                                    </Popconfirm>
                                </Space>
                            )
                        }
                    },
                ]}
                dataSource={state.groups}
                pagination={false}
            />
        </div>
    )
}