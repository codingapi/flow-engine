import {PlusCircleOutlined} from "@ant-design/icons";
import {Dropdown, MenuProps} from "antd";
import React from "react";

/**
 * TODO 尚未完成
 * @constructor
 */
export const ConditionRelation = () => {

    const items: MenuProps['items'] = [
        {
            key: '1',
            label: (
                <a>
                    括号
                </a>
            ),
        },
        {
            key: '2',
            label: (
                <a>
                    并且
                </a>
            ),
        },
        {
            key: '3',
            label: (
                <a>
                    或者
                </a>
            ),
        },
        {
            key: '4',
            label: '条件',
            children: [
                {
                    key: '3-1',
                    label: '分组1',
                },
                {
                    key: '3-2',
                    label: '分组2',
                },
            ],
        },
        {
            key: '5',
            label: (
                <a>
                    删除
                </a>
            ),
            danger: true
        },
    ];

    return (
        <div style={{
            padding: '4px 11px',
            border: '1px solid #d9d9d9',
            borderRadius: '6px',
            backgroundColor: '#fff',
            color: 'rgba(0,0,0,0.88)',
            minHeight: '60px',
        }}>


            <Dropdown menu={{items}}>
                <PlusCircleOutlined
                    style={{
                        cursor: 'pointer',
                    }}
                />
            </Dropdown>

        </div>
    )
}