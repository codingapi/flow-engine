package com.codingapi.flow.generator;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * 流程Id构建器上下文
 */
public class FlowIDGeneratorGatewayContext {

    @Getter
    private final static FlowIDGeneratorGatewayContext instance = new FlowIDGeneratorGatewayContext();

    private FlowIDGeneratorGatewayContext() {
    }

    @Setter
    private FlowIDGeneratorGateway flowIDGeneratorGateway = new FlowIDGeneratorGateway() {


        private final static RandomStringUtils randomString = RandomStringUtils.secure();

        private String generateId(int count){
            return randomString.nextAlphanumeric(count);
        }

        @Override
        public String generateWorkId() {
            return this.generateId(18);
        }

        @Override
        public String generateWorkCode() {
            return this.generateId(10);
        }

        @Override
        public String generateProcessId() {
            return this.generateId(18);
        }

        @Override
        public String generateParallelId() {
            return this.generateId(18);
        }

        @Override
        public String generateDelayTaskId() {
            return this.generateId(18);
        }

        @Override
        public String generateFormFieldId() {
            return this.generateId(10);
        }

        @Override
        public String generateMockKey() {
            return this.generateId(18);
        }

        @Override
        public String generateFlowScriptKey() {
            return this.generateId(18);
        }

        @Override
        public String generateActionId() {
            return this.generateId(10);
        }

        @Override
        public String generateViewCode() {
            return this.generateId(18);
        }

        @Override
        public String generateNodeId() {
            return this.generateId(10);
        }

        @Override
        public long generateRecordId() {
            return 0;
        }
    };


    public String generateWorkId() {
        return this.flowIDGeneratorGateway.generateWorkId();
    }

    public String generateWorkCode() {
        return flowIDGeneratorGateway.generateWorkCode();
    }

    public String generateProcessId() {
        return flowIDGeneratorGateway.generateProcessId();
    }

    public long generateRecordId() {
        return flowIDGeneratorGateway.generateRecordId();
    }

    public String generateParallelId() {
        return flowIDGeneratorGateway.generateParallelId();
    }

    public String generateActionId() {
        return flowIDGeneratorGateway.generateActionId();
    }

    public String generateViewCode() {
        return flowIDGeneratorGateway.generateViewCode();
    }

    public String generateNodeId() {
        return flowIDGeneratorGateway.generateNodeId();
    }

    public String generateDelayTaskId() {
        return flowIDGeneratorGateway.generateDelayTaskId();
    }

    public String generateFormFieldId() {
        return flowIDGeneratorGateway.generateFormFieldId();
    }

    public String generateMockKey() {
        return flowIDGeneratorGateway.generateMockKey();
    }

    public String generateFlowScriptKey() {
        return flowIDGeneratorGateway.generateFlowScriptKey();
    }
}
