# AFG Framework Gradle Plugin

[![Maven Central](https://img.shields.io/maven-central/v/io.github.afg-projects/afg-framework-gradle-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=g:io.github.afg-projects)

AFG Framework Gradle 插件，提供项目初始化、代码生成、数据库迁移、逆向工程等功能。

## 功能特性

| 功能 | CLI 命令 | 说明 |
|------|----------|------|
| 项目初始化 | `init` | 从模板创建新项目骨架 |
| 脚手架生成 | `scaffold` | 生成 CRUD 代码（Entity、Repository、Service、Controller、Migration） |
| 实体代码生成 | `generate-entity` | 从 YAML/JSON 配置生成 Entity 类 |
| 数据库逆向工程 | `reverse-engineer` | 从数据库表结构生成 Entity 类 |
| 迁移脚本生成 | `generate-migration` | 扫描实体类生成 Liquibase 变更日志 |
| 数据库迁移 | `migrate` | 执行 Liquibase 数据库迁移 |
| 一致性验证 | `validate` | 验证实体类与数据库表结构一致性 |
| 差异对比 | `diff` | 对比实体与数据库差异 |
| API 文档生成 | `api-doc` | 生成 OpenAPI 文档 |

## 安装

### Gradle (Kotlin DSL)

```kotlin
plugins {
    id("io.github.afg-projects.plugin") version "1.0.0"
}
```

### Gradle (Groovy DSL)

```groovy
plugins {
    id 'io.github.afg-projects.plugin' version '1.0.0'
}
```

## 快速开始

### 1. 创建新项目

```bash
# 使用 CLI 创建新项目
java -jar afg-framework-gradle-plugin.jar init my-project \
    -p com.example.demo \
    -d MYSQL \
    -f LIQUIBASE,VALIDATION,OPENAPI

# 生成的目录结构
my-project/
├── build.gradle.kts
├── settings.gradle.kts
├── src/main/java/com/example/demo/
│   ├── Application.java
│   ├── entity/
│   ├── repository/
│   ├── service/
│   └── controller/
├── src/main/resources/
│   ├── application.yml
│   └── db/changelog.xml
└── README.md
```

### 2. 生成 CRUD 代码

```bash
# 生成 User 实体的 CRUD 代码（简化架构：Entity + Controller）
java -jar afg-framework-gradle-plugin.jar scaffold User \
    -f name:String,email:String(100),age:Integer \
    -p com.example.demo

# 生成的文件
# - UserEntity.java           (实体类)
# - UserController.java       (REST 控制器，直接使用 DataManager)
# - xxx_create_user_table.xml (Liquibase 迁移)
# - UserControllerTest.java   (控制器测试)

# 架构说明：
# - 无需 Repository 层：DataManager.entity(EntityClass) 提供所有 CRUD
# - 无需 Service 层：简单业务直接在 Controller 中操作 DataManager
# - Service 层仅在复杂业务逻辑时添加（使用 --service 参数）
```

### 3. DataManager 使用示例

```java
@RestController
@RequiredArgsConstructor
public class UserController {

    private final DataManager dataManager;

    // 查询单个
    @GetMapping("/{id}")
    public ResponseEntity<User> getById(@PathVariable Long id) {
        return dataManager.entity(UserEntity.class)
            .findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // 分页查询
    @GetMapping
    public Page<UserEntity> list(int page, int size) {
        return dataManager.entity(UserEntity.class)
            .findAll(Conditions.empty(), PageRequest.of(page, size));
    }

    // 条件查询
    @GetMapping("/search")
    public List<UserEntity> search(String keyword) {
        Condition condition = Conditions.builder()
            .like("name", "%" + keyword + "%")
            .build();
        return dataManager.entity(UserEntity.class).findAll(condition);
    }

    // 保存
    @PostMapping
    public UserEntity create(@RequestBody UserEntity entity) {
        return dataManager.entity(UserEntity.class).save(entity);
    }

    // 删除
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        dataManager.entity(UserEntity.class).deleteById(id);
    }
}
```

### 3. 字段类型支持

| 类型 | Java 类型 | SQL 类型 |
|------|-----------|----------|
| String | String | VARCHAR(255) |
| Text | String | TEXT |
| Integer | Integer | INT |
| Long | Long | BIGINT |
| Double | Double | DOUBLE |
| Decimal | BigDecimal | DECIMAL(19,4) |
| Boolean | Boolean | BOOLEAN |
| Date | LocalDate | DATE |
| Time | LocalTime | TIME |
| DateTime | LocalDateTime | TIMESTAMP |
| UUID | UUID | UUID |
| JSON | String | JSON |

## 命令详解

### init - 项目初始化

```bash
afg init <project-name> [options]

选项:
  -o, --output      输出目录 (默认: 当前目录)
  -p, --package     基础包名 (默认: com.example)
  -t, --type        项目类型: DATA, WEB, MICROSERVICE, LIBRARY (默认: DATA)
  -d, --database    数据库类型: H2, MYSQL, POSTGRESQL, ORACLE (默认: H2)
  -j, --java        Java 版本 (默认: 21)
  -f, --features    功能特性，逗号分隔:
                    LIQUIBASE, REDIS, SECURITY, VALIDATION, OPENAPI, ACTUATOR, TESTCONTAINERS

示例:
  afg init my-app -p com.example -d MYSQL -f LIQUIBASE,VALIDATION
  afg init order-service -t MICROSERVICE -d POSTGRESQL -f SECURITY,OPENAPI
```

### scaffold - 脚手架生成

```bash
afg scaffold <entity-name> [options]

选项:
  -f, --fields        字段定义，格式: name:Type,name:Type(length)
  -o, --output        输出目录 (默认: 当前目录)
  -p, --package       基础包名 (默认: com.example)
  -t, --table         表名 (默认: 根据实体名推断)
  -a, --api           API 路径 (默认: /api/{table})
  --lombok            使用 Lombok (默认: true)
  --validation        使用 Bean Validation (默认: true)
  --service           生成 Service 层 (默认: false，复杂业务时使用)
  --tests             生成测试代码 (默认: true)

架构说明:
  - 默认生成 Entity + Controller + Migration
  - Controller 直接注入 DataManager 进行 CRUD 操作
  - 无需 Repository 层
  - Service 层仅在复杂业务逻辑时添加

示例:
  # 简单 CRUD（默认）
  afg scaffold User -f name:String,email:String(100),age:Integer

  # 带复杂业务逻辑
  afg scaffold Order -f orderNo:String,amount:Decimal(10,2) --service

  # 自定义表名和 API 路径
  afg scaffold Product -f name:String,price:Double --table t_product --api /products
```

### generate-migration - 生成迁移脚本

```bash
afg generate-migration [options]

选项:
  --packages, -p    实体包路径，逗号分隔 (必需)
  --output, -o      输出文件路径 (必需)
  --author, -a      作者
  --check-db        是否比对数据库

示例:
  afg generate-migration --packages com.example.entity --output db/changelog.xml
```

### migrate - 执行数据库迁移

```bash
afg migrate [options]

选项:
  --jdbc-url        JDBC URL (必需)
  --username, -u    数据库用户名 (必需)
  --password, -p    数据库密码 (必需)
  --changelog, -c   ChangeLog 文件路径 (必需)
  --target          目标版本 (可选，默认最新)

示例:
  afg migrate --jdbc-url jdbc:mysql://localhost:3306/mydb \
      --username root --password secret \
      --changelog db/changelog.xml
```

### validate - 验证一致性

```bash
afg validate [options]

选项:
  --packages        实体包路径，逗号分隔 (必需)
  --jdbc-url        JDBC URL (必需)
  --username, -u    数据库用户名 (必需)
  --password        数据库密码 (必需)
  --fail-on-error   发现错误时退出 (默认: true)

示例:
  afg validate --packages com.example.entity \
      --jdbc-url jdbc:mysql://localhost:3306/mydb \
      --username root --password secret
```

### diff - 差异对比

```bash
afg diff [options]

选项:
  --packages        实体包路径，逗号分隔 (必需)
  --jdbc-url        JDBC URL (必需)
  --username, -u    数据库用户名 (必需)
  --password        数据库密码 (必需)
  --output, -o      输出文件 (可选，默认控制台)

示例:
  afg diff --packages com.example.entity \
      --jdbc-url jdbc:mysql://localhost:3306/mydb \
      --username root --password secret
```

### api-doc - 生成 API 文档

```bash
afg api-doc [options]

选项:
  --source, -s      源代码路径 (必需)
  --output, -o      输出文件路径 (必需)
  --format, -f      输出格式: json, yaml (默认: yaml)
  --package, -p     扫描的包名

示例:
  afg api-doc --source src/main/java --output openapi.yaml
```

## Gradle 插件配置

应用插件后，会自动配置以下内容：

### 自动配置项

| 配置项 | 说明 |
|--------|------|
| **核心依赖** | 根据模块类型自动添加框架依赖 |
| **Lombok** | 自动配置 Lombok 依赖和注解处理器 |
| **编译选项** | `-parameters`（保留参数名）、UTF-8 编码 |
| **源码目录** | 自动添加生成的源码目录 |
| **测试配置** | JUnit 5、AssertJ、JaCoCo 覆盖率 |
| **代码质量** | PMD 规则集、JaCoCo 报告 |

### 配置示例

```kotlin
plugins {
    id("io.github.afg-projects.plugin") version "1.0.0"
}

afg {
    // 模块类型（决定自动添加哪些依赖）
    // data: 添加 data-jdbc、data-liquibase
    // auth: 添加 auth 模块
    // storage: 添加 storage 模块
    // job: 添加 job 模块
    moduleType.set("data")

    // 框架版本
    frameworkVersion.set("1.0.0")

    // 代码特性
    useLombok.set(true)          // 自动配置 Lombok
    useJsr305.set(true)          // 自动配置 JSR-305 空安全注解
    useValidation.set(true)      // 自动配置 Bean Validation API

    // 启用代码生成（编译时自动执行 generateEntity）
    enableCodegen.set(false)

    // 迁移配置
    migration {
        entityPackages.set(listOf("com.example.entity"))
        changeLogFile.set("src/main/resources/db/changelog.xml")
        author.set("developer")
        checkDatabase.set(true)   // 比对数据库现有结构
        checkChangeLog.set(true)  // 比对历史 ChangeLog
    }

    // 逆向工程配置
    reverseEngineering {
        jdbcUrl.set("jdbc:mysql://localhost:3306/mydb")
        username.set("root")
        password.set("password")
        tables.set(listOf("user", "order"))
        basePackage.set("com.example.entity")
    }
}
```

### 自动添加的依赖

```kotlin
// 模块类型 = "data" 时自动添加：
implementation("io.github.afg-projects:afg-framework-core:1.0.0")
implementation("io.github.afg-projects:afg-framework-data-jdbc:1.0.0")
implementation("io.github.afg-projects:afg-framework-data-liquibase:1.0.0")

// Lombok（useLombok = true）
compileOnly("org.projectlombok:lombok:1.18.36")
annotationProcessor("org.projectlombok:lombok:1.18.36")

// Validation（useValidation = true）
implementation("jakarta.validation:jakarta.validation-api:3.1.0")

// 测试依赖
testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
testImplementation("org.assertj:assertj-core:3.27.3")
```

### 插件任务

```bash
# 显示框架配置信息
./gradlew afgInfo

# 生成配置文件模板（application.yml、changelog.xml）
./gradlew afgInitConfig

# 生成实体类
./gradlew generateEntity --entityName=User --tableName=t_user

# 从数据库逆向生成
./gradlew generateEntityFromDb \
    --jdbcUrl=jdbc:mysql://localhost:3306/mydb \
    --username=root --password=password \
    --basePackage=com.example.entity

# 生成迁移脚本
./gradlew generateMigration

# 执行数据库迁移
./gradlew dbMigrate \
    --jdbcUrl=jdbc:mysql://localhost:3306/mydb \
    --username=root --password=password
```

## 依赖要求

插件会自动添加框架核心依赖，无需手动配置。

如需额外依赖，可按需添加：

```kotlin
dependencies {
    // 已自动添加，无需重复配置
    // implementation("io.github.afg-projects:afg-framework-core:1.0.0")
    // implementation("io.github.afg-projects:afg-framework-data-jdbc:1.0.0")

    // 数据库驱动（根据实际数据库选择）
    runtimeOnly("com.mysql:mysql-connector-j")
    // runtimeOnly("org.postgresql:postgresql")
    // runtimeOnly("com.h2database:h2")

    // 可选：Spring Boot Web
    implementation("org.springframework.boot:spring-boot-starter-web")
}
```

## 许可证

Apache License 2.0