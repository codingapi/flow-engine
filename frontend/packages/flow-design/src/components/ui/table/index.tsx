import React from "react";
import { Table as AntdTable, TableProps as AntdTableProps,Flex,Space,Card } from "antd";


export interface Result<T> {
    data:T[]
    total: number;
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
    toolBarRender?():React.ReactElement[]
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
                setTotal(res.total);
                setDataSource(res.data);
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
        <div style={{
            width: '100%'
        }}>
           <Card>
               <Flex justify={"end"} style={{
                   paddingLeft: 20,
                   marginBottom:10
               }}>
                   <Space>
                       {props.toolBarRender && props.toolBarRender()}
                   </Space>
               </Flex>
               <AntdTable<RecordType>
                   dataSource={dataSource}
                   pagination={
                       {
                           pageSize: pageSize,
                           current: current,
                           total: total,
                           showSizeChanger: true,
                           showQuickJumper: true,
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
           </Card>
        </div>
    );
}