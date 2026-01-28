import {usePlaygroundTools} from '@flowgram.ai/fixed-layout-editor';
import {RetweetOutlined} from "@ant-design/icons";
import {Button, Tooltip} from "antd";

export const SwitchVertical = () => {
    const tools = usePlaygroundTools();
    return (
        <Tooltip title={!tools.isVertical ? 'Vertical Layout' : 'Horizontal Layout'}>
            <Button
                type="text"
                onClick={() => tools.changeLayout()}
                icon={
                    <RetweetOutlined
                       size={10}
                        style={{
                            transform: !tools.isVertical ? '' : 'rotate(90deg)',
                            transition: 'transform .3s ease',
                        }}
                    />
                }
            />
        </Tooltip>
    );
};
