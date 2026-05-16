package io.github.afgprojects.framework.core.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import java.nio.file.Path
import kotlin.io.path.*

/**
 * 一键创建项目任务
 * <p>
 * 根据模板生成完整的项目结构，包括：
 * - build.gradle.kts / settings.gradle.kts
 * - 主应用类
 * - 配置文件
 * - 示例实体、控制器
 * - 数据库迁移脚本
 * - Docker 配置
 * <p>
 * 使用方式：
 * <pre>
 * # 在任意目录执行
 * gradle createProject --projectName=my-project --basePackage=com.example
 *
 * # 或使用完整参数
 * gradle createProject \
 *   --projectName=my-project \
 *   --basePackage=com.mycompany \
 *   --moduleType=data \
 *   --databaseType=mysql
 * </pre>
 */
@DisableCachingByDefault(because = "Creates new project")
abstract class CreateProjectTask : DefaultTask() {

    init {
        group = "afg"
        description = "一键创建基于 AFG Framework 的项目"
    }

    @get:Option(option = "name", description = "项目名称")
    @get:Input
    abstract val projectName: Property<String>

    @get:Option(option = "package", description = "基础包名")
    @get:Input
    abstract val basePackage: Property<String>

    @get:Option(option = "group", description = "项目 Group")
    @get:Input
    @get:Optional
    abstract val projectGroup: Property<String>

    @get:Option(option = "version", description = "项目版本")
    @get:Input
    @get:Optional
    abstract val projectVersion: Property<String>

    @get:Option(option = "springBoot", description = "Spring Boot 版本")
    @get:Input
    @get:Optional
    abstract val springBootVersion: Property<String>

    @get:Option(option = "framework", description = "AFG Framework 版本")
    @get:Input
    @get:Optional
    abstract val frameworkVersion: Property<String>

    @get:Option(option = "module", description = "模块类型: starter/data/integration")
    @get:Input
    @get:Optional
    abstract val moduleType: Property<String>

    @get:Option(option = "db", description = "数据库类型: h2/mysql/postgresql")
    @get:Input
    @get:Optional
    abstract val databaseType: Property<String>

    @get:Option(option = "no-example", description = "不生成示例代码")
    @get:Input
    @get:Optional
    abstract val generateExample: Property<Boolean>

    @get:Option(option = "no-docker", description = "不生成 Docker 配置")
    @get:Input
    @get:Optional
    abstract val generateDocker: Property<Boolean>

    @get:Option(option = "output", description = "输出目录")
    @get:OutputDirectory
    abstract val outputDirectory: org.gradle.api.file.DirectoryProperty

    init {
        projectName.convention("my-project")
        basePackage.convention("com.example")
        projectGroup.convention("com.example")
        projectVersion.convention("1.0.0")
        springBootVersion.convention("4.0.6")
        frameworkVersion.convention("1.0.0-SNAPSHOT")
        moduleType.convention("data")
        databaseType.convention("h2")
        generateExample.convention(true)
        generateDocker.convention(true)
        outputDirectory.convention(project.layout.projectDirectory)
    }

