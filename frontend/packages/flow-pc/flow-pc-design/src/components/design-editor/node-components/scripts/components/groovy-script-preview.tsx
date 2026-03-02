import React from "react";

interface GroovyScriptPreviewProps {
    value: string;
    // 多行
    multiline?: boolean;
}

export const GroovyScriptPreview: React.FC<GroovyScriptPreviewProps> = (props) => {
    const value = props.value;

    const multiline = props.multiline || false;

    return (
        <>
            {multiline && (
                <div
                    style={{
                        minHeight: '45px',
                        padding: '4px 11px',
                        border: '1px solid #d9d9d9',
                        borderRadius: '6px 0 0 6px',
                        backgroundColor: value ? '#fff' : '#fafafa',
                        color: value ? 'rgba(0,0,0,0.88)' : 'rgba(0,0,0,0.25)',
                    }}
                >
                    {value}
                </div>
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
                >
                    {value}
                </div>
            )}
        </>

    )
}