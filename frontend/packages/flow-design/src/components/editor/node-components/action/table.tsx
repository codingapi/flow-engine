import React, {useContext} from "react";
import {Space, Switch} from "antd";
import {Table} from "@/components/table";
import {NodeRenderContext} from "@/components/editor/context";
import {ActionManager} from "@/components/editor/node-components/action/index";

interface ActionTableProps {
    value: any[];
    onChange: (data: any[]) => void;
}

export const ActionTable: React.FC<ActionTableProps> = (props) => {
    const {node} = useContext(NodeRenderContext);
    const actions = node.getNodeRegistry()?.meta.actions || [];
    const actionManager = new ActionManager(props.value,props.onChange);
    const datasource = actionManager.getDatasource(actions);
    const columns = React.useCallback(() => {
        return [
            {
                title: 'id',
                dataIndex: 'id',
                key: 'id',
                hidden: true,
            },
            {
                title: '标题',
                dataIndex: 'title',
                key: 'title',
            },
            {
                title: '启用',
                dataIndex: 'enable',
                key: 'enable',
                render: (_: any, record: any) => {
                    return (
                        <Switch
                            size="small"
                            value={record.enable}
                            onChange={(value) => {
                                actionManager.enable(record.id, value);
                            }}
                        />
                    )
                }
            },
            {
                title: '操作',
                dataIndex: 'operation',
                key: 'operation',
                render: (_: any, record: any) => {
                    return (
                        <Space>
                            <a>
                                编辑
                            </a>
                        </Space>
                    )
                }
            },
        ];
    }, [props.value]);

    return (
        <Table
            columns={columns()}
            dataSource={datasource}
            style={{
                width: "100%",
            }}
            pagination={false}
        />
    )
}