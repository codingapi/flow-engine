---
name: commons/lang3
module: commons
description: Apache Commons 工具库，提供字符串、集合、IO 等通用工具方法
status: 已实现
scope: 后端
source: 框架:Apache Commons
import: "org.apache.commons:commons-lang3"
framework_version: "3.20.0"
---

## 解决什么问题

提供 Java 标准库缺失的通用工具方法：

- **字符串处理**：`StringUtils` 提供空值安全的字符串操作（判空、截取、替换等）
- **集合操作**：`CollectionUtils` 提供集合判空、交集、并集等操作
- **对象工具**：`ObjectUtils` 提供空值安全的 equals、toString 等
- **IO 工具**：`commons-io` 提供文件复制、流操作等简化方法

## 如何使用

### 项目使用的 Commons 库

| 库 | 版本 | 说明 |
|---|------|-----------|
| `commons-lang3` | 3.20.0 | 项目中仅使用 `RandomStringUtils` 生成各类 ID；`StringUtils`、`CollectionUtils`、`ObjectUtils`、`NumberUtils` 等为库自带能力，本项目未使用 |
| `commons-io` | 2.17.0 | 仅在根 pom 声明依赖，代码中未实际引用 |

### 在 Flow Engine 中的使用

`RandomStringUtils`（`org.apache.commons.lang3.RandomStringUtils`）是项目中唯一的 `commons-lang3` 使用点，位于 `FlowIDGeneratorGatewayContext`（`flow-engine-framework/src/main/java/com/codingapi/flow/generator/FlowIDGeneratorGatewayContext.java`），用于流程 ID、流程编码、流程实例 ID、节点 ID 等各类 ID 的生成：

- `generateWorkId()` — 生成 18 位流程 ID
- `generateWorkCode()` — 生成 10 位流程编码
- `generateProcessId()` — 生成 18 位流程实例 ID
- `generateNodeId()` / `generateViewCode()` 等 — 生成节点 ID、视图编码、动作 ID、并行 ID、表单字段 ID 等

## 使用实例

```java
import org.apache.commons.lang3.RandomStringUtils;

// 基于 RandomStringUtils 生成随机 ID（字母 + 数字混合）

// 生成 18 位流程 ID
private final static RandomStringUtils randomString = RandomStringUtils.secure();

public String generateWorkId() {
    return randomString.nextAlphanumeric(18);
}

// 生成 10 位流程编码
public String generateWorkCode() {
    return randomString.nextAlphanumeric(10);
}
```