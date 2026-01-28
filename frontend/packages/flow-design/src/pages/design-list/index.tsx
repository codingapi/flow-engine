import {ActionType, Table, TableProps} from "@/components/table";
import React from "react";
import {DataType, DesignListProps} from "./types";
import {usePresenter} from "./hooks/use-presenter";
import {Button, Space, Popconfirm, message} from "antd";
import {DesignPanel} from "@/pages/design-panel";
import dayjs from "dayjs";

export const DesignList: React.FC<DesignListProps> = (props) => {

    const actionType = React.useRef<ActionType>(null);
    const {state, presenter} = usePresenter(actionType);
    const columns: TableProps<DataType>['columns'] = [
        {
            dataIndex: 'id',
            title: '编号',
            hidden: true,
        },
        {
            dataIndex: 'code',
            title: '编码',
        },
        {
            dataIndex: 'title',
            title: '名称',
        },
        {
            dataIndex: 'createdTime',
            title: '创建时间',
            render: (value, record) => {
                return dayjs(value).format("YYYY-MM-DD HH:mm:ss");
            }
        },
        {
            dataIndex: 'option',
            title: '操作',
            render: (value, record) => {
                return (
                    <Space>
                        <a onClick={() => {
                            presenter.editCurrent(record.id);
                        }}>编辑</a>
                        <Popconfirm
                            title={"确认要删除该流程吗？"}
                            onConfirm={() => {
                                presenter.deleteRecord(record.id).then(() => {
                                    message.success('流程已删除.')
                                });
                            }}
                        >
                            <a>删除</a>
                        </Popconfirm>
                    </Space>
                )
            }
        }
    ];

    return (
        <div>
            <Table<DataType>
                rowKey={"id"}
                actionType={actionType}
                toolBarRender={() => {
                    return [
                        <Button
                            key={"create"}
                            type={'primary'}
                            onClick={() => {
                                presenter.clearCurrent();
                                presenter.showEditable();
                            }}>创建流程</Button>
                    ]
                }}
                columns={columns}
                request={(request) => {
                    return presenter.request(request);
                }}
            />

            <DesignPanel
                id={state.currentId}
                open={state.editable}
                onClose={() => {
                    presenter.hideEditable();
                    presenter.reload();
                }}/>
        </div>
    )
}
