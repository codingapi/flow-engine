import React from "react";
import { Table as AntdTable, TableProps as AntdTableProps } from "antd";


export interface Result<T> {
    data:{
        total: number;
        list: T[];
    }
    success: boolean
}

export interface ActionType {
    reload(): void;
}

export interface ParamRequest {
    current: number;
    pageSize: number;
    [key: string]: any;
}

export interface TableProps<RecordType> extends AntdTableProps<RecordType> {
    actionType?: React.Ref<ActionType>;
    request?(params: ParamRequest): Promise<Result<any>>;
}

export function Table<RecordType extends object = any>(props: TableProps<RecordType>) {

    const defaultCurrent = 1;
    const defaultPageSize = 10;

    const [dataSource, setDataSource] = React.useState<any[]>([]);

    const [total, setTotal] = React.useState(0);
    const [current, setCurrent] = React.useState(1);
    const [pageSize, setPageSize] = React.useState(10);

    React.useImperativeHandle(props.actionType, () => {
        return {
            reload: () => {
                requestData({
                    current: defaultCurrent,
                    pageSize: defaultPageSize
                })
            }
        }
    }, [total, current, pageSize]);


    const requestData = (request: ParamRequest) => {
        props.request?.({
            current: current,
            pageSize: pageSize,
        }).then(res => {
            if (res.success) {
                setCurrent(request.current);
                setPageSize(request.pageSize);
                setTotal(res.data.total);
                setDataSource(res.data.list);
            }
        })
    };

    React.useEffect(() => {
        requestData({
            current: defaultCurrent,
            pageSize: defaultPageSize
        });
    }, []);

    return (
        <AntdTable<RecordType>
            dataSource={dataSource}
            pagination={
                {
                    pageSize: pageSize,
                    current: current,
                    total: total,
                    defaultCurrent: defaultCurrent,
                    defaultPageSize: defaultPageSize,
                    onChange: (pageSize, current) => {
                        requestData({
                            current,
                            pageSize
                        });
                    },
                    ...props.pagination
                }
            }
            {...props}
        />
    );
}