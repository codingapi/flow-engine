import React from "react";

interface FlowTitleProps {
    title: string;
}


export const FlowTitle:React.FC<FlowTitleProps> = (props) => {

    return (
        <div
            dangerouslySetInnerHTML={{__html: props.title}}
        />
    )
}