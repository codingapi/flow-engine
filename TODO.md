# Flow Engine 开发进度

## 已完成功能 ✅

### 节点功能

| 功能 | 状态 | 说明 |
|-----|------|------|
| 15 种节点类型 | ✅ | StartNode, EndNode, ApprovalNode, HandleNode, NotifyNode, RouterNode, SubProcessNode, DelayNode, TriggerNode (基础9种); ConditionNode, ParallelNode, InclusiveNode (块节点3种); ConditionBranchNode, ParallelBranchNode, InclusiveBranchNode (分支节点3种) |
| 节点层次化结构 | ✅ | 通过blocks属性实现节点间的层次关系，不再使用独立的边关系 |
| 节点生命周期管理 | ✅ | verifySession → continueTrigger → generateCurrentRecords → fillNewRecord → isDone |
| 节点配置面板 | ✅ | 基础信息、审批人配置、策略配置 |

### 多人审批策略

| 功能 | 状态 | 说明 |
|-----|------|------|
| 顺序审批 (SEQUENCE) | ✅ | 按顺序依次审批，隐藏后续记录 |
| 会签 (MERGE) | ✅ | 可设置完成比例 |
| 或签 (ANY) | ✅ | 任意一人完成即可 |
| 随机审批 (RANDOM_ONE) | ✅ | 随机选择一人审批 |

### 审批控制

| 功能 | 状态 | 说明 |
|-----|------|------|
| 相同审批人自动跳过 | ✅ | SameOperatorAuditStrategy |
| 审批意见必填控制 | ✅ | AdviceStrategy.adviceNullable |
| 签名控制 | ✅ | AdviceStrategy.signable |
| 超时处理 | ✅ | TimeoutStrategy (自动提醒、自动同意、自动拒绝) |
| 退回模式控制 | ✅ | ResubmitStrategy (恢复到退回节点/逐级提交) |

### 流程操作

| 操作 | 状态 | 说明 |
|-----|------|------|
| 通过 (PASS) | ✅ | PassAction |
| 拒绝 (REJECT) | ✅ | RejectAction，支持退回上级/指定节点/终止流程 |
| 保存 (SAVE) | ✅ | SaveAction |
| 加签 (ADD_AUDIT) | ✅ | AddAuditAction |
| 委派 (DELEGATE) | ✅ | DelegateAction |
| 退回 (RETURN) | ✅ | ReturnAction |
| 转办 (TRANSFER) | ✅ | TransferAction |
| 自定义 (CUSTOM) | ✅ | CustomAction，支持脚本配置 |

### 流程全局配置

| 功能 | 状态 | 说明 |
|-----|------|------|
| 流程撤销 | ✅ | Workflow.isRevoke |
| 流程干预 | ✅ | Workflow.isInterfere |
| 流程催办 | ✅ | UrgeStrategy，支持催办间隔配置（默认60秒） |
| 节点撤回 | ✅ | RevokeStrategy，支持撤回下级/撤回到当前节点 |
| 发起人匹配 | ✅ | OperatorMatchScript |

### 委托功能

| 功能 | 状态 | 说明 |
|-----|------|------|
| 委托代理 | ✅ | 支持审批权限委托 |

### 并行分支

| 功能 | 状态 | 说明 |
|-----|------|------|
| 并行分支执行 | ✅ | ParallelBranchNode |
| 汇聚节点检测 | ✅ | ParallelNodeRelationHelper |
| 并行分支同步 | ✅ | parallelId, parallelBranchNodeId, parallelBranchTotal |

### 策略体系

| 策略 | 状态 | 说明 |
|-----|------|------|
| MultiOperatorAuditStrategy | ✅ | 多人审批策略 |
| TimeoutStrategy | ✅ | 超时策略 |
| SameOperatorAuditStrategy | ✅ | 同一操作者审批策略 |
| RecordMergeStrategy | ✅ | 记录合并策略 |
| ResubmitStrategy | ✅ | 重新提交策略 |
| AdviceStrategy | ✅ | 审批意见策略 |
| OperatorLoadStrategy | ✅ | 操作者加载策略 |
| ErrorTriggerStrategy | ✅ | 异常触发策略 |
| NodeTitleStrategy | ✅ | 节点标题策略 |
| FormFieldPermissionStrategy | ✅ | 表单字段权限策略 |
| DelayStrategy | ✅ | 延迟策略（支持秒/分钟/小时/天） |
| TriggerStrategy | ✅ | 触发策略 |
| RouterStrategy | ✅ | 路由策略 |
| SubProcessStrategy | ✅ | 子流程策略 |
| RevokeStrategy | ✅ | 撤回策略（撤回下级/撤回到当前节点） |
| InterfereStrategy | ✅ | 干预策略（工作流级别） |
| UrgeStrategy | ✅ | 催办策略（工作流级别，支持催办间隔配置） |

