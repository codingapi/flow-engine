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

提供 Java 标准库缺失的通用工具方法，解决以下问题：

- **字符串处理**：`StringUtils` 提供空值安全的字符串操作（判空、截取、替换等）
- **集合操作**：`CollectionUtils` 提供集合判空、交集、并集等操作
- **对象工具**：`ObjectUtils` 提供空值安全的 equals、toString 等
- **IO 工具**：`commons-io` 提供文件复制、流操作等简化方法

## 如何使用

### 项目使用的 Commons 库

| 库 | 版本 | 核心工具类 |
|---|------|-----------|
| `commons-lang3` | 3.20.0 | `StringUtils`、`CollectionUtils`、`ObjectUtils`、`NumberUtils` |
| `commons-io` | 2.17.0 | `IOUtils`、`FileUtils` |

### 在 Flow Engine 中的使用

- `StringUtils` 用于脚本执行、节点名称验证等场景的空值安全检查
- `IOUtils` 用于流程配置文件的读写操作

## 使用实例

```java
import org.apache.commons.lang3.StringUtils;

// 字符串判空（空值安全）
if (StringUtils.isBlank(nodeId)) {
    throw FlowNotFoundException.node("null");
}

// 默认值
String title = StringUtils.defaultIfBlank(node.getName(), "未命名节点");

// 字符串截取
String truncated = StringUtils.abbreviate(description, 100);

// IO 工具
import org.apache.commons.io.IOUtils;
String json = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
```
