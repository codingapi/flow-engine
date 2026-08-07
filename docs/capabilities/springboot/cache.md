---
name: springboot/cache
module: springboot
description: 内存缓存能力，基于手写 LRU 缓存（LinkedHashCache）缓存流程运行时配置
status: 已实现
scope: 后端
source: 框架:Flow Engine 自研
framework_version: "3.5.9"
---

## 解决什么问题

Flow Engine 不使用 Spring Cache（无 `spring-boot-starter-cache` 依赖，也无 `@Cacheable` / `@CacheEvict` 等注解），真实缓存为手写的进程内内存缓存，解决以下问题：

- **减少数据库查询**：缓存流程运行时配置，避免频繁的数据库查询和 JSON 反序列化
- **LRU 淘汰**：`WorkflowRuntimeCache` 容量上限 1024，超出后按最近最少使用（LRU）策略自动淘汰最久未访问的条目

## 如何使用

### 核心类

| 类 | 用途 |
|------|------|
| `WorkflowRuntimeCache` | 流程运行时缓存入口（单例，`getInstance()`），提供 `get` / `sync` 方法 |
| `LinkedHashCache` | 基于 `LinkedHashMap`（accessOrder=true）手写的通用 LRU 缓存容器 |

### 在 Flow Engine 中的使用

真实缓存为 `WorkflowRuntimeCache`（`flow-engine-framework/src/main/java/com/codingapi/flow/cache/WorkflowRuntimeCache.java`），基于 `LinkedHashCache`（`LinkedHashMap` 实现 LRU）缓存流程运行时配置，避免频繁的数据库查询和 JSON 反序列化。

`WorkflowService`（`flow-engine-framework/.../service/WorkflowService.java`）中手动调用缓存，与 Spring Cache 无关：

## 使用实例

```java
// 手写 LRU 缓存容器（LinkedHashMap accessOrder=true）
public class LinkedHashCache<KEY, VALUE> {

    private final Map<KEY, VALUE> cache;

    public LinkedHashCache(int maxCacheSize) {
        this.cache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<KEY, VALUE> eldest) {
                return size() > maxCacheSize;
            }
        };
    }

    public void put(KEY key, VALUE value) {
        this.cache.put(key, value);
    }

    public VALUE get(KEY key) {
        return this.cache.get(key);
    }

    public void remove(KEY key) {
        this.cache.remove(key);
    }

    public void clear() {
        this.cache.clear();
    }
}

// 流程运行时缓存（单例）
public class WorkflowRuntimeCache {

    public static final int MAX_CACHE_SZE = 1024;

    private final LinkedHashCache<Long, WorkflowRuntime> cache;

    public void sync(WorkflowRuntime workflowRuntime) {
        this.cache.put(workflowRuntime.getId(), workflowRuntime);
    }

    public WorkflowRuntime get(long runtimeId, Supplier<WorkflowRuntime> defaultLoader) {
        WorkflowRuntime current = this.cache.get(runtimeId);
        if (current == null) {
            current = defaultLoader.get();
            if (current != null) {
                this.sync(current);
            }
        }
        return current;
    }
}

// WorkflowService 中手动调用缓存
public WorkflowRuntime getWorkflowRuntime(long runtimeId) {
    return WorkflowRuntimeCache.getInstance().get(runtimeId, () -> workflowRuntimeRepository.get(runtimeId));
}

public void saveWorkflowRuntime(WorkflowRuntime workflowRuntime) {
    this.workflowRuntimeRepository.save(workflowRuntime);
    WorkflowRuntimeCache.getInstance().sync(workflowRuntime);
}
```