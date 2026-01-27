import React from "react";
import {Provider} from "react-redux";
import {DesignPanelContext} from "../context";
import {createContext} from "../hooks/use-context";
import {designStore} from "../store";
import {Header} from "./header";
import {Footer} from "./footer";
import {Body} from "./body";
import {DesignPanelProps} from "../types";


const ContentScope: React.FC<DesignPanelProps> = (props) => {
    const {context} = createContext(props);
    return (
        <DesignPanelContext.Provider value={context}>
            <Header/>
            <Body/>
            <Footer/>
        </DesignPanelContext.Provider>
    )
}

export const Content: React.FC<DesignPanelProps> = (props) => {
    return (
        <>
            <Provider store={designStore}>
                <ContentScope {...props}/>
            </Provider>
        </>
    )
}