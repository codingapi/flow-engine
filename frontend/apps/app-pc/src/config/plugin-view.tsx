import {ViewPlugin} from "@flow-engine/flow-types";
import {LeaveView} from "@/views/leave.tsx";

ViewPlugin.getInstance().register('default',LeaveView);