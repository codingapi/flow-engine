# 多身份参与（Operator Identity）

> 面向业务项目的集成说明，对应 issue #288。引擎只负责**透传、存储与比较**身份标识，不解释其业务语义，也不内建组织模型。

## 1. 解决什么问题

一个用户可能同时隶属多个部门（或身兼多个岗位），在不同事项上以不同身份参与流程：

| 差异类型 | 例子 |
|----------|------|
| 节点 / 分支差异 | user1 在 department1 发起需要「组长审核」，在 department2 发起则直接「部门经理审核」 |
| 人员差异 | department1 的经理是 boss1，department2 的经理是 boss2 |
| 待办差异 | user1 在 department1 / department2 各有一条待办，不能合并成一条 |

0.2.x 的引擎只认 `userId`：记录、待办、权限判定全部按 userId 比较，上述场景无法表达。

## 2. 模型

身份标识是一个**不透明字符串**，由下游定义取值，框架只做四件事：

1. 从 `IFlowOperator#getIdentity()` 读取；
2. 落库到流程记录与待办；
3. 在判定时按 `(userId, identity)` 比较；
4. 透传给 Groovy 脚本与 REST 响应。

约定：`null` 与空串等价，均表示**默认身份**（`OperatorIdentity.DEFAULT`）。未启用多身份的业务、以及 0.3.0 之前的历史数据，全部落在默认身份上，行为与历史完全一致。

```java
public interface IFlowOperator extends IUser {

    long getUserId();

    String getName();

    /**
     * 身份标识，空串表示默认身份。框架不解释语义。
     */
    default String getIdentity() {
        return OperatorIdentity.DEFAULT;
    }

    /**
     * 身份的附加属性（部门名、岗位名等），只透传给脚本与前端，不参与引擎判定，不落库。
     */
    default Map<String, String> getIdentityAttributes() {
        return Map.of();
    }
}
```

两个方法都是 `default`，**下游存量实现类无需改动即可编译**。

## 3. 下游需要做什么

### 3.1 实现身份（可选）

```java
public class User implements IFlowOperator {

    private String identity;   // 运行期选择，通常不落用户主表

    @Override
    public String getIdentity() {
        return identity;
    }
}
```

### 3.2 让网关按身份返回操作者（关键）

```java
@Override
public IFlowOperator get(long id, String identity) {
    User user = userRepository.getUserById(id);
    if (user == null || OperatorIdentity.isDefault(identity)) {
        return user;
    }
    // 必须复制：直接改写实体会污染持久化上下文 / 线程缓存，导致同一 id 的两个身份互相覆盖
    return user.withIdentity(identity);
}
```

> `get(long, String)` 在接口上是 `default` 方法，默认退化为 `get(id)`（忽略身份）。**不实现它时，请求里声明的身份不会被写入记录** —— 引擎的身份来源是操作者对象本身，而不是请求参数。多身份场景必须实现此方法。

批量取人同样提供了两个 `default` 重载，用于流程树与人员设定场景（建议一并覆盖，避免同一批人按身份回查时退化为默认身份）：

| 接口 | 新增方法 | 未覆盖时的行为 |
|------|----------|----------------|
| `FlowOperatorGateway` | `findByIds(List<Long>, String)` | 退化为 `findByIds(ids)` |
| `IRepositoryHolder` | `findOperatorByIds(List<Long>, String)` | 退化为 `findOperatorByIds(ids)` |
| `IRepositoryHolder` | `getOperatorById(long, String)` | 退化为 `getOperatorById(id)` |

> 引擎在 `GatewayContext#getFlowOperator(id, identity)` 处做运行期检查：请求声明了非默认身份、但回查到的操作者身份为空时，会按 `(人员id, 身份)` 去重输出一条 WARN，提示下游未覆盖带身份的取人方法。这类问题原先是**静默降级**（按身份查待办直接返回空列表），集成阶段极难定位。

### 3.3 请求声明身份

REST 接口支持 `operatorIdentity` 参数（与 `operatorId` 对称）：未传时取当前登录操作者的身份。

```bash
# 以 department1 的身份发起
curl -X POST 'http://host/api/cmd/record/create?operatorIdentity=department1' \
     -H 'Content-Type: application/json' -d '{...}'

# 以 department1 的身份查看待办
curl 'http://host/api/query/record/todo?operatorId=1&operatorIdentity=department1'
```

编程式 API 中对应各请求对象的 `operatorIdentity` 字段：

| 请求对象 | 字段 |
|----------|------|
| `FlowCreateRequest` | `operatorIdentity` |
| `FlowAdviceBody`（`FlowActionRequest.advice`） | `operatorIdentity` |
| `FlowRevokeRequest` / `FlowDeleteRequest` / `FlowUrgeRequest` | `operatorIdentity` |
| `FlowDetailRequest` / `FlowProcessNodeRequest` | `operatorIdentity` |
| `FlowReissueRequest` / `FlowForceSyncRequest` / `FlowSubProcessResetRequest` | `operatorIdentity` |

**为节点指定同一人的多个身份**：`INITIATOR_SELECT` / `APPROVER_SELECT` 节点原有 `operatorSelectMap`（`节点ID -> List<Long>`）无法表达「同一人员以两个身份参与」，新增 `operatorSelectOperators`（`节点ID -> List<OperatorKey>`），每项为 `(operatorId, identity)`：

```java
createRequest.setOperatorSelectOperators(Map.of(approvalNodeId, List.of(
        OperatorKey.of(2L, "department1"),
        OperatorKey.of(2L, "department2"))));
```

