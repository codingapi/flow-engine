import React from 'react';
import {usePlayground, useRefresh} from '@flowgram.ai/fixed-layout-editor';
import {Button,Tooltip} from "antd";
import {LockOutlined, UnlockOutlined} from "@ant-design/icons";

export const Readonly = () => {
    const playground = usePlayground();
    const refresh = useRefresh();

    React.useEffect(() => {
        const disposable = playground.config.onReadonlyOrDisabledChange(() => refresh());
        return () => disposable.dispose();
    }, [playground]);

    const toggleReadonly = React.useCallback(() => {
        playground.config.readonly = !playground.config.readonly;
    }, [playground]);

    return (
        <Tooltip title={playground.config.readonly?"Unlock":"Lock"}>
            {playground.config.readonly && <Button type="text" icon={<LockOutlined/>} onClick={toggleReadonly}/>}
            {!playground.config.readonly &&  <Button type="text" icon={<UnlockOutlined/>} onClick={toggleReadonly}/>}
        </Tooltip>
    )
};
