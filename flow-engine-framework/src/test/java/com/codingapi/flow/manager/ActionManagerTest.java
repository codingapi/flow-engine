package com.codingapi.flow.manager;

import com.codingapi.flow.action.ActionDisplay;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActionManagerTest {

    @Test
    void should_only_return_enabled_actions_that_are_allowed_to_display() {
        IFlowAction enabledAction = new TestAction("enabled", true, true);
        IFlowAction disabledAction = new TestAction("disabled", false, true);
        IFlowAction hiddenAction = new TestAction("hidden", true, false);

        ActionManager actionManager = new ActionManager(List.of(enabledAction, disabledAction, hiddenAction));

        List<IFlowAction> actions = actionManager.filterActions(null);

        assertEquals(List.of(enabledAction), actions);
    }

    private record TestAction(String id, boolean enable, boolean displayable) implements IFlowAction {

        @Override
        public String type() {
            return "TEST";
        }

        @Override
        public String title() {
            return id;
        }

        @Override
        public ActionDisplay display() {
            return null;
        }

        @Override
        public boolean show(FlowSession flowSession) {
            return displayable;
        }

        @Override
        public List<FlowRecord> generateRecords(FlowSession flowSession) {
            return List.of();
        }

        @Override
        public void run(FlowSession flowSession) {
        }

        @Override
        public Map<String, Object> toMap() {
            return Map.of();
        }

        @Override
        public void copy(IFlowAction source) {
        }
    }
}