- 同一人员的两个身份是**两项**参与人，不会被去重合并；完全相同的二元组才去重；
- `operatorSelectOperators` 优先于存量的 `operatorSelectMap`，两者都不传则不落分配关系；
- 存量调用方继续使用 `operatorSelectMap`，按默认身份处理，无需改动；
- 二次发起（reissue）会连同身份一起复制源流程的人员设定，避免重走后退化到默认身份。

### 3.4 在脚本中使用身份

`GroovyScriptRequest` 新增三个函数：

| 脚本函数 | 含义 |
|----------|------|
| `getCurrentOperatorIdentity()` | 当前操作者身份 |
| `getCreatedOperatorIdentity()` | 流程发起人身份 |
| `getSubmitOperatorIdentity()` | 流程提交人身份 |

节点/分支差异（条件脚本）：

```groovy
def run(request) {
    // department1 需要组长审核，其他身份直接进入部门经理审批
    return request.getCurrentOperatorIdentity() == 'department1'
}
```

人员差异（审批人脚本）：

```groovy
def run(request) {
    def identity = request.getCreatedOperatorIdentity()
    if (identity == 'department1') return [boss1Id]
    if (identity == 'department2') return [boss2Id]
    return [defaultBossId]
}
```

需要「让某人以指定身份参与」时，用 `$bind.getOperatorByIdentity(id, identity)` 拿到带身份的操作者：

```groovy
def run(request) {
    // 让 user2 以 department1 的身份成为审批人（记录里落的就是 department1）
    return [$bind.getOperatorByIdentity(2, 'department1')]
}
```

## 4. 引擎侧行为

### 4.1 记录与待办落库

| 表 | 新增列 |
|----|--------|
| `t_flow_record` | `create_operator_identity`、`submit_operator_identity`、`current_operator_identity`、`forward_operator_identity`、`interfered_operator_identity` |
| `t_flow_todo_record` | `create_operator_identity`、`submit_operator_identity`、`current_operator_identity` |

只存身份标识本身，`getIdentityAttributes()` 的附加属性**不落库**（避免 5 个 JSON 快照列，也避免身份名称被冻结在历史数据里）。展示名称仍取操作者姓名。

升级脚本见 [sql/0.3.0-operator-identity.sql](./sql/0.3.0-operator-identity.sql)。

### 4.2 待办合并键（兼容追加）

`FlowRecord#getTodoKey()` 的格式：

| 身份 | 合并键 |
|------|--------|
| 默认身份 | `{operatorId}-{workRuntimeId}-{nodeId}`（与 0.2.x 完全一致） |
| 非空身份 | `{operatorId}-{workRuntimeId}-{nodeId}#{identity}` |

默认身份保持历史格式是刻意的：升级瞬间仍在运行的流程，其存量待办行是旧格式键，若统一改格式会导致新流转算出的键查不到旧行 → 重复生成待办、合并关系断裂。

### 4.3 查询隔离

待办 / 已办 / 抄送查询按 `(currentOperatorId, currentOperatorIdentity)` 过滤，默认身份的条件写成 `coalesce(identity, '') = ''`，保证历史 NULL 行仍能被查到（否则升级后存量待办会从列表中消失）。

**Mock 链路同口径**：带 `mockKey` 的 REST 调用走 `FlowRecordQueryMockService`，三个查询的 `identity` 重载均已实现（待办 / 抄送 / 已办），不会再退化成按 id 全量返回。前端联调与真实库路径行为一致。

### 4.4 判定升级为 `(userId, identity)`

以下判定统一由「只比 userId」升级为「比 userId + identity」，身份为空时与历史等价：

- 审批动作的参与人校验（`WorkflowStrategyManager#verifyOperator`）
- 撤销、删除、催办、子流程重置的记录归属校验
- 加签 / 减签 / 转交 / 委托的人员匹配（`OperatorManager`）
- 提交人 == 审批人的自动通过判定
- 流程干预人识别
- 操作者线程缓存与网关缓存的键
- 详情页 `currentNodeOperators`（待审批人 / 减签候选）的去重与「排除当前操作人」判定
- 流程树批量登记操作者：按记录里的身份分组回查，与单条读取口径一致（避免 N+1）

### 4.5 身份写入约束

落库前统一经 `OperatorIdentity.validate()`：

| 约束 | 原因 |
|------|------|
| 长度 ≤ 64 | 与 DDL `VARCHAR(64)` 及实体 `@Column(length = 64)` 对齐 |
| 不含 `#` | 与待办合并键的身份后缀分隔符冲突 |
| 不含控制字符 | 内部缓存键以控制字符拼接 |

读取路径仍用宽松的 `normalize()`（仅 null → 空串），历史数据不会被校验拦住。

人员分配表 `t_flow_operator_assignment` **无需改表**：身份编码在 `operatorIds` JSON 列内。全部为默认身份时保持历史格式 `[1,2,3]`；存在非默认身份时写 `[{"operatorId":2,"identity":"department1"}]`，读取端同时兼容两种格式。

## 5. 兼容性与限制

| 项 | 说明 |
|----|------|
| 存量数据 | 新列为 NULL，按默认身份读取，**无需回填** |
| 回滚 | 仅新增 nullable 列，旧版本不映射新列，可直接回滚 |
| 工作流定义 | 与身份无关，导入导出不受影响 |
| 身份长度 | 建议 ≤ 64 字符（复合 key 用 `:` 拼接） |
| 待办索引 | 实体未声明 `@Index`，大表请按 DDL 脚本手工加索引 |
| 不做的事 | 引擎不建组织模型、不做身份授权校验（「user1 能否以 department1 身份操作」由业务侧保证）、不冻结身份名称 |

> 身份是一个**不透明字符串**，不要尝试在引擎里比较它的业务含义；需要「部门名称」等展示信息，请用 `getIdentityAttributes()` 或按身份回查组织数据。
