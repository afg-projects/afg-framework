package io.github.afgprojects.framework.core.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

/**
 * AFG 项目初始化任务。
 *
 * <p>生成项目初始文件，包括：
 * <ul>
 *   <li>Application.java - Spring Boot 启动类</li>
 *   <li>application.yml - 配置文件</li>
 *   <li>UserDetailsServiceImpl.java - 用户详情服务（认证服务器/单体应用）</li>
 *   <li>目录结构 - entity, controller, security</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 * <pre>
 * # 在 build.gradle.kts 中配置
 * afg {
 *     basePackage.set("com.example.myapp")
 *     securityMode.set("MONOLITH")  // AUTH_SERVER, RESOURCE_SERVER, MONOLITH
 *     databaseType.set("MYSQL")
 * }
 *
 * # 执行任务
 * ./gradlew afgInit
 * </pre>
 *
 * @since 1.1.0
 */
@CacheableTask
abstract class AfgInitTask : DefaultTask() {

    /**
     * 基础包名。
     * 默认使用项目 group。
     */
    @get:Input
    abstract val basePackage: Property<String>

    /**
     * 安全模式。
     * 可选值：AUTH_SERVER, RESOURCE_SERVER, MONOLITH。
     * 为空表示不生成安全相关文件。
     */
    @get:Input
    @get:Optional
    abstract val securityMode: Property<String>

    /**
     * 数据库类型。
     * 可选值：H2, MYSQL, POSTGRESQL, ORACLE。
     * 默认 H2。
     */
    @get:Input
    @get:Optional
    abstract val databaseType: Property<String>

    /**
     * 输出目录。
     * 默认项目根目录。
     */
    @get:OutputDirectory
    abstract val outputDir: Property<File>

    /**
     * 是否覆盖已存在的文件。
     * 默认 false。
     */
    @get:Input
    @get:Optional
    abstract val overwrite: Property<Boolean>

    @TaskAction
    fun generate() {
        val pkg = basePackage.get()
        val mode = securityMode.orNull
        val db = databaseType.getOrElse("H2")
        val output = outputDir.get().toPath()
        val forceOverwrite = overwrite.getOrElse(false)

        logger.lifecycle("正在初始化项目...")
        logger.lifecycle("  基础包名: $pkg")
        logger.lifecycle("  安全模式: ${mode ?: "无"}")
        logger.lifecycle("  数据库类型: $db")
        logger.lifecycle("  输出目录: $output")

        // 生成目录结构
        generateSourceStructure(output, pkg, mode, forceOverwrite)

        // 生成配置文件
        generateConfigFiles(output, pkg, mode, db, forceOverwrite)

        // 生成安全相关文件
        if (mode != null) {
            generateSecurityFiles(output, pkg, mode, forceOverwrite)
        }

        logger.lifecycle("")
        logger.lifecycle("✅ 项目初始化完成")
        logger.lifecycle("")
        logger.lifecycle("生成的文件:")
        logger.lifecycle("  - src/main/java/${pkg.replace('.', '/')}/Application.java")
        if (mode == "AUTH_SERVER" || mode == "MONOLITH") {
            logger.lifecycle("  - src/main/java/${pkg.replace('.', '/')}/security/UserDetailsServiceImpl.java")
        }
        logger.lifecycle("  - src/main/resources/application.yml")
        logger.lifecycle("")
        logger.lifecycle("下一步:")
        logger.lifecycle("  1. 根据实际环境修改 application.yml 中的数据库配置")
        if (mode == "AUTH_SERVER" || mode == "MONOLITH") {
            logger.lifecycle("  2. 实现 UserDetailsServiceImpl 中的用户加载逻辑")
            logger.lifecycle("  3. 配置 afg.security.auth-server.token.signing-key")
            logger.lifecycle("  4. 根据需要配置 oauth2、casbin、permission、tenant、security、audit 等模块")
        }
        if (mode == "RESOURCE_SERVER") {
            logger.lifecycle("  2. 配置 afg.security.resource-server.jwt.jwk-set-uri 或 issuer-uri")
            logger.lifecycle("  3. 配置 afg.security.resource-server.permission（远程权限校验）")
            logger.lifecycle("  4. 根据需要配置 tenant 解析策略")
        }
    }

    /**
     * 生成源码目录结构。
     */
    private fun generateSourceStructure(output: Path, pkg: String, mode: String?, overwrite: Boolean) {
        val pkgPath = pkg.replace('.', '/')
        val srcMain = output.resolve("src/main/java/$pkgPath")
        srcMain.createDirectories()

        // Application.java
        val appFile = srcMain.resolve("Application.java")
        if (!appFile.exists() || overwrite) {
            appFile.writeText(generateApplicationClass(pkg))
            logger.lifecycle("  生成: ${appFile.relativeTo(output)}")
        } else {
            logger.lifecycle("  跳过（已存在）: ${appFile.relativeTo(output)}")
        }

        // 创建目录结构
        listOf("entity", "controller").forEach { dir ->
            val dirPath = srcMain.resolve(dir)
            if (!dirPath.exists()) {
                dirPath.createDirectories()
                // 创建 package-info.java
                val packageInfo = dirPath.resolve("package-info.java")
                packageInfo.writeText("""
/**
 * ${dir.replaceFirstChar { it.uppercase() }} package
 */
package $pkg.$dir;

                """.trimIndent())
            }
        }

        // 如果是认证服务器，创建 security 目录
        if (mode == "AUTH_SERVER" || mode == "MONOLITH") {
            val securityDir = srcMain.resolve("security")
            if (!securityDir.exists()) {
                securityDir.createDirectories()
                val packageInfo = securityDir.resolve("package-info.java")
                packageInfo.writeText("""
/**
 * Security package
 */
package $pkg.security;

                """.trimIndent())
            }
        }
    }

