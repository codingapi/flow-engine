import React from "react";
import {Button, Col, Popover, Row} from "antd";
import {Icon} from "@flow-engine/flow-pc-ui";
import {icons} from "@flow-engine/flow-types";

interface ActionIconProps {
    value?: string;
    onChange?: (value: string) => void;
}


export const ActionIcon: React.FC<ActionIconProps> = (props) => {
    const { value, onChange } = props;

    const options = React.useMemo(() => {
        return icons.groups;
    }, []);

    const handleSelectIcon = (iconType: string) => {
        onChange?.(iconType);
    };

    const content = () => {
        return (
            <div
                style={{
                    maxHeight: 400,
                    width: 500,
                    overflowY: 'auto',  // 添加滚动
                    padding: '4px'
                }}
            >
                <Row gutter={[12, 12]}>
                    {options.map((group) => (
                        <React.Fragment key={group.label}>
                            <Col span={24}>
                                <div style={{
                                    fontWeight: 500,
                                    marginBottom: 8,
                                    color: '#666',
                                    fontSize: 13
                                }}>
                                    {group.label}
                                </div>
                            </Col>

                            {group.icons.map((iconType) => (
                                <Col span={4} key={iconType}>
                                    <div
                                        onClick={() => handleSelectIcon(iconType)}
                                        style={{
                                            padding: '12px 8px',
                                            borderRadius: 6,
                                            cursor: 'pointer',
                                            display: 'flex',
                                            justifyContent: 'center',
                                            alignItems: 'center',
                                            backgroundColor: value === iconType ? '#e6f7ff' : 'transparent',
                                            border: value === iconType ? '1px solid #1890ff' : '1px solid transparent',
                                            transition: 'all 0.3s'
                                        }}
                                        onMouseEnter={(e) => {
                                            if (value !== iconType) {
                                                e.currentTarget.style.backgroundColor = '#f5f5f5';
                                                e.currentTarget.style.borderColor = '#d9d9d9';
                                            }
                                        }}
                                        onMouseLeave={(e) => {
                                            if (value !== iconType) {
                                                e.currentTarget.style.backgroundColor = 'transparent';
                                                e.currentTarget.style.borderColor = 'transparent';
                                            }
                                        }}
                                    >
                                        <Icon type={iconType} style={{ fontSize: 24 }} />
                                    </div>
                                </Col>
                            ))}
                        </React.Fragment>
                    ))}
                </Row>
            </div>
        );
    };

    return (
        <Popover
            content={content}
            title="选择图标"
            trigger="click"
            placement="bottomLeft"
        >
            <Button>
                {value ? (
                    <>
                        <Icon type={value} style={{ marginRight: 8 }} />
                        更换图标
                    </>
                ) : (
                    '选择图标'
                )}
            </Button>
        </Popover>
    );
};