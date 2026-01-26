import {PresenterHooks} from "@flow-engine/flow-core";
import {Presenter} from "@/components/design-panel/presenter";
import {DesignPanelProps, State} from "@/components/design-panel/types";
import {DesignPanelApiImpl} from "@/components/design-panel/model";

const initState: State = {
    open: false,
}

export const usePresenter = (props:DesignPanelProps) => {

    const {state, presenter} = PresenterHooks.create(Presenter, initState, new DesignPanelApiImpl());

    return {
        state,
        presenter
    }

}