import { Table, TableProps } from "@/components/table";
import React from "react";
import { DataType, DesignListProps } from "./types";
import { usePresenter } from "./hooks/use-presenter";

export const DesignList: React.FC<DesignListProps> = (props) => {

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


    const { state, presenter } = usePresenter();

    return (
        <div>
            <Table<DataType>
                columns={columns}
                request={(request)=>{
                    return presenter.request(request);
                }}
            />
        </div>
    )
}
