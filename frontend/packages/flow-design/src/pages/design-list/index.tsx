import {ActionType, Table, TableProps} from "@/components/table";
import React from "react";
import {DataType, DesignListProps} from "./types";
import {usePresenter} from "./hooks/use-presenter";
import {Button} from "antd";
import {DesignPanel} from "@/pages/design-panel";

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
            dataIndex: 'workCode',
            title: '编码',
        },
        {
            dataIndex: 'title',
            title: '名称',
        },
        {
            dataIndex: 'createTime',
            title: '创建时间',
        }
    ];

    return (
        <div>
            <Table<DataType>
                actionType={actionType}
                toolBarRender={() => {
                    return [
                        <Button type={'primary'} onClick={() => {
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
                open={state.editable}
                onClose={() => {
                    presenter.hideEditable();
                }}/>
        </div>
    )
}
