import {PresenterHooks} from "@flow-engine/flow-core";
import {Presenter} from "@/components/design-panel/presenter";
import {State} from "@/components/design-panel/types";
import {DesignPanelApiImpl} from "@/components/design-panel/model";

const initState: State = {}

export const usePresenter = () => {

    const {state, presenter} = PresenterHooks.create(Presenter, initState, new DesignPanelApiImpl());

    return {
        state,
        presenter
    }

}