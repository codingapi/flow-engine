import React from "react";
import {Table} from "@flow-engine/flow-pc-ui";
import {Button, Popconfirm, Space} from "antd";
import {PlusOutlined} from "@ant-design/icons";
import {useConditionContext} from "../hooks/use-condition-context";
import {Condition, RelationType, relationTypeOptions} from "../typings";


interface ConditionItemViewProps {
    data?: Condition;
}

const ConditionItemView: React.FC<ConditionItemViewProps> = (props) => {
    const label = props.data?.label || '未设置';
    return (
        <span>{label}</span>
    )
}

interface ConditionTypeViewProps {
    type?: RelationType;
}

const ConditionTypeView: React.FC<ConditionTypeViewProps> = (props) => {
    const type = props.type;
    const typeLabel = relationTypeOptions.find(item => item.value === type);
    if (typeLabel) {
        return (
            <span>{typeLabel.label}</span>
        )
    }
    return (
        <span>未知</span>
    )
}

/**
 * @constructor
 */
export const ConditionGroup = () => {

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
                            return (<ConditionItemView data={record.left}/>)
                        }
                    },
                    {
                        key: 'type',
                        title: '条件关系',
                        dataIndex: 'type',
                        render: (_, record) => {
                            return (<ConditionTypeView type={record.type}/>)
                        }
                    },
                    {
                        key: 'right',
                        title: '右侧参数',
                        dataIndex: 'right',
                        render: (_, record) => {
                            return (<ConditionItemView data={record.right}/>)
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