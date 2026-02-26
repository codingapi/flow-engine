import {ViewBindPlugin} from "@flow-engine/flow-types";
import {LeaveView} from "@/views/leave.tsx";

ViewBindPlugin.getInstance().register('default',LeaveView);