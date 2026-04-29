package io.github.afgprojects.framework.core.gradle.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int
import io.github.afgprojects.framework.core.gradle.service.*
import io.github.afgprojects.framework.core.gradle.service.impl.DefaultProjectInitService
import io.github.afgprojects.framework.core.gradle.service.impl.DefaultScaffoldService
import java.nio.file.Paths

/**
 * AFG Framework CLI 入口
 */
class AfgCli : CliktCommand(name = "afg") {
    override fun help(context: Context) = "AFG Framework Command Line Tools"

    init {
        subcommands(
            InitCommand(),
            ScaffoldCommand(),
            GenerateEntityCommand(),
            ReverseEngineerCommand(),
            MigrateCommand(),
            GenerateMigrationCommand(),
            ValidateCommand(),
            DiffCommand(),
            ApiDocCommand()
        )
    }

    override fun run() = Unit
}

/**
 * 项目初始化命令
 */
class InitCommand : CliktCommand(name = "init") {
    override fun help(context: Context) = "创建新项目骨架"

    val projectName by argument(help = "项目名称")
    val output by option("-o", "--output", help = "输出目录").default(".")
    val basePackage by option("-p", "--package", help = "基础包名").default("com.example")
    val type by option("-t", "--type", help = "项目类型: DATA, WEB, MICROSERVICE, LIBRARY")
        .enum<ProjectType>(ignoreCase = true).default(ProjectType.DATA)
    val database by option("-d", "--database", help = "数据库类型: H2, MYSQL, POSTGRESQL, ORACLE")
        .enum<DatabaseType>(ignoreCase = true).default(DatabaseType.H2)
    val javaVersion by option("-j", "--java", help = "Java 版本").int().default(21)
    val features by option("-f", "--features", help = "功能特性，逗号分隔: LIQUIBASE,REDIS,SECURITY,VALIDATION,OPENAPI,ACTUATOR,TESTCONTAINERS")
        .split(",").default(emptyList())

    override fun run() {
        val outputPath = java.nio.file.Paths.get(output).toAbsolutePath()

        echo("正在初始化项目: $projectName")
        echo("输出目录: $outputPath")
        echo("基础包名: $basePackage")
        echo("项目类型: $type")
        echo("数据库: $database")
        echo("Java 版本: $javaVersion")
        echo("功能特性: ${features.ifEmpty { "无" }}")

        val featureSet = features.mapNotNull {
            try { Feature.valueOf(it.uppercase()) } catch (e: Exception) { null }
        }.toSet()

        val options = ProjectInitOptions(
            projectType = type,
            basePackage = basePackage,
            javaVersion = javaVersion,
            databaseType = database,
            features = featureSet
        )

        val service = DefaultProjectInitService()
        val result = service.init(projectName, outputPath, options)

        if (result.success) {
            echo("\n✅ ${result.message}")
            echo("\n生成的文件:")
            result.generatedFiles.forEach { echo("  - ${it.fileName}") }
            echo("\n下一步:")
            echo("  cd $projectName")
            echo("  ./gradlew bootRun")
        } else {
            echo("\n❌ ${result.message}")
        }
    }
}

/**
 * 脚手架命令
 */
class ScaffoldCommand : CliktCommand(name = "scaffold") {
    override fun help(context: Context) = "生成 CRUD 代码骨架（Entity + Controller + Migration）"

    val entityName by argument(help = "实体名称")
    val fields by option("-f", "--fields", help = "字段定义，格式: name:Type,name:Type(length)").required()
    val output by option("-o", "--output", help = "输出目录").default(".")
    val basePackage by option("-p", "--package", help = "基础包名").default("com.example")
    val tableName by option("-t", "--table", help = "表名（默认根据实体名推断）")
    val apiPath by option("-a", "--api", help = "API 路径（默认 /api/{table}）")
    val useLombok by option("--lombok", help = "使用 Lombok").boolean().default(true)
    val useValidation by option("--validation", help = "使用 Bean Validation").boolean().default(true)
    val generateService by option("--service", help = "生成 Service 层（复杂业务时使用）").boolean().default(false)
    val generateTests by option("--tests", help = "生成测试代码").boolean().default(true)

    override fun run() {
        val outputPath = java.nio.file.Paths.get(output).toAbsolutePath()
        val fieldList = fields.split(",").map { FieldDefinition.parse(it.trim()) }

        echo("正在生成脚手架: $entityName")
        echo("字段: ${fieldList.joinToString { "${it.name}:${it.type.name}" }}")
        echo("输出目录: $outputPath")
        echo("架构: Entity + Controller (使用 DataManager)")

        val options = ScaffoldOptions(
            basePackage = basePackage,
            useLombok = useLombok,
            useValidation = useValidation,
            generateTests = generateTests,
            generateService = generateService,
            tableName = tableName,
            apiPath = apiPath
        )

        val service = DefaultScaffoldService()
        val result = service.generate(entityName, fieldList, outputPath, options)

        if (result.success) {
            echo("\n✅ ${result.message}")
            echo("\n生成的文件:")
            result.entityFile?.let { echo("  - Entity: ${it.toFile().name}") }
            result.controllerFile?.let { echo("  - Controller: ${it.toFile().name}") }
            result.serviceFile?.let { echo("  - Service: ${it.toFile().name}") }
            result.migrationFile?.let { echo("  - Migration: ${it.toFile().name}") }
            result.testFiles.forEach { echo("  - Test: ${it.toFile().name}") }
            echo("\n说明:")
            echo("  - Controller 直接注入 DataManager 进行 CRUD 操作")
            echo("  - 无需 Repository 层，DataManager.entity(EntityClass) 提供所有 CRUD")
            echo("  - Service 层仅在复杂业务逻辑时添加")
        } else {
            echo("\n❌ ${result.message}")
        }
    }
}

