import React from "react";
import {Table} from "@flow-engine/flow-pc-ui";
import {Button, Space } from "antd";
import { PlusOutlined } from "@ant-design/icons";
import {useConditionContext} from "../hooks/use-condition-context";

/**
 * @constructor
 */
export const ConditionGroup = () => {

    const {state} = useConditionContext();

    return (
        <div style={{
            backgroundColor: '#fff',
            color: 'rgba(0,0,0,0.88)',
            minHeight: '60px',
        }}>
            <Table
                toolBarRender={()=>{
                    return [
                        <Button icon={<PlusOutlined />}>添加条件</Button>
                    ]
                }}
                columns={[
                    {
                        key:'id',
                        title:'id',
                        hidden:true,
                        dataIndex: 'id',
                    },
                    {
                        key:'left',
                        title:'左侧参数',
                        dataIndex:'left',
                    },
                    {
                        key:'type',
                        title:'条件关系',
                        dataIndex:'type',
                    },
                    {
                        key:'right',
                        title:'右侧参数',
                        dataIndex: 'right',
                    },
                    {
                        key:'option',
                        title:'操作',
                        render: (text, record) => {
                            return (
                                <Space>
                                    <a>互换</a>
                                    <a>删除</a>
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