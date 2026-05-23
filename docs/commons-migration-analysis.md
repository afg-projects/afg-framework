# 迁移到 Commons 模块的候选类分析

## 一、候选类评估

### 1.1 高优先级候选（推荐迁移）

| 类名 | 当前位置 | 使用次数 | 依赖项 | 迁移难度 | 推荐理由 |
|------|----------|----------|--------|----------|----------|
| `AfgException` | core/exception | 1 | 无 | 低 | 异常基类，无外部依赖 |
| `ErrorCode` | core/model/exception | 6 | ErrorCategory, ErrorCodeMessageSource | 中 | 接口定义，通用性强 |
| `ErrorCategory` | core/model/exception | 5 | 无 | 低 | 枚举，无依赖 |
| `BusinessException` | core/model/exception | 7 | AfgException, ErrorCode, CommonErrorCode | 中 | 核心业务异常 |
| `Result` | core/model/result | 5 | Jackson | 低 | 统一响应，仅依赖Jackson |
| `PageData` | core/model/result | 4 | 无 | 低 | 分页结果，无外部依赖 |

### 1.2 中优先级候选（需评估）

| 类名 | 当前位置 | 使用次数 | 依赖项 | 迁移难度 | 备注 |
|------|----------|----------|--------|----------|------|
| `CommonErrorCode` | core/model/exception | 17 | ErrorCode | 低 | 通用错误码枚举 |
| `JacksonUtils` | core/util | 3 | Jackson | 低 | 工具类，但会引入Jackson依赖 |
| `ErrorCodeMessageSource` | core/model/exception | 1 | Spring Context | 高 | 依赖Spring，不适合放commons |

### 1.3 不推荐迁移

| 类名 | 原因 |
|------|------|
| `AfgCoreProperties` | 强依赖Spring Boot，属于配置层 |
| `DefaultCacheManager` | 依赖Caffeine和Spring，属于实现层 |
| `BaseUnitTest` | 测试基类，应保留在core |
| `ModuleRegistry` | 模块系统核心，有复杂依赖 |

## 二、依赖关系分析

### 2.1 异常体系依赖图

```
commons (目标)
├── AfgException (无依赖)
├── ErrorCategory (无依赖)
├── ErrorCode (依赖 ErrorCategory)
├── BusinessException (依赖 AfgException, ErrorCode)
└── CommonErrorCode (实现 ErrorCode)

core (保留)
├── ErrorCodeMessageSource (依赖 Spring MessageSource)
└── ErrorCodeMessageSourceConfig (Spring配置)
```

### 2.2 结果模型依赖图

```
commons (目标)
├── Result (仅依赖 Jackson annotations)
└── PageData (无依赖)
```

## 三、迁移方案

### 3.1 推荐迁移的类

**第一批：无依赖类**

```java
// commons/src/main/java/io/github/afgprojects/framework/commons/exception/
- AfgException.java
- ErrorCategory.java (移至 exception/)

// commons/src/main/java/io/github/afgprojects/framework/commons/model/
- Result.java
- PageData.java
```

**第二批：有简单依赖的类**

```java
// commons/src/main/java/io/github/afgprojects/framework/commons/exception/
- ErrorCode.java (接口)
- BusinessException.java
- CommonErrorCode.java (枚举)
```

### 3.2 Commons 模块需要的依赖

迁移后，commons 模块需要添加以下依赖：

```kotlin
// commons/build.gradle.kts
dependencies {
    // JSpecify 空安全注解
    api(libs.jspecify)
    
    // Jackson (用于 Result 的 JSON 注解)
    api(libs.bundles.jackson)
    
    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}
```

### 3.3 迁移后的包结构

```
commons/
└── src/main/java/io/github/afgprojects/framework/commons/
    ├── exception/
    │   ├── AfgException.java
    │   ├── ErrorCategory.java
    │   ├── ErrorCode.java
    │   ├── BusinessException.java
    │   └── CommonErrorCode.java
    ├── model/
    │   ├── Result.java
    │   └── PageData.java
    └── util/
        ├── NamingUtils.java (现有)
        └── JacksonUtils.java (可选)
```

## 四、迁移步骤

### 4.1 阶段一：迁移无依赖类（预估 0.5 天）

1. 创建 commons 模块目录结构
2. 迁移 `AfgException`
3. 迁移 `ErrorCategory`
4. 迁移 `Result`
5. 迁移 `PageData`
6. 更新 commons/build.gradle.kts 添加依赖
7. 更新 core 模块，改为依赖 commons

### 4.2 阶段二：迁移异常体系（预估 0.5 天）

1. 迁移 `ErrorCode` 接口
2. 迁移 `BusinessException`
3. 迁移 `CommonErrorCode`
4. 在 core 中创建 `ErrorCodeMessageSource`，保留 Spring 集成

### 4.3 阶段三：更新下游模块（预估 0.5 天）

1. 更新所有模块的导入路径
2. 更新文档
3. 运行测试验证

## 五、迁移收益

| 收益 | 说明 |
|------|------|
| 依赖解耦 | data-core、ai-core 可仅依赖 commons，获得异常和结果类 |
| 减少传递依赖 | 不再需要引入完整 core 即可使用基础模型 |
| 模块职责清晰 | commons 专注基础模型，core 专注业务能力 |

## 六、风险与对策

| 风险 | 对策 |
|------|------|
| API 变更导致编译失败 | 全局替换导入路径 |
| ErrorCodeMessageSource 依赖 Spring | 保留在 core 中，通过接口解耦 |
| Jackson 版本兼容 | 使用 Spring Boot BOM 管理 |

## 七、最终建议

**推荐执行迁移**，优先迁移以下类：

1. ✅ `AfgException` - 异常基类
2. ✅ `ErrorCategory` - 错误分类枚举
3. ✅ `ErrorCode` - 错误码接口
4. ✅ `BusinessException` - 业务异常
5. ✅ `CommonErrorCode` - 通用错误码
6. ✅ `Result` - 统一响应
7. ✅ `PageData` - 分页结果

**暂不迁移**：
- `ErrorCodeMessageSource` - 保留在 core，依赖 Spring
- `JacksonUtils` - 可选，看是否需要工具类

---

**预估工作量**：1.5 人天