    /**
     * 生成配置文件。
     */
    private fun generateConfigFiles(output: Path, pkg: String, mode: String?, db: String, overwrite: Boolean) {
        val resources = output.resolve("src/main/resources")
        resources.createDirectories()

        val appYml = resources.resolve("application.yml")
        if (!appYml.exists() || overwrite) {
            appYml.writeText(generateApplicationYml(pkg, mode, db))
            logger.lifecycle("  生成: ${appYml.relativeTo(output)}")
        } else {
            logger.lifecycle("  跳过（已存在）: ${appYml.relativeTo(output)}")
        }

        // 生成数据库迁移目录
        val dbDir = resources.resolve("db/changelog")
        dbDir.createDirectories()

        val changelogXml = resources.resolve("db/changelog.xml")
        if (!changelogXml.exists()) {
            changelogXml.writeText("""
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-5.0.xsd">

    <!-- 包含迁移文件 -->
    <include file="changelog/init.xml" relativeToChangelogFile="true"/>

</databaseChangeLog>

            """.trimIndent())
        }

        val initXml = dbDir.resolve("init.xml")
        if (!initXml.exists()) {
            initXml.writeText("""
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-5.0.xsd">

    <!-- 初始化迁移 -->

</databaseChangeLog>

            """.trimIndent())
        }
    }

    /**
     * 生成安全相关文件。
     */
    private fun generateSecurityFiles(output: Path, pkg: String, mode: String, overwrite: Boolean) {
        if (mode == "AUTH_SERVER" || mode == "MONOLITH") {
            val pkgPath = pkg.replace('.', '/')
            val securityDir = output.resolve("src/main/java/$pkgPath/security")
            securityDir.createDirectories()

            val userDetailsService = securityDir.resolve("UserDetailsServiceImpl.java")
            if (!userDetailsService.exists() || overwrite) {
                userDetailsService.writeText(generateUserDetailsService(pkg))
                logger.lifecycle("  生成: ${userDetailsService.relativeTo(output)}")
            } else {
                logger.lifecycle("  跳过（已存在）: ${userDetailsService.relativeTo(output)}")
            }
        }
    }

    /**
     * 生成 Application.java 内容。
     */
    private fun generateApplicationClass(pkg: String): String {
        return """
package $pkg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot Application
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

        """.trimIndent()
    }

