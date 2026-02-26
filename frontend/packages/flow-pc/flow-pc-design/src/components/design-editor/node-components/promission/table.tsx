import React from "react";
import {Tabs,Switch} from "antd";
import {Table} from "@flow-engine/flow-pc-ui";
import {PromissionManager} from "@/components/design-editor/node-components/promission/index";
import {useDesignContext} from "@/components/design-panel/hooks/use-design-context";

interface PromissionTableProps {
    value: any;
    onChange: (value: any) => void;
}

interface FormPromissionTableProps {
    title: string;
    code: string;
    promissionManager:PromissionManager;
}

const FormPromissionTable: React.FC<FormPromissionTableProps> = (props) => {
    const code = props.code;
    const promissionManager = props.promissionManager;
    const datasource = promissionManager.getDatasource(code);

    const columns = [
        {
            title: 'id',
            dataIndex: 'id',
            key: 'id',
            hidden: true,
        },
        {
            title: '名称',
            dataIndex: 'fieldName',
            key: 'fieldName',
        },
        {
            title: '编码',
            dataIndex: 'fieldCode',
            key: 'fieldCode',
        },
        {
            title: '可编辑',
            dataIndex: 'editable',
            key: 'editable',
            render: (_: any, record: any) => {
                return (
                    <Switch
                        size="small"
                        checked={record.type==='WRITE'}
                        onChange={(value) => {
                            promissionManager.changeEditable(code, record.fieldCode, value);
                        }}
                    />
                )
            }
        },
        {
            title: '只读',
            dataIndex: 'readable',
            key: 'readable',
            render: (_: any, record: any) => {
                return (
                    <Switch
                        size="small"
                        checked={record.type==='READ'}
                        onChange={(value) => {
                            promissionManager.changeReadable(code, record.fieldCode, value);
                        }}
                    />
                )
            }
        },
        {
            title: '隐藏',
            dataIndex: 'hidden',
            key: 'hidden',
            render: (_: any, record: any) => {
                return (
                    <Switch
                        size="small"
                        checked={record.type==='HIDDEN'}
                        onChange={(value) => {
                            promissionManager.changeHidden(code, record.fieldCode, value);
                        }}
                    />
                )
            }
        },
    ];

    return (
        <Table
            title={() => {
                return `表单：${props.title || '未选择表单'}`;
            }}
            rowKey={"id"}
            columns={columns}
            dataSource={datasource}
            style={{
                width: "100%",
            }}
            pagination={false}
        />
    );
}

export const PromissionTable: React.FC<PromissionTableProps> = (props) => {
    const {state} = useDesignContext();
    const form = state.workflow.form;
    const promissionManager = new PromissionManager(form,props.value, props.onChange);
    promissionManager.initFormPromission();

    const formList = form.subForms || [];

    const items = formList.map((item) => {
        return {
            key: item.name,
            label: `子表单：${item.name}`,
            children: (
                <FormPromissionTable
                    title={item.name}
                    code={item.code}
                    promissionManager={promissionManager}
                />
            )
        }
    });

    return (
        <>
            <FormPromissionTable
                title={form?.name || '主表单'}
                code={form?.code || ''}
                promissionManager={promissionManager}
            />

            {items && items.length > 0 && (
                <Tabs
                    items={items}
                    style={{
                        width: "100%",
                    }}
                    centered={true}
                />
            )}
        </>
    )
}