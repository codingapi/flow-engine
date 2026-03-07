import {Col, ColorPicker, Input, Row, Space} from "antd";
import React from "react";
import {DisplayStyle} from "@flow-engine/flow-types";


interface ActionStyleProps {
    value?: string;
    onChange?: (value: string) => void;
}


export const ActionStyle: React.FC<ActionStyleProps> = (props) => {

    const [style, setStyle] = React.useState<DisplayStyle>(props.value ? JSON.parse(props.value) : {});

    React.useEffect(() => {
        props.onChange && props.onChange(JSON.stringify(style));
    }, [style]);

    return (
        <div>
            <Row gutter={[8, 8]}>
                <Col span={12}>
                    <Space.Compact>
                        <Space.Addon
                            style={{
                                width: '150px',
                            }}
                        >
                            <span>背景颜色</span>
                        </Space.Addon>
                        <ColorPicker
                            value={style.backgroundColor}
                            onChange={(value) => {
                                if (value) {
                                    setStyle(prevState => {
                                        return {
                                            ...prevState,
                                            backgroundColor: value.toHex()
                                        }
                                    })
                                }
                            }}
                        />
                    </Space.Compact>
                </Col>
                <Col span={12}>
                    <Space.Compact>
                        <Space.Addon
                            style={{
                                width: '150px',
                            }}
                        >
                            <span>边框颜色</span>
                        </Space.Addon>
                        <ColorPicker
                            value={style.borderColor}
                            onChange={(value) => {
                                if (value) {
                                    setStyle(prevState => {
                                        return {
                                            ...prevState,
                                            borderColor: value.toHex()
                                        }
                                    })
                                }
                            }}
                        />
                    </Space.Compact>
                </Col>
                <Col span={12}>
                    <Space.Compact>
                        <Space.Addon
                            style={{
                                width: '150px',
                            }}
                        >
                            <span>边框圆角</span>
                        </Space.Addon>
                        <Input
                            type={"number"}
                            style={{
                                width: '150px',
                            }}
                            placeholder={"请输入边框圆角"}
                            value={style.borderRadius}
                            onChange={(event) => {
                                setStyle(prevState => {
                                    return {
                                        ...prevState,
                                        borderRadius: event.target.value
                                    }
                                })
                            }}
                        />
                    </Space.Compact>
                </Col>
                <Col span={12}>
                    <Space.Compact>
                        <Space.Addon
                            style={{
                                width: '150px',
                            }}
                        >
                            <span>边框大小</span>
                        </Space.Addon>
                        <Input
                            style={{
                                width: '150px',
                            }}
                            type={"number"}
                            placeholder={"请输入边框大小"}
                            value={style.borderSize}
                            onChange={(event) => {
                                setStyle(prevState => {
                                    return {
                                        ...prevState,
                                        borderSize: event.target.value
                                    }
                                })
                            }}
                        />
                    </Space.Compact>
                </Col>
            </Row>

        </div>
    )
}