    @TaskAction
    fun create() {
        val name = projectName.get()
        val pkg = basePackage.get()
        val group = projectGroup.get()
        val version = projectVersion.get()
        val sbVersion = springBootVersion.get()
        val fwVersion = frameworkVersion.get()
        val modType = moduleType.get()
        val dbType = databaseType.get()
        val withExample = generateExample.get()
        val withDocker = generateDocker.get()

        val baseDir = outputDir().resolve(name)
        baseDir.createDirectories()

        logger.lifecycle("========================================")
        logger.lifecycle("创建项目: $name")
        logger.lifecycle("包名: $pkg")
        logger.lifecycle("模块类型: $modType")
        logger.lifecycle("数据库: $dbType")
        logger.lifecycle("输出目录: $baseDir")
        logger.lifecycle("========================================")

        // 1. 创建 Gradle 配置文件
        createGradleFiles(baseDir, name, group, version, sbVersion, fwVersion, modType, pkg)

        // 2. 创建源码目录结构
        val packagePath = pkg.replace('.', '/')
        val srcMainJava = baseDir.resolve("src/main/java/$packagePath")
        val srcMainResources = baseDir.resolve("src/main/resources")
        val srcTestJava = baseDir.resolve("src/test/java/$packagePath")
        srcMainJava.createDirectories()
        srcMainResources.createDirectories()
        srcTestJava.createDirectories()

        // 3. 创建主应用类
        createApplicationClass(srcMainJava, pkg, name)

        // 4. 创建配置文件
        createApplicationConfig(srcMainResources, name, dbType, modType)

        // 5. 创建数据库迁移目录
        createMigrationFiles(srcMainResources)

        // 6. 创建示例代码（可选）
        if (withExample) {
            createExampleCode(srcMainJava, srcMainResources, pkg, dbType)
        }

        // 7. 创建测试类
        createTestFiles(srcTestJava, pkg, name)

        // 8. 创建 Docker 配置（可选）
        if (withDocker) {
            createDockerFiles(baseDir, name, dbType)
        }

        // 9. 创建 README
        createReadme(baseDir, name, pkg, modType, dbType)

        // 10. 创建 .gitignore
        createGitignore(baseDir)

        logger.lifecycle("========================================")
        logger.lifecycle("✅ 项目创建成功！")
        logger.lifecycle("")
        logger.lifecycle("下一步：")
        logger.lifecycle("  cd $name")
        logger.lifecycle("  ./gradlew bootRun")
        logger.lifecycle("========================================")
    }

    private fun outputDir(): Path = outputDirectory.asFile.get().toPath()

    private fun createGradleFiles(
        baseDir: Path,
        name: String,
        group: String,
        version: String,
        sbVersion: String,
        fwVersion: String,
        modType: String,
        pkg: String
    ) {
        // build.gradle.kts
        val buildGradle = baseDir.resolve("build.gradle.kts")
        buildGradle.writeText("""
plugins {
    id("io.github.afg-projects.afg-plugin") version "$fwVersion"
}

group = "$group"
version = "$version"

afg {
    springBootVersion.set("$sbVersion")
    frameworkVersion.set("$fwVersion")
    moduleType.set("$modType")
    deploymentMode.set("module")
    useLombok.set(true)
    useValidation.set(true)

    migration {
        entityPackages.set(listOf("$pkg.entity"))
        author.set("afg")
        checkDatabase.set(true)
        checkChangeLog.set(true)
    }
}

// 如果需要发布到 Maven Central
// publishing {
//     publications {
//         create<MavenPublication>("mavenJava") {
//             from(components["java"])
//         }
//     }
// }

        """.trimIndent())

        // settings.gradle.kts
        val settingsGradle = baseDir.resolve("settings.gradle.kts")
        settingsGradle.writeText("""
rootProject.name = "$name"

pluginManagement {
    repositories {
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_NOT_CONFIGURED)
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

        """.trimIndent())

        // gradle.properties
        val gradleProperties = baseDir.resolve("gradle.properties")
        gradleProperties.writeText("""
# Gradle 配置
org.gradle.jvmargs=-Xmx2g -XX:+HeapDumpOnOutOfMemoryError
org.gradle.parallel=true
org.gradle.caching=true
kotlin.code.style=official

        """.trimIndent())

        // 创建 gradle wrapper
        val gradleDir = baseDir.resolve("gradle/wrapper")
        gradleDir.createDirectories()
        val gradleWrapperProps = gradleDir.resolve("gradle-wrapper.properties")
        gradleWrapperProps.writeText("""
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.14-bin.zip
networkTimeout=10000
validateDistributionUrl=true

        """.trimIndent())
    }

    private fun createApplicationClass(srcMainJava: Path, pkg: String, name: String) {
        val className = name.split("-", "_")
            .joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }
            .replaceFirstChar { it.uppercaseChar() } + "Application"

