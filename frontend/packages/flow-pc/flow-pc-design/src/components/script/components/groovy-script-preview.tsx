import React from "react";
import {GroovyScriptConvertorUtil} from "@flow-engine/flow-core";

interface GroovyScriptPreviewProps {
    script: string;
    // 多行
    multiline?: boolean;
}

export const GroovyScriptPreview: React.FC<GroovyScriptPreviewProps> = (props) => {

    const multiline = props.multiline || false;

    const value = GroovyScriptConvertorUtil.getScriptTitle(props.script);

    return (
        <>
            {multiline && (
                <div
                    style={{
                        minHeight: '45px',
                        padding: '4px 11px',
                        border: '1px solid #d9d9d9',
                        borderRadius: '6px',
                        backgroundColor: value ? '#fff' : '#fafafa',
                        color: value ? 'rgba(0,0,0,0.88)' : 'rgba(0,0,0,0.25)',
                    }}
                    dangerouslySetInnerHTML={{__html: value}}
                />
            )}

            {!multiline && (
                <div
                    style={{
                        flex: 1,
                        padding: '4px 11px',
                        backgroundColor: value ? '#fff' : '#fafafa',
                        border: '1px solid #d9d9d9',
                        borderRadius: '6px 0 0 6px',
                        color: value ? 'rgba(0,0,0,0.88)' : 'rgba(0,0,0,0.25)',
                        whiteSpace: 'nowrap',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                    }}
                    dangerouslySetInnerHTML={{__html: value}}
                />
            )}
        </>

    )
}