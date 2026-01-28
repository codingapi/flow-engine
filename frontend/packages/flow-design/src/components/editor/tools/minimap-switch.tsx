import {InsertRowBelowOutlined} from "@ant-design/icons";
import {Button, Tooltip} from "antd";

export const MinimapSwitch = (props: {
    minimapVisible: boolean;
    setMinimapVisible: (visible: boolean) => void;
}) => {
    const {minimapVisible, setMinimapVisible} = props;

    return (
        <Tooltip title="Minimap">
            <Button
                type="text"
                icon={
                    <InsertRowBelowOutlined
                        style={{
                            color: minimapVisible ? undefined : '#060709cc',
                        }}
                    />
                }
                onClick={() => {
                    setMinimapVisible(Boolean(!minimapVisible));
                }}
            />
        </Tooltip>
    );
};
