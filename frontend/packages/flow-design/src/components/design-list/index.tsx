import {ActionType, Table, TableProps} from "@/components/table";
import React from "react";
import {DataType, DesignListProps} from "./types";
import {usePresenter} from "./hooks/use-presenter";
import {Button,Drawer,Space} from "antd";
import {DesignPanel} from "@/components/design-panel";

export const DesignList: React.FC<DesignListProps> = (props) => {

    const actionType = React.useRef<ActionType>(null);

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
    ]


    const { state, presenter } = usePresenter(actionType);

    return (
        <div>
            <Table<DataType>
                actionType={actionType}
                toolBarRender={()=>{
                    return [
                        <Button type={'primary'} onClick={()=>{
                            presenter.showEditable();
                        }}>创建流程</Button>
                    ]
                }}
                columns={columns}
                request={(request)=>{
                    return presenter.request(request);
                }}
            />

            <Drawer
                title={"流程设计面板"}
                open={state.editable}
                size={'100%'}
                closable={false}
                onClose={()=>{
                    presenter.closeEditable();
                }}
                extra={(
                    <Space>
                        <Button
                            type={'primary'}
                            onClick={()=>{
                                presenter.closeEditable();
                                presenter.reload();
                            }}
                        >保存</Button>
                        <Button
                            onClick={()=>{
                                presenter.closeEditable();
                            }}
                        >关闭</Button>
                    </Space>
                )}
            >
                <DesignPanel/>
            </Drawer>
        </div>
    )
}