### 脚本系统

| 功能 | 状态 | 说明 |
|-----|------|------|
| Groovy 脚本执行 | ✅ | ScriptRuntimeContext |
| 发起人匹配脚本 | ✅ | OperatorMatchScript |
| 审批人加载脚本 | ✅ | OperatorLoadScript |
| 节点标题脚本 | ✅ | NodeTitleScript |
| 条件判断脚本 | ✅ | ConditionScript |
| 路由节点脚本 | ✅ | RouterNodeScript |
| 子流程脚本 | ✅ | SubProcessScript |
| 触发器脚本 | ✅ | TriggerScript |
| 异常触发脚本 | ✅ | ErrorTriggerScript |
| 拒绝动作脚本 | ✅ | RejectActionScript |
| 自定义动作脚本 | ✅ | CustomScript |
| 线程安全 | ✅ | 细粒度同步锁，支持并发执行 |
| 自动资源清理 | ✅ | 阈值触发 + 定时清理 |

### 延迟与触发节点

| 功能 | 状态 | 说明 |
|-----|------|------|
| 延迟节点 | ✅ | DelayNode，支持延迟时间单位配置（秒/分钟/小时/天） |
| 延迟任务管理 | ✅ | DelayTaskManager，基于Timer的延迟触发 |
| 触发节点 | ✅ | TriggerNode，支持触发脚本配置 |
| 触发任务执行 | ✅ | FlowDelayTriggerService |

### 异常体系

| 异常 | 状态 | 说明 |
|-----|------|------|
| FlowException | ✅ | 基类 (RuntimeException) |
| FlowValidationException | ✅ | 参数验证异常 |
| FlowNotFoundException | ✅ | 资源未找到异常 |
| FlowStateException | ✅ | 状态异常 |
| FlowPermissionException | ✅ | 权限异常 |
| FlowConfigException | ✅ | 配置异常 |
| FlowExecutionException | ✅ | 执行异常 |
| 字符串错误码格式 | ✅ | `category.subcategory.errorType` 格式 |
| 静态工厂方法 | ✅ | 所有异常通过静态工厂方法创建 |

### 表单系统

| 功能 | 状态 | 说明 |
|-----|------|------|
| 主表字段配置 | ✅ | FormMeta |
| 子表支持 | ✅ | SubFormMeta |
| 字段权限控制 | ✅ | READ/WRITE/NONE |
| 表单数据验证 | ✅ | FormData |

### 事件系统

| 事件 | 状态 | 说明 |
|-----|------|------|
| FlowRecordStartEvent | ✅ | 流程开始事件 |
| FlowRecordTodoEvent | ✅ | 待办事件 |
| FlowRecordDoneEvent | ✅ | 已办事件 |
| FlowRecordFinishEvent | ✅ | 流程完成事件 |
| FlowRecordUrgeEvent | ✅ | 催办事件 |

### 设计模式应用

| 模式 | 状态 | 说明 |
|-----|------|------|
| Builder Pattern | ✅ | WorkflowBuilder, BaseNodeBuilder |
| Factory Pattern | ✅ | NodeFactory, NodeStrategyFactory, FlowActionFactory |
| Strategy Pattern | ✅ | INodeStrategy + StrategyManager |
| Template Method | ✅ | BaseFlowNode, BaseAction, BaseStrategy |
| Singleton Pattern | ✅ | ScriptRuntimeContext, RepositoryContext, GatewayContext |
| Chain of Responsibility | ✅ | triggerNode() 递归触发 |
| Composite Pattern | ✅ | 节点包含多个策略和动作 |
| Copy Pattern | ✅ | 策略和动作的复制更新 |

---

## 待开发功能 🚧

### 节点功能增强

- [ ] **条件分支节点增强**
  - [ ] 支持多条件表达式组合
  - [ ] 条件优先级配置
  - [ ] 默认分支配置

- [ ] **子流程节点完善**
  - [ ] 子流程参数传递机制
  - [ ] 子流程结果返回
  - [ ] 子流程异步调用
  - [ ] 子流程实例管理

