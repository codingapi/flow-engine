import { FlowNodeEntity } from "@flowgram.ai/fixed-layout-editor";
import { nanoid } from "nanoid";

export const generateNodeId = (n: FlowNodeEntity) => `${n.type || n.flowNodeType}_${nanoid()}`;
