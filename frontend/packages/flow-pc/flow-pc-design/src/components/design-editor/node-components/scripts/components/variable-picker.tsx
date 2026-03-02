import React, {useState} from 'react';
import {Button, Col, Empty, Input, Popover, Row} from "antd";
import {EditOutlined, SearchOutlined} from "@ant-design/icons";
import {GroovyVariableMapping} from "@/components/design-editor/typings/script";

export interface VariablePickerProps {
    /** 变量映射列表 */
    mappings: GroovyVariableMapping[];
    /** 选中变量回调 */
    onSelect: (mapping: GroovyVariableMapping) => void;
}


const VariablePickerContent: React.FC<VariablePickerProps> = (props) => {
    const [searchText, setSearchText] = useState('');

    const mappings = React.useMemo(() => {
        const groups = new Map<string, GroovyVariableMapping[]>();

        for (const mapping of props.mappings) {
            const existing = groups.get(mapping.tag) || [];
            existing.push(mapping);
            groups.set(mapping.tag, existing);
        }

        return groups;
    }, []);

    // 阻止事件冒泡，防止 Popover 关闭
    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        e.stopPropagation();
        setSearchText(e.target.value);
    };

    return (
        <div style={{maxHeight: 300, overflowY: 'auto', width: 400}}>
            <Input
                placeholder="搜索变量..."
                prefix={<SearchOutlined style={{color: '#999'}}/>}
                value={searchText}
                onChange={handleInputChange}
                allowClear
                style={{marginBottom: 8}}
            />

            {mappings.size === 0 && (
                <Empty description="未找到匹配的变量" image={Empty.PRESENTED_IMAGE_SIMPLE}/>
            )}

            {Array.from(mappings.entries()).map(([tag, group]) => {
                return (
                    <Row
                        gutter={[8, 8]} style={{
                        width: '100%',
                    }}
                        key={tag}>
                        <Col span={24} style={{fontWeight: 'bold', color: '#1890ff'}}>
                            <span>{tag}</span>
                        </Col>
                        {group.map((mapping, index) => {
                            return (
                                <Col
                                    span={12}
                                    key={index}
                                    style={{
                                        padding: '8px',
                                        border: '1px solid #f0f0f0',
                                        borderRadius: 4,
                                        cursor: 'pointer',
                                        backgroundColor: '#fff',
                                    }}
                                    onClick={()=>{
                                        props.onSelect(mapping);
                                    }}
                                >
                                    <div>{mapping.label}</div>
                                    <div>{mapping.value}</div>
                                </Col>
                            )
                        })}
                    </Row>
                )
            })}

        </div>
    )
}

/**
 * 变量选择器组件
 * 用于在脚本表达式中插入变量
 */
export const VariablePicker: React.FC<VariablePickerProps> = (props) => {

    const [visible, setVisible] = React.useState(false);

    return (
        <Popover
            content={
                (
                    <VariablePickerContent {...props}/>
                )
            }
            open={visible}
            destroyOnHidden={true}
            onOpenChange={(visible) => {
                setVisible(visible);
            }}
            trigger="click"
            placement="bottom"
            getPopupContainer={(trigger) => trigger.parentElement || document.body}
        >
            <Button
                icon={<EditOutlined/>}
                onClick={() => {
                    setVisible(true);
                }}
            >
                插入变量
            </Button>
        </Popover>
    );
};
