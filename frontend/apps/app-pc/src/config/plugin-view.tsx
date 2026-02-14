import {ViewPlugin} from "@flow-engine/flow-design";
import {LeaveView} from "@/views/leave.tsx";

ViewPlugin.getInstance().register('default',LeaveView);