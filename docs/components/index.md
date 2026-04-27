# 组件索引

> 本文档由 `/rebuild-components-index` 自动生成。每次 `/write-components` 完成后应自动重建。
> 手动维护本文件会被下一次重建覆盖。

| 组件模块 | 组件名称 | 组件描述（应用场景） | 文档地址 |
|----------|----------|----------------------|----------|
| flow-engine-framework | FlowScriptContext | Groovy 脚本运行时的 $bind 上下文对象，为脚本提供获取 Spring Bean、流程记录、审批人等数据能力的统一入口。 | [flow-engine-framework_FlowScriptContext](./flow-engine-framework_FlowScriptContext.md) |
| flow-engine-framework | GatewayContext | 流程操作者网关上下文，以防腐层模式（Anti-Corruption Layer）隔离框架与业务系统对用户数据的访问，是框架内部获取审批人信息的统一入口。 | [flow-engine-framework_GatewayContext](./flow-engine-framework_GatewayContext.md) |
| flow-engine-framework | GroovyScriptBind | Groovy 脚本中 $bind 变量的实际绑定对象，包装 FlowScriptContext 的数据访问方法，为脚本提供获取 Spring Bean、流程记录和审批人的能力。 | [flow-engine-framework_GroovyScriptBind](./flow-engine-framework_GroovyScriptBind.md) |
| flow-engine-framework | GroovyScriptRequest | Groovy 流程脚本中 request 参数的实际类型，封装流程会话中的节点、表单、操作人等上下文数据，供脚本读取和判断。 | [flow-engine-framework_GroovyScriptRequest](./flow-engine-framework_GroovyScriptRequest.md) |
| flow-engine-framework | GroovyWorkflowRequest | 工作流级别的轻量级脚本请求对象，仅包含当前操作人和工作流信息，用于人员匹配脚本和流程创建者验证场景。 | [flow-engine-framework_GroovyWorkflowRequest](./flow-engine-framework_GroovyWorkflowRequest.md) |
| flow-engine-framework | IFlowOperator | 流程操作人核心接口，定义用户 ID、名称、管理员标识和转交审批人能力，是流程引擎中所有用户相关操作的统一抽象。 | [flow-engine-framework_IFlowOperator](./flow-engine-framework_IFlowOperator.md) |
| flow-engine-framework | RepositoryHolderContext | 流程引擎仓库持有者上下文，以单例模式聚合并管理所有仓储实例和服务，是框架运行时的基础设施注册中心。 | [flow-engine-framework_RepositoryHolderContext](./flow-engine-framework_RepositoryHolderContext.md) |
| flow-engine-framework | ScriptRegistryContext | 脚本注册中心单例，管理各类默认 Groovy 脚本（路由、条件、触发、人员加载等），支持自定义替换默认脚本实现。 | [flow-engine-framework_ScriptRegistryContext](./flow-engine-framework_ScriptRegistryContext.md) |