    /**
     * 生成 application.yml 内容。
     */
    private fun generateApplicationYml(pkg: String, mode: String?, db: String): String {
        val appName = pkg.substringAfterLast('.')
        val dbConfig = getDatabaseConfig(db)

        return buildString {
            appendLine("spring:")
            appendLine("  application:")
            appendLine("    name: $appName")
            appendLine()
            appendLine("  datasource:")
            appendLine("    driver-class-name: ${dbConfig.driver}")
            appendLine("    url: ${dbConfig.url}")
            appendLine("    username: ${dbConfig.username}")
            appendLine("    password: ${dbConfig.password}")
            appendLine()
            appendLine("  liquibase:")
            appendLine("    enabled: true")
            appendLine("    change-log: classpath:db/changelog.xml")
            appendLine()

            // 安全配置
            if (mode != null) {
                appendLine("# AFG Security Configuration")
                appendLine("afg:")
                appendLine("  security:")
                when (mode) {
                    "AUTH_SERVER" -> {
                        appendLine("    auth-server:")
                        appendLine("      enabled: true")
                        appendLine("      token:")
                        appendLine("        issuer: https://auth.example.com")
                        appendLine("        signing-key: change-this-to-your-secret-key-at-least-256-bits")
                        appendLine("        access-token-ttl: 2h")
                        appendLine("        refresh-token-ttl: 7d")
                        appendLine("      login:")
                        appendLine("        enabled: true")
                        appendLine("        captcha-ttl: 5m")
                        appendLine("        captcha-length: 4")
                        appendLine("      oauth2:")
                        appendLine("        enabled: true")
                        appendLine("        authorization-code-ttl: 5m")
                        appendLine("      casbin:")
                        appendLine("        enabled: true")
                        appendLine("        model-type: rbac-domain")
                        appendLine("        policy-adapter-type: jdbc")
                        appendLine("      permission:")
                        appendLine("        enabled: true")
                        appendLine("        default-data-scope: ALL")
                        appendLine("      tenant:")
                        appendLine("        enabled: true")
                        appendLine("        strategies: TOKEN,HEADER,DOMAIN,DEFAULT")
                        appendLine("        default-tenant: default")
                        appendLine("        header-name: X-Tenant-Id")
                        appendLine("      security:")
                        appendLine("        enabled: true")
                        appendLine("        max-login-failures: 5")
                        appendLine("        lock-duration: 30m")
                        appendLine("      audit:")
                        appendLine("        enabled: true")
                    }
                    "RESOURCE_SERVER" -> {
                        appendLine("    resource-server:")
                        appendLine("      enabled: true")
                        appendLine("      jwt:")
                        appendLine("        enabled: true")
                        appendLine("        jwk-set-uri: https://auth.example.com/.well-known/jwks.json")
                        appendLine("        issuer-uri: https://auth.example.com")
                        appendLine("        cache-ttl: 5m")
                        appendLine("      permission:")
                        appendLine("        auth-server-url: http://auth-server:8080/auth-api/internal")
                        appendLine("        key-id: resource-server-1")
                        appendLine("        secret: shared-secret-key")
                        appendLine("      tenant:")
                        appendLine("        strategies: token,header")
                        appendLine("        header-name: X-Tenant-Id")
                        appendLine("        fail-if-unresolved: true")
                    }
                    "MONOLITH" -> {
                        appendLine("    auth-server:")
                        appendLine("      enabled: true")
                        appendLine("      token:")
                        appendLine("        issuer: https://app.example.com")
                        appendLine("        signing-key: change-this-to-your-secret-key-at-least-256-bits")
                        appendLine("        access-token-ttl: 2h")
                        appendLine("        refresh-token-ttl: 7d")
                        appendLine("      login:")
                        appendLine("        enabled: true")
                        appendLine("      oauth2:")
                        appendLine("        enabled: true")
                        appendLine("      casbin:")
                        appendLine("        enabled: true")
                        appendLine("        model-type: rbac-domain")
                        appendLine("      permission:")
                        appendLine("        enabled: true")
                        appendLine("      tenant:")
                        appendLine("        enabled: true")
                        appendLine("        strategies: TOKEN,HEADER,DOMAIN,DEFAULT")
                        appendLine("      security:")
                        appendLine("        enabled: true")
                        appendLine("      audit:")
                        appendLine("        enabled: true")
                        appendLine("    resource-server:")
                        appendLine("      enabled: true")
                        appendLine("      jwt:")
                        appendLine("        enabled: true")
                    }
                }
                appendLine()
            }

            appendLine("# Logging Configuration")
            appendLine("logging:")
            appendLine("  level:")
            appendLine("    root: INFO")
            appendLine("    $pkg: DEBUG")
        }
    }

    /**
     * 生成 UserDetailsServiceImpl.java 内容。
     */
    private fun generateUserDetailsService(pkg: String): String {
        return """
package $pkg.security;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 用户详情服务实现。
 *
 * <p>实现 AfgUserDetailsService 接口，提供用户加载逻辑。
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements AfgUserDetailsService {

    private final DataManager dataManager;

    @Override
    @NonNull
    public AfgUserDetails loadUserByUsername(@NonNull String username) {
        // TODO: 实现用户加载逻辑
        // 示例：
        // User user = dataManager.query(User.class)
        //     .where(Conditions.eq(User::getUsername, username))
        //     .one()
        //     .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        //
        // return AfgUserDetails.builder()
        //     .userId(user.getId().toString())
        //     .username(user.getUsername())
        //     .password(user.getPassword())
        //     .roles(user.getRoles())
        //     .permissions(user.getPermissions())
        //     .tenantId(user.getTenantId())
        //     .build();

        throw new UnsupportedOperationException("请实现 loadUserByUsername 方法");
    }

    @Override
    @NonNull
    public AfgUserDetails loadUserByUserId(@NonNull String userId) {
        // TODO: 实现用户加载逻辑
        // 示例：
        // User user = dataManager.findById(User.class, Long.parseLong(userId))
        //     .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
        // return loadUserByUsername(user.getUsername());

        throw new UnsupportedOperationException("请实现 loadUserByUserId 方法");
    }
}

        """.trimIndent()
    }

    /**
     * 获取数据库配置。
     */
    private fun getDatabaseConfig(db: String): DatabaseConfig {
        return when (db.uppercase()) {
            "MYSQL" -> DatabaseConfig(
                driver = "com.mysql.cj.jdbc.Driver",
                url = "jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=UTC",
                username = "root",
                password = "password"
            )
            "POSTGRESQL" -> DatabaseConfig(
                driver = "org.postgresql.Driver",
                url = "jdbc:postgresql://localhost:5432/mydb",
                username = "postgres",
                password = "password"
            )
            "ORACLE" -> DatabaseConfig(
                driver = "oracle.jdbc.OracleDriver",
                url = "jdbc:oracle:thin:@localhost:1521:orcl",
                username = "system",
                password = "password"
            )
            else -> DatabaseConfig(
                driver = "org.h2.Driver",
                url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                username = "sa",
                password = ""
            )
        }
    }

    /**
     * 数据库配置。
     */
    private data class DatabaseConfig(
        val driver: String,
        val url: String,
        val username: String,
        val password: String
    )
}