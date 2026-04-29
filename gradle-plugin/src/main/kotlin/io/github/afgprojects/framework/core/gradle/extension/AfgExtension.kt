package io.github.afgprojects.framework.core.gradle.extension

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * AFG 插件扩展配置
 *
 * 使用方式：
 * <pre>
 * afg {
 *     moduleType.set("data")
 *     frameworkVersion.set("1.0.0")
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
interface AfgExtension {
    /**
     * 模块类型：core, data, auth, storage, job, registry, starter
     *
     * 决定自动添加哪些框架依赖：
     * - data: core + data-jdbc + data-liquibase
     * - auth: core + auth
     * - storage: core + storage
     * - job: core + job
     * - registry: core + registry
     * - starter: 仅 core
     */
    val moduleType: Property<String>

    /**
     * 框架版本
     *
     * 格式: major.minor.patch[-qualifier]
     * 示例: 1.0.0, 1.0.0-SNAPSHOT
     */
    val frameworkVersion: Property<String>

    /**
     * 是否启用代码生成
     *
     * 启用后，编译时自动执行 generateEntity 任务
     */
    val enableCodegen: Property<Boolean>

    /**
     * 是否使用 Lombok
     *
     * 自动配置 Lombok 依赖和注解处理器
     */
    val useLombok: Property<Boolean>

    /**
     * 是否使用 JSR-305 空安全注解
     *
     * 自动配置 findbugs-jsr305 依赖
     */
    val useJsr305: Property<Boolean>

    /**
     * 是否使用 Bean Validation
     *
     * 自动配置 jakarta.validation-api 依赖
     */
    val useValidation: Property<Boolean>

    /**
     * 迁移配置
     *
     * 用于 generateMigration 任务
     */
    val migration: MigrationExtension

    /**
     * 逆向工程配置
     *
     * 用于 generateEntityFromDb 任务
     */
    val reverseEngineering: ReverseEngineeringExtension
}

/**
 * 迁移配置扩展
 */
interface MigrationExtension {
    /**
     * 实体包路径列表
     *
     * 示例: ["com.example.entity", "com.example.domain"]
     */
    val entityPackages: ListProperty<String>

    /**
     * ChangeLog 文件路径
     *
     * 默认: src/main/resources/db/changelog.xml
     */
    val changeLogFile: Property<String>

    /**
     * 作者
     *
     * 用于生成的 changeset author 字段
     */
    val author: Property<String>

    /**
     * 是否比对数据库
     *
     * 启用后，生成迁移时会比对数据库现有结构
     */
    val checkDatabase: Property<Boolean>

    /**
     * 是否比对历史 ChangeLog
     *
     * 启用后，生成迁移时会比对历史 ChangeLog
     */
    val checkChangeLog: Property<Boolean>
}

/**
 * 逆向工程配置扩展
 */
interface ReverseEngineeringExtension {
    /**
     * JDBC URL
     *
     * 示例: jdbc:mysql://localhost:3306/mydb
     */
    val jdbcUrl: Property<String>

    /**
     * 用户名
     */
    val username: Property<String>

    /**
     * 密码
     */
    val password: Property<String>

    /**
     * 表名列表（为空则读取所有表）
     *
     * 示例: ["user", "order", "product"]
     */
    val tables: ListProperty<String>

    /**
     * 生成的包名
     *
     * 默认: io.github.afgprojects.entity
     */
    val basePackage: Property<String>

    /**
     * 是否使用 Lombok
     */
    val useLombok: Property<Boolean>

    /**
     * 是否使用 JSR-305 空安全注解
     */
    val useJsr305: Property<Boolean>
}