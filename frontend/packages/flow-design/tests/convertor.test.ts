import {describe, test} from '@rstest/core';
import {WorkflowConvertor} from "@/pages/design-panel/presenters/convertor";

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
                                "id": "vJepAYtXx4TeX0UvI5",
                                "type": "PASS",
                                "title": "通过"
                            },
                            {
                                "enable": true,
                                "display": {
                                    "title": "保存"
                                },
                                "id": "4p8ICY3kd4dKF9JQ7D",
                                "type": "SAVE",
                                "title": "保存"
                            },
                            {
                                "enable": true,
                                "display": {
                                    "title": "自定义"
                                },
                                "id": "HXm0uao7v3iusuP9f4",
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
                    "id": "1branch",
                    "type": "CONDITION",
                    "data": {
                        "title": "条件控制",
                        "order": 1
                    },
                    "blocks": [
                        {
                            "id": "1branch1",
                            "type": "CONDITION_BRANCH",
                            "data": {
                                "title": "条件节点节点",
                                "order": "0",
                                "actions": [],
                                "script": "def run(session){return true}"
                            },
                            "blocks": [
                                {
                                    "id": "2branch",
                                    "type": "CONDITION",
                                    "data": {
                                        "title": "条件控制",
                                        "order": 1
                                    },
                                    "blocks": [
                                        {
                                            "id": "2branch1",
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
                                            "id": "2branch2",
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
                            "id": "1branch2",
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

        const apiData = workflowConvertor.toApi();
        console.log(apiData.nodes);
        console.log(apiData.edges);

        console.log(JSON.stringify(apiData));

    });

    test('toRender', () => {
        const workflow = {
            "updatedTime": "1770114901125",
            "code": "GB8Mi0UgxV",
            "nodes": [
                {
                    "view": "default",
                    "strategies": [
                        {
                            "script": "def run(request){return '你有一条待办'}",
                            "strategyType": "NodeTitleStrategy"
                        },
                        {
                            "strategyType": "FormFieldPermissionStrategy",
                            "fieldPermissions": []
                        },
                        {
                            "enable": true,
                            "type": "REVOKE_CURRENT",
                            "strategyType": "RevokeStrategy"
                        }
                    ],
                    "name": "开始节点",
                    "id": "pqkm5BsNdfcCmlrE5a",
                    "type": "START",
                    "actions": [
                        {
                            "enable": true,
                            "display": {
                                "title": "通过"
                            },
                            "id": "6ToApmIIktIE25N1Yl",
                            "type": "PASS",
                            "title": "通过"
                        },
                        {
                            "enable": true,
                            "display": {
                                "title": "保存"
                            },
                            "id": "PbPn8WnT01GSMmhrCX",
                            "type": "SAVE",
                            "title": "保存"
                        },
                        {
                            "enable": true,
                            "display": {
                                "title": "自定义"
                            },
                            "id": "FuldI2bNA5JgsK4g8y",
                            "type": "CUSTOM",
                            "title": "自定义",
                            "script": "def run(session){return 'PASS'}"
                        }
                    ],
                    "order": "0"
                },
                {
                    "strategies": [],
                    "name": "结束节点",
                    "id": "lCRSVX2fJW7cfKkHtC",
                    "type": "END",
                    "actions": [],
                    "order": "0"
                }
            ],
            "strategies": [
                {
                    "enable": true,
                    "strategyType": "InterfereStrategy"
                },
                {
                    "enable": true,
                    "interval": "60",
                    "strategyType": "UrgeStrategy"
                }
            ],
            "edges": [
                {
                    "from": "pqkm5BsNdfcCmlrE5a",
                    "to": "lCRSVX2fJW7cfKkHtC"
                }
            ],
            "createdTime": "1770114901125",
            "id": "2hw6LXYLokSKqOmFmF",
            "operatorCreateScript": "def run(operator){return true}"
        };

        const workflowConvertor = new WorkflowConvertor(workflow as any);
        const renderData = workflowConvertor.toRender();
        console.log(renderData.nodes);
    });
});
