package io.github.afgprojects.framework.core.gradle.service

import java.nio.file.Path

/**
 * 项目初始化服务接口
 */
interface ProjectInitService {

    /**
     * 初始化新项目
     *
     * @param projectName 项目名称
     * @param outputPath 输出路径
     * @param options 初始化选项
     */
    fun init(projectName: String, outputPath: Path, options: ProjectInitOptions): InitResult
}

/**
 * 项目初始化选项
 */
data class ProjectInitOptions(
    val projectType: ProjectType = ProjectType.DATA,
    val basePackage: String = "com.example",
    val javaVersion: Int = 21,
    val springBootVersion: String = "4.0.5",
    val afgFrameworkVersion: String = "1.0.0",
    val databaseType: DatabaseType = DatabaseType.H2,
    val buildTool: BuildTool = BuildTool.GRADLE_KOTLIN,
    val features: Set<Feature> = emptySet()
)

/**
 * 项目类型
 */
enum class ProjectType {
    DATA,       // 数据访问模块
    WEB,        // Web 应用
    MICROSERVICE, // 微服务
    LIBRARY     // 库模块
}

/**
 * 数据库类型
 */
enum class DatabaseType(val driver: String, val urlTemplate: String) {
    H2("org.h2.Driver", "jdbc:h2:mem:testdb"),
    MYSQL("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/{db}"),
    POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/{db}"),
    ORACLE("oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@localhost:1521:{db}")
}

/**
 * 构建工具
 */
enum class BuildTool {
    GRADLE_KOTLIN,
    GRADLE_GROOVY,
    MAVEN
}

/**
 * 功能特性
 */
enum class Feature {
    LIQUIBASE,      // 数据库迁移
    REDIS,          // Redis 缓存
    SECURITY,       // Spring Security
    VALIDATION,     // Bean Validation
    OPENAPI,        // OpenAPI 文档
    ACTUATOR,       // Spring Actuator
    TESTCONTAINERS  // 测试容器
}

/**
 * 初始化结果
 */
data class InitResult(
    val success: Boolean,
    val projectPath: Path,
    val generatedFiles: List<Path>,
    val message: String
)