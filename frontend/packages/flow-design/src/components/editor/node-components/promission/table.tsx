import React from "react";
import {Tabs,Switch} from "antd";
import {Table} from "@/components/table";
import {PromissionManager} from "@/components/editor/node-components/promission/index";
import {useDesignContext} from "@/pages/design-panel/hooks/use-design-context";

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
            title: '字段',
            dataIndex: 'name',
            key: 'name',
        },
        {
            title: '只读',
            dataIndex: 'readable',
            key: 'readable',
            render: (_: any, record: any) => {
                return (
                    <Switch
                        size="small"
                        checked={record.readable}
                        onChange={(value) => {
                            promissionManager.changeReadable(code, record.id, value);
                        }}
                    />
                )
            }
        },
        {
            title: '可编辑',
            dataIndex: 'editable',
            key: 'editable',
            render: (_: any, record: any) => {
                return (
                    <Switch
                        size="small"
                        checked={record.editable}
                        onChange={(value) => {
                            promissionManager.changeEditable(code, record.id, value);
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
                        checked={record.hidden}
                        onChange={(value) => {
                            promissionManager.changeHidden(code, record.id, value);
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
    const promissionManager = new PromissionManager(props.value, props.onChange);
    const {state} = useDesignContext();
    const form = state.workflow.form;
    promissionManager.initFormPromission(form);

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