- [ ] **包容分支节点完善**
  - [ ] 包容分支汇聚逻辑
  - [ ] 包容分支条件配置

### 流程功能

- [ ] **流程版本管理**
  - [ ] 流程版本历史记录
  - [ ] 版本对比功能
  - [ ] 版本回滚功能

- [ ] **流程模拟**
  - [ ] 流程预执行
  - [ ] 流程路径分析
  - [ ] 潜在问题检测

- [ ] **流程撤回**
  - [ ] 撤回条件判断
  - [ ] 撤回权限控制
  - [ ] 撤回记录追溯

- [ ] **流程干预**
  - [ ] 管理员强制干预
  - [ ] 异常流程修正
  - [ ] 跳节点处理

### 监控与分析

- [ ] **流程监控大屏**
  - [ ] 流程实例统计
  - [ ] 节点效率分析
  - [ ] 异常流程预警

- [ ] **流程数据分析**
  - [ ] 流程周期统计
  - [ ] 审批效率分析
  - [ ] 流程瓶颈识别
  - [ ] 工作负载分析

### 性能优化

- [ ] **缓存机制**
  - [ ] 流程定义缓存
  - [ ] 审批人缓存
  - [ ] 脚本编译缓存

- [ ] **异步处理**
  - [ ] 异步事件通知
  - [ ] 异步脚本执行
  - [ ] 异步日志记录

- [ ] **数据库优化**
  - [ ] 索引优化
  - [ ] 分表分页查询
  - [ ] 历史数据归档

### 前端功能

- [ ] **流程设计器**
  - [ ] 节点拖拽设计
  - [ ] 连线绘制
  - [ ] 节点配置面板
  - [ ] 实时预览

- [ ] **流程展示**
  - [ ] PC 端流程处理界面
  - [ ] 移动端流程处理界面
  - [ ] 流程进度可视化
  - [ ] 流程图展示

- [ ] **流程监控**
  - [ ] 流程实例查询
  - [ ] 流程记录查询
  - [ ] 待办/已办列表
  - [ ] 流程统计分析

### 扩展功能

- [ ] **国际化支持**
  - [ ] 多语言支持
  - [ ] 时区处理

- [ ] **消息通知**
  - [ ] 邮件通知
  - [ ] 短信通知
  - [ ] 站内消息
  - [ ] 企业微信/钉钉集成

- [ ] **电子签名**
  - [ ] 手写签名
  - [ ] 数字证书签名
  - [ ] 签名验证

- [ ] **附件管理**
  - [ ] 附件上传
  - [ ] 附件预览
  - [ ] 附件权限控制

---

## 技术债务 🔧

- [ ] 补充单元测试覆盖率（目标 80%+）
- [ ] 完善 API 文档（Swagger/OpenAPI）
- [ ] 代码规范检查工具集成（Checkstyle/SonarQube）
- [ ] 性能基准测试
- [ ] 安全漏洞扫描
- [ ] 代码复杂度优化
- [ ] 异常处理统一化

---

## 版本规划

### v0.2.0 (当前开发版本)

- ✅ 15 种节点类型实现（新增ConditionNode、ParallelNode、InclusiveNode块节点）
- ✅ 节点层次化结构（blocks属性替代独立的边关系）
- ✅ FlowNodeState节点分类（块节点vs分支节点）
- ✅ FlowNodeEdgeManager关系管理器（遍历blocks实现）
- ✅ 延迟节点完整实现
- ✅ 触发节点完整实现
- ✅ 路由节点完整实现
- ✅ 异常体系标准化（字符串错误码格式）
- ✅ 脚本系统完善（10种脚本类型）
- ✅ 流程催办功能（UrgeStrategy + UrgeInterval + FlowRecordUrgeEvent）
- ✅ 流程干预功能（InterfereStrategy）
- ✅ 节点撤回功能（RevokeStrategy）
- 🚧 前端流程设计器基础功能（@flowgram.ai集成）

### v0.3.0 (计划中)

- [ ] 流程版本管理
- [ ] 流程模拟功能
- [ ] 消息通知集成
- [ ] 前端流程处理界面

### v1.0.0 (未来)

- [ ] 流程监控大屏
- [ ] 流程数据分析
- [ ] 性能优化（缓存、异步）
- [ ] 完整的前后端实现
- [ ] 企业级部署支持
