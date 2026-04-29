package io.github.afgprojects.framework.core.gradle.extension

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * AFG 插件扩展配置
 */
interface AfgExtension {
    /**
     * 模块类型：core, data, auth, storage, job, registry, starter, example
     */
    val moduleType: Property<String>

    /**
     * 是否启用代码生成
     */
    val enableCodegen: Property<Boolean>

    /**
     * 迁移配置
     */
    val migration: MigrationExtension

    /**
     * 逆向工程配置
     */
    val reverseEngineering: ReverseEngineeringExtension
}

/**
 * 迁移配置扩展
 */
interface MigrationExtension {
    /**
     * 实体包路径列表
     */
    val entityPackages: ListProperty<String>

    /**
     * ChangeLog 文件路径
     */
    val changeLogFile: Property<String>

    /**
     * 作者
     */
    val author: Property<String>

    /**
     * 是否比对数据库
     */
    val checkDatabase: Property<Boolean>

    /**
     * 是否比对历史 ChangeLog
     */
    val checkChangeLog: Property<Boolean>
}

/**
 * 逆向工程配置扩展
 */
interface ReverseEngineeringExtension {
    /**
     * JDBC URL
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
     */
    val tables: ListProperty<String>

    /**
     * 生成的包名
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
