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
     * 模块类型：starter, data, integration
     *
     * 决定自动添加哪些框架依赖：
     * - starter: 仅核心依赖
     * - data: 核心 + data-jdbc + data-liquibase
     * - integration: 核心 + spring-boot-starter
     */
    abstract val moduleType: Property<String>

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
     * 是否使用 JSR-305 空安全注解
     *
     * 自动配置 findbugs-jsr305 依赖
     */
    abstract val useJsr305: Property<Boolean>

    /**
     * 是否使用 Bean Validation
     *
     * 自动配置 jakarta.validation-api 依赖（版本由 Spring Boot BOM 管理）
     */
    abstract val useValidation: Property<Boolean>

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