import {describe, test} from '@rstest/core';
import {WorkflowConvertor} from "@/pages/design-panel/presenters/convertor";
import {WorkflowEdgeConvertor} from "@/pages/design-panel/presenters/convertor/edge";

describe.sequential('WorkflowConvert', () => {

    test('toApi', () => {
        const workflow = {
            nodes: [
                {
                    "id": "start",
                    "type": "START",
                    "data": {
                        "title": "开始节点",
                        "order": "0",
                        "actions": [
                            {
                                "enable": true,
                                "display": {
                                    "title": "通过"
                                },
                                "id": "EJ1crTZHbrlS9j0o9G",
                                "type": "PASS",
                                "title": "通过"
                            },
                            {
                                "enable": true,
                                "display": {
                                    "title": "保存"
                                },
                                "id": "E9b19t5vLh2w9ExBvV",
                                "type": "SAVE",
                                "title": "保存"
                            },
                            {
                                "enable": true,
                                "display": {
                                    "title": "自定义"
                                },
                                "id": "TAunDeC59jMdJnm26z",
                                "type": "CUSTOM",
                                "title": "自定义",
                                "script": "def run(session){return 'PASS'}"
                            }
                        ],
                        "NodeTitleStrategy": {
                            "script": "def run(request){return '你有一条待办'}",
                            "strategyType": "NodeTitleStrategy"
                        },
                        "FormFieldPermissionStrategy": {
                            "strategyType": "FormFieldPermissionStrategy",
                            "fieldPermissions": []
                        },
                        "RevokeStrategy": {
                            "enable": true,
                            "type": "REVOKE_CURRENT",
                            "strategyType": "RevokeStrategy"
                        }
                    },
                    "blocks": []
                },
                {
                    "id": "approval1",
                    "type": "APPROVAL",
                    "data": {
                        "title": "审批节点",
                        "order": "0",
                        "actions": [
                            {
                                "enable": true,
                                "display": {
                                    "icon": null,
                                    "style": null,
                                    "title": "通过"
                                },
                                "id": "wKOE3waOh2G23dbQZ1",
                                "type": "PASS",
                                "title": "通过"
                            },
                            {
                                "enable": true,
                                "display": {
                                    "icon": null,
                                    "style": null,
                                    "title": "拒绝"
                                },
                                "id": "U65weDHfcTaMPmHHno",
                                "type": "REJECT",
                                "title": "拒绝",
                                "script": "def run(session){return new com.codingapi.flow.script.action.RejectActionScript.RejectResult(session.getStartNode().getId())}"
                            },
                            {
                                "enable": true,
                                "display": {
                                    "icon": null,
                                    "style": null,
                                    "title": "保存"
                                },
                                "id": "QAa8h5TFcZvhVINFPe",
                                "type": "SAVE",
                                "title": "保存"
                            },
                            {
                                "enable": true,
                                "display": {
                                    "icon": null,
                                    "style": null,
                                    "title": "加签"
                                },
                                "id": "PLweONmmnwYUBHPUoz",
                                "type": "ADD_AUDIT",
                                "title": "加签"
                            },
                            {
                                "enable": true,
                                "display": {
                                    "icon": null,
                                    "style": null,
                                    "title": "转办"
                                },
                                "id": "jEOGBl2FjScqGSXFXe",
                                "type": "TRANSFER",
                                "title": "转办"
                            },
                            {
                                "enable": true,
                                "display": {
                                    "icon": null,
                                    "style": null,
                                    "title": "退回"
                                },
                                "id": "uNZwsZoBW3O8g37Ei0",
                                "type": "RETURN",
                                "title": "退回"
                            },
                            {
                                "enable": true,
                                "display": {
                                    "icon": null,
                                    "style": null,
                                    "title": "委派"
                                },
                                "id": "fXUMgnCQHPYdkiJ5gT",
                                "type": "DELEGATE",
                                "title": "委派"
                            }
                        ],
                        "TimeoutStrategy": {
                            "timeoutTime": "86400000",
                            "type": "REMIND",
                            "strategyType": "TimeoutStrategy"
                        },
                        "MultiOperatorAuditStrategy": {
                            "type": "SEQUENCE",
                            "percent": "0.0",
                            "strategyType": "MultiOperatorAuditStrategy"
                        },
                        "SameOperatorAuditStrategy": {
                            "type": "AUTO_PASS",
                            "strategyType": "SameOperatorAuditStrategy"
                        },
                        "RecordMergeStrategy": {
                            "enable": false,
                            "strategyType": "RecordMergeStrategy"
                        },
                        "ResubmitStrategy": {
                            "type": "RESUME",
                            "strategyType": "ResubmitStrategy"
                        },
                        "AdviceStrategy": {
                            "signable": false,
                            "adviceNullable": true,
                            "strategyType": "AdviceStrategy"
                        },
                        "ErrorTriggerStrategy": {
                            "script": "def run(request){ return $bind.createErrorThrow(request.getStartNode()); }",
                            "strategyType": "ErrorTriggerStrategy"
                        },
                        "NodeTitleStrategy": {
                            "script": "def run(request){return '你有一条待办'}",
                            "strategyType": "NodeTitleStrategy"
                        },
                        "FormFieldPermissionStrategy": {
                            "strategyType": "FormFieldPermissionStrategy",
                            "fieldPermissions": []
                        },
                        "OperatorLoadStrategy": {
                            "script": "def run(request){return [request.getCreatedOperator()]}",
                            "strategyType": "OperatorLoadStrategy"
                        },
                        "RevokeStrategy": {
                            "enable": true,
                            "type": "REVOKE_CURRENT",
                            "strategyType": "RevokeStrategy"
                        }
                    },
                    "blocks": []
                },
                {
                    "id": "1condition",
                    "type": "CONDITION",
                    "data": {
                        "title": "条件控制",
                        "order": 1
                    },
                    "blocks": [
                        {
                            "id": "1condition1",
                            "type": "CONDITION_BRANCH",
                            "data": {
                                "title": "条件节点节点",
                                "order": "0",
                                "actions": [],
                                "script": "def run(session){return true}"
                            },
                            "blocks": [
                                {
                                    "id": "approval2",
                                    "type": "APPROVAL",
                                    "data": {
                                        "title": "审批节点",
                                        "order": "0",
                                        "actions": [
                                            {
                                                "enable": true,
                                                "display": {
                                                    "icon": null,
                                                    "style": null,
                                                    "title": "通过"
                                                },
                                                "id": "jFclrQY7BXnufqrRVi",
                                                "type": "PASS",
                                                "title": "通过"
                                            },
                                            {
                                                "enable": true,
                                                "display": {
                                                    "icon": null,
                                                    "style": null,
                                                    "title": "拒绝"
                                                },
                                                "id": "M381NXjHxpHRSovY6M",
                                                "type": "REJECT",
                                                "title": "拒绝",
                                                "script": "def run(session){return new com.codingapi.flow.script.action.RejectActionScript.RejectResult(session.getStartNode().getId())}"
                                            },
                                            {
                                                "enable": true,
                                                "display": {
                                                    "icon": null,
                                                    "style": null,
                                                    "title": "保存"
                                                },
                                                "id": "md0ILpIsXfyxeAmwzK",
                                                "type": "SAVE",
                                                "title": "保存"
                                            },
                                            {
                                                "enable": true,
                                                "display": {
                                                    "icon": null,
                                                    "style": null,
                                                    "title": "加签"
                                                },
                                                "id": "LReGubxiOmVWdR1tlX",
                                                "type": "ADD_AUDIT",
                                                "title": "加签"
                                            },
                                            {
                                                "enable": true,
                                                "display": {
                                                    "icon": null,
                                                    "style": null,
                                                    "title": "转办"
                                                },
                                                "id": "BF9RWHJPT7X9kcyRNy",
                                                "type": "TRANSFER",
                                                "title": "转办"
                                            },
                                            {
                                                "enable": true,
                                                "display": {
                                                    "icon": null,
                                                    "style": null,
                                                    "title": "退回"
                                                },
                                                "id": "s8BstOlYnFHyqYKkBo",
                                                "type": "RETURN",
                                                "title": "退回"
                                            },
                                            {
                                                "enable": true,
                                                "display": {
                                                    "icon": null,
                                                    "style": null,
                                                    "title": "委派"
                                                },
                                                "id": "1sFv6SEp8BlZZ0BQwd",
                                                "type": "DELEGATE",
                                                "title": "委派"
                                            }
                                        ],
                                        "TimeoutStrategy": {
                                            "timeoutTime": "86400000",
                                            "type": "REMIND",
                                            "strategyType": "TimeoutStrategy"
                                        },
                                        "MultiOperatorAuditStrategy": {
                                            "type": "SEQUENCE",
                                            "percent": "0.0",
                                            "strategyType": "MultiOperatorAuditStrategy"
                                        },
                                        "SameOperatorAuditStrategy": {
                                            "type": "AUTO_PASS",
                                            "strategyType": "SameOperatorAuditStrategy"
                                        },
                                        "RecordMergeStrategy": {
                                            "enable": false,
                                            "strategyType": "RecordMergeStrategy"
                                        },
                                        "ResubmitStrategy": {
                                            "type": "RESUME",
                                            "strategyType": "ResubmitStrategy"
                                        },
                                        "AdviceStrategy": {
                                            "signable": false,
                                            "adviceNullable": true,
                                            "strategyType": "AdviceStrategy"
                                        },
                                        "ErrorTriggerStrategy": {
                                            "script": "def run(request){ return $bind.createErrorThrow(request.getStartNode()); }",
                                            "strategyType": "ErrorTriggerStrategy"
                                        },
                                        "NodeTitleStrategy": {
                                            "script": "def run(request){return '你有一条待办'}",
                                            "strategyType": "NodeTitleStrategy"
                                        },
                                        "FormFieldPermissionStrategy": {
                                            "strategyType": "FormFieldPermissionStrategy",
                                            "fieldPermissions": []
                                        },
                                        "OperatorLoadStrategy": {
                                            "script": "def run(request){return [request.getCreatedOperator()]}",
                                            "strategyType": "OperatorLoadStrategy"
                                        },
                                        "RevokeStrategy": {
                                            "enable": true,
                                            "type": "REVOKE_CURRENT",
                                            "strategyType": "RevokeStrategy"
                                        }
                                    },
                                    "blocks": []
                                },
                                {
                                    "id": "2condition",
                                    "type": "CONDITION",
                                    "data": {
                                        "title": "条件控制",
                                        "order": 1
                                    },
                                    "blocks": [
                                        {
                                            "id": "2condition1",
                                            "type": "CONDITION_BRANCH",
                                            "data": {
                                                "title": "条件节点节点",
                                                "order": "0",
                                                "actions": [],
                                                "script": "def run(session){return true}"
                                            },
                                            "blocks": []
                                        },
                                        {
                                            "id": "2condition2",
                                            "type": "CONDITION_BRANCH",
                                            "data": {
                                                "title": "条件节点节点",
                                                "order": "0",
                                                "actions": [],
                                                "script": "def run(session){return true}"
                                            },
                                            "blocks": []
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            "id": "1condition2",
                            "type": "CONDITION_BRANCH",
                            "data": {
                                "title": "条件节点节点",
                                "order": "0",
                                "actions": [],
                                "script": "def run(session){return true}"
                            },
                            "blocks": []
                        }
                    ]
                },
                {
                    "id": "end",
                    "type": "END",
                    "data": {
                        "title": "结束节点",
                        "order": "0",
                        "actions": []
                    },
                    "blocks": []
                }
            ]
        }

        const workflowConvertor = new WorkflowConvertor(workflow as any);

        // const apiData = workflowConvertor.toApi();
        // console.log(apiData.nodes.map(item=>item.id));
        // console.log(apiData.edges);

        // console.log(JSON.stringify(apiData));

    });


    test('toEdge', () => {
        const workflow = {
            nodes: [
                {
                    "id": "start",
                    "type": "START",
                    "data": {
                        "title": "开始节点",
                        "order": "0",
                        "actions": [
                            {
                                "enable": true,
                                "display": {
                                    "title": "通过"
                                },
                                "id": "EJ1crTZHbrlS9j0o9G",
                                "type": "PASS",
                                "title": "通过"
                            },
                            {
                                "enable": true,
                                "display": {
                                    "title": "保存"
                                },
                                "id": "E9b19t5vLh2w9ExBvV",
                                "type": "SAVE",
                                "title": "保存"
                            },
                            {
                                "enable": true,
                                "display": {
                                    "title": "自定义"
                                },
                                "id": "TAunDeC59jMdJnm26z",
                                "type": "CUSTOM",
                                "title": "自定义",
                                "script": "def run(session){return 'PASS'}"
                            }
                        ],
                        "NodeTitleStrategy": {
                            "script": "def run(request){return '你有一条待办'}",
                            "strategyType": "NodeTitleStrategy"
                        },
                        "FormFieldPermissionStrategy": {
                            "strategyType": "FormFieldPermissionStrategy",
                            "fieldPermissions": []
                        },
                        "RevokeStrategy": {
                            "enable": true,
                            "type": "REVOKE_CURRENT",
                            "strategyType": "RevokeStrategy"
                        }
                    },
                    "blocks": []
                },
                {
                    "id": "approval1",
                    "type": "APPROVAL",
                    "data": {
                        "title": "审批节点",
                        "order": "0",
                        "actions": [
                            {
                                "enable": true,
                                "display": {
                                    "icon": null,
                                    "style": null,
                                    "title": "通过"
                                },
                                "id": "wKOE3waOh2G23dbQZ1",
                                "type": "PASS",
                                "title": "通过"
                            },
                            {
                                "enable": true,
                                "display": {
                                    "icon": null,
                                    "style": null,
                                    "title": "拒绝"
                                },
                                "id": "U65weDHfcTaMPmHHno",
                                "type": "REJECT",
                                "title": "拒绝",
                                "script": "def run(session){return new com.codingapi.flow.script.action.RejectActionScript.RejectResult(session.getStartNode().getId())}"
                            },
                            {
                                "enable": true,
                                "display": {
                                    "icon": null,
                                    "style": null,
                                    "title": "保存"
                                },
                                "id": "QAa8h5TFcZvhVINFPe",
                                "type": "SAVE",
                                "title": "保存"
                            },
                            {
                                "enable": true,
                                "display": {
                                    "icon": null,
                                    "style": null,
                                    "title": "加签"
                                },
                                "id": "PLweONmmnwYUBHPUoz",
                                "type": "ADD_AUDIT",
                                "title": "加签"
                            },
                            {
                                "enable": true,
                                "display": {
                                    "icon": null,
                                    "style": null,
                                    "title": "转办"
                                },
                                "id": "jEOGBl2FjScqGSXFXe",
                                "type": "TRANSFER",
                                "title": "转办"
                            },
                            {
                                "enable": true,
                                "display": {
                                    "icon": null,
                                    "style": null,
                                    "title": "退回"
                                },
                                "id": "uNZwsZoBW3O8g37Ei0",
                                "type": "RETURN",
                                "title": "退回"
                            },
                            {
                                "enable": true,
                                "display": {
                                    "icon": null,
                                    "style": null,
                                    "title": "委派"
                                },
                                "id": "fXUMgnCQHPYdkiJ5gT",
                                "type": "DELEGATE",
                                "title": "委派"
                            }
                        ],
                        "TimeoutStrategy": {
                            "timeoutTime": "86400000",
                            "type": "REMIND",
                            "strategyType": "TimeoutStrategy"
                        },
                        "MultiOperatorAuditStrategy": {
                            "type": "SEQUENCE",
                            "percent": "0.0",
                            "strategyType": "MultiOperatorAuditStrategy"
                        },
                        "SameOperatorAuditStrategy": {
                            "type": "AUTO_PASS",
                            "strategyType": "SameOperatorAuditStrategy"
                        },
                        "RecordMergeStrategy": {
                            "enable": false,
                            "strategyType": "RecordMergeStrategy"
                        },
                        "ResubmitStrategy": {
                            "type": "RESUME",
                            "strategyType": "ResubmitStrategy"
                        },
                        "AdviceStrategy": {
                            "signable": false,
                            "adviceNullable": true,
                            "strategyType": "AdviceStrategy"
                        },
                        "ErrorTriggerStrategy": {
                            "script": "def run(request){ return $bind.createErrorThrow(request.getStartNode()); }",
                            "strategyType": "ErrorTriggerStrategy"
                        },
                        "NodeTitleStrategy": {
                            "script": "def run(request){return '你有一条待办'}",
                            "strategyType": "NodeTitleStrategy"
                        },
                        "FormFieldPermissionStrategy": {
                            "strategyType": "FormFieldPermissionStrategy",
                            "fieldPermissions": []
                        },
                        "OperatorLoadStrategy": {
                            "script": "def run(request){return [request.getCreatedOperator()]}",
                            "strategyType": "OperatorLoadStrategy"
                        },
                        "RevokeStrategy": {
                            "enable": true,
                            "type": "REVOKE_CURRENT",
                            "strategyType": "RevokeStrategy"
                        }
                    },
                    "blocks": []
                },
                {
                    "id": "1condition",
                    "type": "CONDITION",
                    "data": {
                        "title": "条件控制",
                        "order": 1
                    },
                    "blocks": [
                        {
                            "id": "1condition1",
                            "type": "CONDITION_BRANCH",
                            "data": {
                                "title": "条件节点节点",
                                "order": "0",
                                "actions": [],
                                "script": "def run(session){return true}"
                            },
                            "blocks": [
                                {
                                    "id": "approval2",
                                    "type": "APPROVAL",
                                    "data": {
                                        "title": "审批节点",
                                        "order": "0",
                                        "actions": [
                                            {
                                                "enable": true,
                                                "display": {
                                                    "icon": null,
                                                    "style": null,
                                                    "title": "通过"
                                                },
                                                "id": "jFclrQY7BXnufqrRVi",
                                                "type": "PASS",
                                                "title": "通过"
                                            },
                                            {
                                                "enable": true,
                                                "display": {
                                                    "icon": null,
                                                    "style": null,
                                                    "title": "拒绝"
                                                },
                                                "id": "M381NXjHxpHRSovY6M",
                                                "type": "REJECT",
                                                "title": "拒绝",
                                                "script": "def run(session){return new com.codingapi.flow.script.action.RejectActionScript.RejectResult(session.getStartNode().getId())}"
                                            },
                                            {
                                                "enable": true,
                                                "display": {
                                                    "icon": null,
                                                    "style": null,
                                                    "title": "保存"
                                                },
                                                "id": "md0ILpIsXfyxeAmwzK",
                                                "type": "SAVE",
                                                "title": "保存"
                                            },
                                            {
                                                "enable": true,
                                                "display": {
                                                    "icon": null,
                                                    "style": null,
                                                    "title": "加签"
                                                },
                                                "id": "LReGubxiOmVWdR1tlX",
                                                "type": "ADD_AUDIT",
                                                "title": "加签"
                                            },
                                            {
                                                "enable": true,
                                                "display": {
                                                    "icon": null,
                                                    "style": null,
                                                    "title": "转办"
                                                },
                                                "id": "BF9RWHJPT7X9kcyRNy",
                                                "type": "TRANSFER",
                                                "title": "转办"
                                            },
                                            {
                                                "enable": true,
                                                "display": {
                                                    "icon": null,
                                                    "style": null,
                                                    "title": "退回"
                                                },
                                                "id": "s8BstOlYnFHyqYKkBo",
                                                "type": "RETURN",
                                                "title": "退回"
                                            },
                                            {
                                                "enable": true,
                                                "display": {
                                                    "icon": null,
                                                    "style": null,
                                                    "title": "委派"
                                                },
                                                "id": "1sFv6SEp8BlZZ0BQwd",
                                                "type": "DELEGATE",
                                                "title": "委派"
                                            }
                                        ],
                                        "TimeoutStrategy": {
                                            "timeoutTime": "86400000",
                                            "type": "REMIND",
                                            "strategyType": "TimeoutStrategy"
                                        },
                                        "MultiOperatorAuditStrategy": {
                                            "type": "SEQUENCE",
                                            "percent": "0.0",
                                            "strategyType": "MultiOperatorAuditStrategy"
                                        },
                                        "SameOperatorAuditStrategy": {
                                            "type": "AUTO_PASS",
                                            "strategyType": "SameOperatorAuditStrategy"
                                        },
                                        "RecordMergeStrategy": {
                                            "enable": false,
                                            "strategyType": "RecordMergeStrategy"
                                        },
                                        "ResubmitStrategy": {
                                            "type": "RESUME",
                                            "strategyType": "ResubmitStrategy"
                                        },
                                        "AdviceStrategy": {
                                            "signable": false,
                                            "adviceNullable": true,
                                            "strategyType": "AdviceStrategy"
                                        },
                                        "ErrorTriggerStrategy": {
                                            "script": "def run(request){ return $bind.createErrorThrow(request.getStartNode()); }",
                                            "strategyType": "ErrorTriggerStrategy"
                                        },
                                        "NodeTitleStrategy": {
                                            "script": "def run(request){return '你有一条待办'}",
                                            "strategyType": "NodeTitleStrategy"
                                        },
                                        "FormFieldPermissionStrategy": {
                                            "strategyType": "FormFieldPermissionStrategy",
                                            "fieldPermissions": []
                                        },
                                        "OperatorLoadStrategy": {
                                            "script": "def run(request){return [request.getCreatedOperator()]}",
                                            "strategyType": "OperatorLoadStrategy"
                                        },
                                        "RevokeStrategy": {
                                            "enable": true,
                                            "type": "REVOKE_CURRENT",
                                            "strategyType": "RevokeStrategy"
                                        }
                                    },
                                    "blocks": []
                                },
                                {
                                    "id": "2condition",
                                    "type": "CONDITION",
                                    "data": {
                                        "title": "条件控制",
                                        "order": 1
                                    },
                                    "blocks": [
                                        {
                                            "id": "2condition1",
                                            "type": "CONDITION_BRANCH",
                                            "data": {
                                                "title": "条件节点节点",
                                                "order": "0",
                                                "actions": [],
                                                "script": "def run(session){return true}"
                                            },
                                            "blocks": []
                                        },
                                        {
                                            "id": "2condition2",
                                            "type": "CONDITION_BRANCH",
                                            "data": {
                                                "title": "条件节点节点",
                                                "order": "0",
                                                "actions": [],
                                                "script": "def run(session){return true}"
                                            },
                                            "blocks": []
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            "id": "1condition2",
                            "type": "CONDITION_BRANCH",
                            "data": {
                                "title": "条件节点节点",
                                "order": "0",
                                "actions": [],
                                "script": "def run(session){return true}"
                            },
                            "blocks": []
                        }
                    ]
                },
                {
                    "id": "end",
                    "type": "END",
                    "data": {
                        "title": "结束节点",
                        "order": "0",
                        "actions": []
                    },
                    "blocks": []
                }
            ]
        }

        const workflowEdgeConvertor = new WorkflowEdgeConvertor(workflow as any);

        const edges = workflowEdgeConvertor.toEdges();
        console.log(edges);

        // console.log(JSON.stringify(apiData));

    });

    // test('toRender', () => {
    //     const workflow = {
    //         "updatedTime": "1770114901125",
    //         "code": "GB8Mi0UgxV",
    //         "nodes": [
    //             {
    //                 "view": "default",
    //                 "strategies": [
    //                     {
    //                         "script": "def run(request){return '你有一条待办'}",
    //                         "strategyType": "NodeTitleStrategy"
    //                     },
    //                     {
    //                         "strategyType": "FormFieldPermissionStrategy",
    //                         "fieldPermissions": []
    //                     },
    //                     {
    //                         "enable": true,
    //                         "type": "REVOKE_CURRENT",
    //                         "strategyType": "RevokeStrategy"
    //                     }
    //                 ],
    //                 "name": "开始节点",
    //                 "id": "pqkm5BsNdfcCmlrE5a",
    //                 "type": "START",
    //                 "actions": [
    //                     {
    //                         "enable": true,
    //                         "display": {
    //                             "title": "通过"
    //                         },
    //                         "id": "6ToApmIIktIE25N1Yl",
    //                         "type": "PASS",
    //                         "title": "通过"
    //                     },
    //                     {
    //                         "enable": true,
    //                         "display": {
    //                             "title": "保存"
    //                         },
    //                         "id": "PbPn8WnT01GSMmhrCX",
    //                         "type": "SAVE",
    //                         "title": "保存"
    //                     },
    //                     {
    //                         "enable": true,
    //                         "display": {
    //                             "title": "自定义"
    //                         },
    //                         "id": "FuldI2bNA5JgsK4g8y",
    //                         "type": "CUSTOM",
    //                         "title": "自定义",
    //                         "script": "def run(session){return 'PASS'}"
    //                     }
    //                 ],
    //                 "order": "0"
    //             },
    //             {
    //                 "strategies": [],
    //                 "name": "结束节点",
    //                 "id": "lCRSVX2fJW7cfKkHtC",
    //                 "type": "END",
    //                 "actions": [],
    //                 "order": "0"
    //             }
    //         ],
    //         "strategies": [
    //             {
    //                 "enable": true,
    //                 "strategyType": "InterfereStrategy"
    //             },
    //             {
    //                 "enable": true,
    //                 "interval": "60",
    //                 "strategyType": "UrgeStrategy"
    //             }
    //         ],
    //         "edges": [
    //             {
    //                 "from": "pqkm5BsNdfcCmlrE5a",
    //                 "to": "lCRSVX2fJW7cfKkHtC"
    //             }
    //         ],
    //         "createdTime": "1770114901125",
    //         "id": "2hw6LXYLokSKqOmFmF",
    //         "operatorCreateScript": "def run(operator){return true}"
    //     };
    //
    //     const workflowConvertor = new WorkflowConvertor(workflow as any);
    //     const renderData = workflowConvertor.toRender();
    //     console.log(renderData.nodes);
    // });
});
