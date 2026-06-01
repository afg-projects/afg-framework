package io.github.afgprojects.framework.core.gradle.extension

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * AFG 插件扩展配置
 *
 * 使用方式：
 * <pre>
 * afg {
 *     springBootVersion.set("4.0.6")
 *     frameworkVersion.set("1.0.0-SNAPSHOT")
 *     moduleType.set("data")
 *     standalone.set(true)  // 独立部署或聚合部署
 *     useLombok.set(true)
 *     useValidation.set(true)
 *
 *     // 安全模式配置
 *     basePackage.set("com.example.myapp")
 *     securityMode.set("MONOLITH")  // AUTH_SERVER, RESOURCE_SERVER, MONOLITH
 *     databaseType.set("MYSQL")
 *
 *     migration {
 *         entityPackages.set(listOf("com.example.entity"))
 *         changeLogFile.set("src/main/resources/db/changelog.xml")
 *     }
 * }
 * </pre>
 *
 * JSON Schema: https://afg-projects.github.io/afg-framework/schema/afg-config-schema.json
 */
abstract class AfgExtension @Inject constructor(
    private val migrationExt: MigrationExtension,
    private val reverseEngineeringExt: ReverseEngineeringExtension
) {
    /**
     * Spring Boot 版本
     *
     * 格式: major.minor.patch
     * 示例: 4.0.5
     * 默认: 4.0.5
     */
    abstract val springBootVersion: Property<String>

    /**
     * 是否独立部署
     *
     * - true: 独立部署，生成可执行 bootJar
     * - false: 聚合部署，作为普通 jar 被主应用依赖
     */
    abstract val standalone: Property<Boolean>

    /**
     * 框架版本
     *
     * 格式: major.minor.patch[-qualifier]
     * 示例: 1.0.0, 1.0.0-SNAPSHOT
     */
    abstract val frameworkVersion: Property<String>

    /**
     * Spring AI 版本
     *
     * 格式: major.minor.patch[-qualifier]
     * 示例: 2.0.0-M7
     * 默认: 2.0.0-M7
     */
    abstract val springAiVersion: Property<String>

    /**
     * 是否启用代码生成
     *
     * 启用后，编译时自动执行 generateEntity 任务
     */
    abstract val enableCodegen: Property<Boolean>

    /**
     * 是否使用 Lombok
     *
     * 自动配置 Lombok 依赖和注解处理器（版本由 Spring Boot BOM 管理）
     */
    abstract val useLombok: Property<Boolean>

    /**
     * 基础包名
     *
     * 用于 afgInit 任务生成代码。
     * 默认使用项目 group。
     */
    abstract val basePackage: Property<String>

    /**
     * 安全模式
     *
     * 用于 afgInit 任务生成安全相关代码。
     * 可选值：
     * - AUTH_SERVER: 认证服务器
     * - RESOURCE_SERVER: 资源服务器
     * - MONOLITH: 单体应用（认证+资源）
     * - 空: 不生成安全相关代码
     */
    abstract val securityMode: Property<String>

    /**
     * 数据库类型
     *
     * 用于 afgInit 任务生成数据库配置。
     * 可选值：H2, MYSQL, POSTGRESQL, ORACLE
     * 默认：H2
     */
    abstract val databaseType: Property<String>

    /**
     * 迁移配置
     *
     * 用于 generateMigration 任务
     */
    val migration: MigrationExtension get() = migrationExt

    /**
     * 逆向工程配置
     *
     * 用于 generateEntityFromDb 任务
     */
    val reverseEngineering: ReverseEngineeringExtension get() = reverseEngineeringExt
}

/**
 * 迁移配置扩展
 */
abstract class MigrationExtension {
    /**
     * 实体包路径列表
     *
     * 示例: ["com.example.entity", "com.example.domain"]
     */
    abstract val entityPackages: ListProperty<String>

    /**
     * ChangeLog 文件路径
     *
     * 默认: src/main/resources/db/changelog.xml
     */
    abstract val changeLogFile: Property<String>

    /**
     * 作者
     *
     * 用于生成的 changeset author 字段
     */
    abstract val author: Property<String>

    /**
     * 是否比对数据库
     *
     * 启用后，生成迁移时会比对数据库现有结构
     */
    abstract val checkDatabase: Property<Boolean>

    /**
     * 是否比对历史 ChangeLog
     *
     * 启用后，生成迁移时会比对历史 ChangeLog
     */
    abstract val checkChangeLog: Property<Boolean>
}

/**
 * 逆向工程配置扩展
 */
abstract class ReverseEngineeringExtension {
    /**
     * JDBC URL
     *
     * 示例: jdbc:mysql://localhost:3306/mydb
     */
    abstract val jdbcUrl: Property<String>

    /**
     * 用户名
     */
    abstract val username: Property<String>

    /**
     * 密码
     */
    abstract val password: Property<String>

    /**
     * 表名列表（为空则读取所有表）
     *
     * 示例: ["user", "order", "product"]
     */
    abstract val tables: ListProperty<String>

    /**
     * 生成的包名
     *
     * 默认: io.github.afgprojects.entity
     */
    abstract val basePackage: Property<String>

    /**
     * 是否使用 Lombok
     */
    abstract val useLombok: Property<Boolean>

    /**
     * 是否使用 JSR-305 空安全注解
     */
    abstract val useJsr305: Property<Boolean>
}