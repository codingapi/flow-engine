import React from "react";
import {VersionPresenter} from "@/components/design-editor/version/presenter";
import {VersionApiImpl} from "@/components/design-editor/version/model";
import {useDesignContext} from "@/components/design-panel/hooks/use-design-context";


export const useVersionPresenter = () => {

    const {state} = useDesignContext();
    const workId = state.workflow.id;
    const ref = React.useRef<VersionPresenter>(undefined);

    const [versions, setVersions] = React.useState<any[]>([]);

    if (!ref.current) {
        ref.current = new VersionPresenter(workId,versions, setVersions, new VersionApiImpl());
    }

    React.useEffect(() => {
        ref.current?.syncState(versions);
    }, [versions]);

    React.useEffect(() => {
        ref.current?.initState();
    }, []);

    return {
        state: versions,
        presenter: ref.current,
    };
}