/**
 * 实体代码生成命令
 */
class GenerateEntityCommand : CliktCommand(name = "generate-entity") {
    override fun help(context: Context) = "从 YAML/JSON 配置生成实体类"

    val config by option("-c", "--config", help = "配置文件路径 (YAML/JSON)").required()
    val output by option("-o", "--output", help = "输出目录").required()

    override fun run() {
        echo("Generating entities from: $config")
        echo("Output directory: $output")
        // TODO: 实现实体生成逻辑
    }
}

/**
 * 逆向工程命令
 */
class ReverseEngineerCommand : CliktCommand(name = "reverse-engineer") {
    override fun help(context: Context) = "从数据库逆向生成实体类"

    val jdbcUrl by option("--jdbc-url", help = "JDBC URL").required()
    val username by option("--username", "-u", help = "数据库用户名").required()
    val password by option("--password", "-p", help = "数据库密码").required()
    val tables by option("--tables", "-t", help = "表名列表，逗号分隔（可选，默认全部）")
    val output by option("--output", "-o", help = "输出目录").required()
    val basePackage by option("--package", help = "基础包名")

    override fun run() {
        echo("Connecting to database: $jdbcUrl")
        echo("Output directory: $output")
        // TODO: 实现逆向工程逻辑
    }
}

/**
 * 数据库迁移命令
 */
class MigrateCommand : CliktCommand(name = "migrate") {
    override fun help(context: Context) = "执行数据库迁移"

    val jdbcUrl by option("--jdbc-url", help = "JDBC URL").required()
    val username by option("--username", "-u", help = "数据库用户名").required()
    val password by option("--password", "-p", help = "数据库密码").required()
    val changeLogFile by option("--changelog", "-c", help = "ChangeLog 文件路径").required()
    val targetVersion by option("--target", help = "目标版本（可选）")

    override fun run() {
        echo("Executing migration: $changeLogFile")
        echo("Target version: ${targetVersion ?: "latest"}")
        // TODO: 实现迁移逻辑
    }
}

/**
 * 迁移脚本生成命令
 */
class GenerateMigrationCommand : CliktCommand(name = "generate-migration") {
    override fun help(context: Context) = "从实体类生成迁移脚本"

    val entityPackages by option("--packages", "-p", help = "实体包路径，逗号分隔").required()
    val output by option("--output", "-o", help = "输出文件路径").required()
    val author by option("--author", "-a", help = "作者")
    val checkDatabase by option("--check-db", help = "是否比对数据库").boolean()

    override fun run() {
        echo("Scanning entity packages: $entityPackages")
        echo("Output file: $output")
        // TODO: 实现迁移生成逻辑
    }
}

/**
 * 验证命令
 */
class ValidateCommand : CliktCommand(name = "validate") {
    override fun help(context: Context) = "验证实体类与数据库一致性"

    val entityPackages by option("--packages", help = "实体包路径，逗号分隔").required()
    val jdbcUrl by option("--jdbc-url", help = "JDBC URL").required()
    val username by option("--username", "-u", help = "数据库用户名").required()
    val password by option("--password", help = "数据库密码").required()
    val failOnError by option("--fail-on-error", help = "发现错误时退出").boolean().default(true)

    override fun run() {
        echo("Validating entity packages: $entityPackages")
        echo("Database: $jdbcUrl")
        // TODO: 实现验证逻辑
    }
}

/**
 * 差异对比命令
 */
class DiffCommand : CliktCommand(name = "diff") {
    override fun help(context: Context) = "对比实体与数据库差异"

    val entityPackages by option("--packages", help = "实体包路径，逗号分隔").required()
    val jdbcUrl by option("--jdbc-url", help = "JDBC URL").required()
    val username by option("--username", "-u", help = "数据库用户名").required()
    val password by option("--password", help = "数据库密码").required()
    val output by option("-o", "--output", help = "输出文件（可选，默认控制台）")

    override fun run() {
        echo("Comparing entities with database: $jdbcUrl")
        // TODO: 实现差异对比逻辑
    }
}

/**
 * API 文档生成命令
 */
class ApiDocCommand : CliktCommand(name = "api-doc") {
    override fun help(context: Context) = "生成 OpenAPI 文档"

    val sourcePath by option("-s", "--source", help = "源代码路径").required()
    val output by option("-o", "--output", help = "输出文件路径").required()
    val format by option("-f", "--format", help = "输出格式: json, yaml").default("yaml")
    val basePackage by option("-p", "--package", help = "扫描的包名")

    override fun run() {
        echo("Generating API documentation from: $sourcePath")
        echo("Output: $output")
        echo("Format: $format")
        // TODO: 实现 API 文档生成逻辑
    }
}

fun main(args: Array<String>) = AfgCli().main(args)