        val appFile = srcMainJava.resolve("${className}.java")
        appFile.writeText("""
package $pkg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * $name 主应用
 */
@SpringBootApplication
public class $className {

    public static void main(String[] args) {
        SpringApplication.run($className.class, args);
    }
}

        """.trimIndent())
    }

    private fun createApplicationConfig(
        srcMainResources: Path,
        name: String,
        dbType: String,
        modType: String
    ) {
        val (driver, url, username, password, dialect) = when (dbType.lowercase()) {
            "mysql" -> listOf(
                "com.mysql.cj.jdbc.Driver",
                "jdbc:mysql://localhost:3306/${name.replace("-", "_")}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                "root",
                "password",
                "org.hibernate.dialect.MySQLDialect"
            )
            "postgresql", "postgres" -> listOf(
                "org.postgresql.Driver",
                "jdbc:postgresql://localhost:5432/${name.replace("-", "_")}",
                "postgres",
                "password",
                "org.hibernate.dialect.PostgreSQLDialect"
            )
            else -> listOf(
                "org.h2.Driver",
                "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "sa",
                "",
                "org.hibernate.dialect.H2Dialect"
            )
        }

        val liquibaseEnabled = modType == "data"

        val appYml = srcMainResources.resolve("application.yml")
        appYml.writeText("""
# 应用配置
spring:
  application:
    name: $name

  # 数据源配置
  datasource:
    driver-class-name: $driver
    url: $url
    username: $username
    password: $password
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000

  # JPA 配置
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: $dialect
        format_sql: true

  # Liquibase 配置
  liquibase:
    enabled: $liquibaseEnabled
    change-log: classpath:db/changelog/changelog.xml

  # H2 控制台（仅开发环境）
  h2:
    console:
      enabled: ${dbType == "h2"}

# AFG Framework 配置
afg:
  data:
    # 数据权限配置
    data-scope:
      enabled: true
    # 租户隔离配置
    tenant:
      enabled: false
    # 软删除配置
    soft-delete:
      enabled: true
      field-name: deleted

# 日志配置
logging:
  level:
    root: INFO
    $pkg: DEBUG
    io.github.afgprojects: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

# Actuator 配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when_authorized

        """.trimIndent())

        // application-dev.yml
        val appDevYml = srcMainResources.resolve("application-dev.yml")
        appDevYml.writeText("""
# 开发环境配置
spring:
  jpa:
    show-sql: true
  h2:
    console:
      enabled: true

logging:
  level:
    $pkg: DEBUG
    org.springframework.web: DEBUG

        """.trimIndent())

        // application-prod.yml
        val appProdYml = srcMainResources.resolve("application-prod.yml")
        appProdYml.writeText("""
# 生产环境配置
spring:
  jpa:
    show-sql: false

logging:
  level:
    root: WARN
    $pkg: INFO

        """.trimIndent())
    }

    private fun createMigrationFiles(srcMainResources: Path) {
        val dbDir = srcMainResources.resolve("db/changelog")
        dbDir.createDirectories()

        // changelog.xml - 主文件
        val changelog = srcMainResources.resolve("db/changelog.xml")
        changelog.writeText("""
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.26.xsd">

    <!-- 包含所有迁移文件 -->
    <includeAll path="changelog/" relativeToChangelogFile="true"/>

</databaseChangeLog>

        """.trimIndent())

        // init.xml - 初始化文件
        val init = dbDir.resolve("init.xml")
        init.writeText("""
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.26.xsd">

    <!-- 初始化迁移脚本 -->
    <!-- 使用 ./gradlew generateMigration 从实体自动生成 -->

</databaseChangeLog>

        """.trimIndent())
    }

    private fun createExampleCode(srcMainJava: Path, srcMainResources: Path, pkg: String, dbType: String) {
        // 创建 User 实体示例
        val entityDir = srcMainJava.resolve("entity")
        entityDir.createDirectories()

        val userEntity = entityDir.resolve("User.java")
        userEntity.writeText("""
package $pkg.entity;

import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 用户实体（示例）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_user", indexes = {
    @Index(name = "idx_user_username", columnList = "username"),
    @Index(name = "idx_user_email", columnList = "email")
})
public class User extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "real_name", length = 50)
    private String realName;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "avatar", length = 255)
    private String avatar;

    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;
}

        """.trimIndent())

        // 创建 UserController 示例
        val controllerDir = srcMainJava.resolve("controller")
        controllerDir.createDirectories()

        val userController = controllerDir.resolve("UserController.java")
        userController.writeText("""
package $pkg.controller;

import $pkg.entity.User;
import io.github.afgprojects.framework.data.core.DataManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器（示例）
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final DataManager dataManager;

    @GetMapping("/{id}")
    public ResponseEntity<User> getById(@PathVariable Long id) {
        return dataManager.findById(User.class, id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<User> list() {
        return dataManager.findAll(User.class);
    }

    @PostMapping
    public User create(@RequestBody User user) {
        return dataManager.save(User.class, user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable Long id, @RequestBody User user) {
        return dataManager.findById(User.class, id)
            .map(existing -> {
                user.setId(id);
                return ResponseEntity.ok(dataManager.save(User.class, user));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        dataManager.deleteById(User.class, id);
        return ResponseEntity.noContent().build();
    }
}

        """.trimIndent())

        // 创建用户迁移脚本
        val dbDir = srcMainResources.resolve("db/changelog")
        val userMigration = dbDir.resolve("001_sys_user.xml")
        userMigration.writeText("""
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.26.xsd">

    <changeSet id="sys-user-init" author="afg">
        <createTable tableName="sys_user" remarks="用户表">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" primaryKeyName="pk_sys_user"/>
            </column>
            <column name="username" type="VARCHAR(50)" remarks="用户名">
                <constraints nullable="false" unique="true" uniqueConstraintName="uk_user_username"/>
            </column>
            <column name="password" type="VARCHAR(255)" remarks="密码">
                <constraints nullable="false"/>
            </column>
            <column name="real_name" type="VARCHAR(50)" remarks="真实姓名"/>
            <column name="email" type="VARCHAR(100)" remarks="邮箱"/>
            <column name="phone" type="VARCHAR(20)" remarks="手机号"/>
            <column name="avatar" type="VARCHAR(255)" remarks="头像"/>
            <column name="status" type="SMALLINT" defaultValueNumeric="1" remarks="状态"/>
            <column name="last_login_time" type="TIMESTAMP" remarks="最后登录时间"/>
            <column name="created_at" type="TIMESTAMP" defaultValueDate="CURRENT_TIMESTAMP" remarks="创建时间"/>
            <column name="updated_at" type="TIMESTAMP" defaultValueDate="CURRENT_TIMESTAMP" remarks="更新时间"/>
            <column name="deleted" type="BOOLEAN" defaultValueBoolean="false" remarks="是否删除"/>
        </createTable>

        <createIndex indexName="idx_user_username" tableName="sys_user">
            <column name="username"/>
        </createIndex>
        <createIndex indexName="idx_user_email" tableName="sys_user">
            <column name="email"/>
        </createIndex>

        <!-- 初始化管理员账户 -->
        <insert tableName="sys_user">
            <column name="username" value="admin"/>
            <column name="password" value="|2a|10|N.zmdr9k7uOCQv37Y7Ce.Tq8J3XZzZzZzZzZzZzZzZzZzZzZzZzZzZzZzZzZzZ"/>
            <column name="real_name" value="系统管理员"/>
            <column name="status" valueNumeric="1"/>
        </insert>
    </changeSet>

</databaseChangeLog>

        """.trimIndent())
    }

    private fun createTestFiles(srcTestJava: Path, pkg: String, name: String) {
        // 创建测试配置
        val testResources = srcTestJava.parent.parent.resolve("resources")
        testResources.createDirectories()

        val testYml = testResources.resolve("application-test.yml")
        testYml.writeText("""
spring:
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

  liquibase:
    enabled: false

        """.trimIndent())

        // 创建示例测试类
        val className = name.split("-", "_")
            .joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }
            .replaceFirstChar { it.uppercaseChar() } + "ApplicationTests"

        val testFile = srcTestJava.resolve("${className}.java")
        testFile.writeText("""
package $pkg;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 应用测试
 */
@SpringBootTest
@ActiveProfiles("test")
class $className {

    @Test
    void contextLoads() {
        // 验证 Spring 上下文加载成功
    }
}

        """.trimIndent())
    }

    private fun createDockerFiles(baseDir: Path, name: String, dbType: String) {
        // Dockerfile
        val dockerfile = baseDir.resolve("Dockerfile")
        dockerfile.writeText("""
# 构建阶段
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src src
RUN ./gradlew bootJar -x test

# 运行阶段
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

        """.trimIndent())

        // docker-compose.yml
        val dockerCompose = baseDir.resolve("docker-compose.yml")
        val dbService = when (dbType.lowercase()) {
            "mysql" -> """
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: password
      MYSQL_DATABASE: ${name.replace("-", "_")}
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

"""
            "postgresql", "postgres" -> """
  postgres:
    image: postgres:16
    environment:
      POSTGRES_PASSWORD: password
      POSTGRES_DB: ${name.replace("-", "_")}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

"""
            else -> ""
        }

        val volumes = when (dbType.lowercase()) {
            "mysql" -> "\nvolumes:\n  mysql_data:\n"
            "postgresql", "postgres" -> "\nvolumes:\n  postgres_data:\n"
            else -> ""
        }

        dockerCompose.writeText("""
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
    depends_on:${if (dbType != "h2") " - ${dbType.lowercase().let { if (it == "postgresql") "postgres" else it }}" else ""}
$dbService
$volumes
        """.trimIndent())

        // .dockerignore
        val dockerignore = baseDir.resolve(".dockerignore")
        dockerignore.writeText("""
.gradle
build
!build/libs/*.jar
*.log
*.md
.git
.gitignore

        """.trimIndent())
    }

    private fun createReadme(baseDir: Path, name: String, pkg: String, modType: String, dbType: String) {
        val readme = baseDir.resolve("README.md")
        readme.writeText("""
# $name

基于 AFG Framework 构建的项目。

## 技术栈

- Java 25
- Spring Boot 4
- AFG Framework
- Gradle 8.14
- Liquibase（数据库迁移）

## 快速开始

### 前置要求

- JDK 25+
- Gradle 8.14+（或使用 ./gradlew）

### 运行项目

```bash
# 开发模式
./gradlew bootRun

# 指定环境
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 构建项目

```bash
# 构建
./gradlew build

# 打包
./gradlew bootJar

# 运行 JAR
java -jar build/libs/$name-1.0.0.jar
```

### 数据库迁移

```bash
# 从实体生成迁移脚本
./gradlew generateMigration

# 执行迁移
./gradlew dbMigrate

# 查看迁移状态
./gradlew liquibaseStatus
```

### 测试

```bash
# 运行测试
./gradlew test

# 测试覆盖率
./gradlew jacocoTestReport
```

## 项目结构

```
src/
├── main/
│   ├── java/$pkg.replace('.', '/')/
│   │   ├── entity/          # 实体类
│   │   ├── controller/      # REST 控制器
│   │   ├── service/         # 业务服务（可选）
│   │   └── *Application.java
│   └── resources/
│       ├── application.yml
│       └── db/changelog/    # Liquibase 迁移脚本
└── test/
    └── java/$pkg.replace('.', '/')/
```

## API 文档

启动应用后访问：
- Swagger UI: http://localhost:8080/swagger-ui.html
- Actuator: http://localhost:8080/actuator

## Docker

```bash
# 构建镜像
docker build -t $name .

# 运行容器
docker run -p 8080:8080 $name

# 使用 docker-compose
docker-compose up -d
```

## 配置说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `spring.datasource.url` | 数据库连接 | ${if (dbType == "h2") "H2 内存数据库" else "$dbType 数据库"} |
| `afg.data.data-scope.enabled` | 数据权限 | true |
| `afg.data.tenant.enabled` | 多租户 | false |
| `afg.data.soft-delete.enabled` | 软删除 | true |

## 相关链接

- [AFG Framework 文档](https://github.com/afg-projects/afg-framework)
- [Spring Boot 文档](https://spring.io/projects/spring-boot)

        """.trimIndent())
    }

    private fun createGitignore(baseDir: Path) {
        val gitignore = baseDir.resolve(".gitignore")
        gitignore.writeText("""
# Gradle
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar

# IDE
.idea/
*.iml
*.ipr
*.iws
.vscode/
*.swp
*.swo

# Java
*.class
*.jar
*.war
*.ear
*.log
hs_err_pid*

# OS
.DS_Store
Thumbs.db

# 敏感配置
application-local.yml
application-secret.yml

        """.trimIndent())
    }
}
