package io.github.afgprojects.framework.core.gradle.extension

import org.gradle.api.provider.Property

/**
 * AFG 插件扩展配置（轻量版）。
 *
 * 使用方式：
 * <pre>
 * afg {
 *     springBootVersion.set("4.0.6")
 *     frameworkVersion.set("1.0.0-SNAPSHOT")
 *     standalone.set(true)
 *     useLombok.set(true)
 */
abstract class AfgExtension {

    /**
     * Spring Boot 版本。
     *
     * 用于 Spring Boot BOM（enforcedPlatform）和 Spring Boot Gradle Plugin。
     * 默认: 4.0.6
     */
    abstract val springBootVersion: Property<String>

    /**
     * 框架版本。
     *
     * 用于 AFG Framework BOM（platform）和框架核心依赖。
     * 默认: 1.0.0-SNAPSHOT
     */
    abstract val frameworkVersion: Property<String>

    /**
     * 是否独立部署。
     *
     * - true: 独立部署，自动应用 Spring Boot 插件，生成可执行 bootJar
     * - false: 聚合部署，作为普通 jar 被主应用依赖
     * 默认: true
     */
    abstract val standalone: Property<Boolean>

    /**
     * 是否使用 Lombok。
     *
     * 自动配置 Lombok 依赖和注解处理器（版本由 Spring Boot BOM 管理）。
     * 默认: true
     */
    abstract val useLombok: Property<Boolean>
}
