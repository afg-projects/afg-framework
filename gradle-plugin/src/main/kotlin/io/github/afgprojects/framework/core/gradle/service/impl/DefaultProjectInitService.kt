package io.github.afgprojects.framework.core.gradle.service.impl

import io.github.afgprojects.framework.core.gradle.service.*
import java.nio.file.Path
import kotlin.io.path.*

/**
 * 项目初始化服务实现
 */
class DefaultProjectInitService : ProjectInitService {

    override fun init(projectName: String, outputPath: Path, options: ProjectInitOptions): InitResult {
        try {
            val projectDir = outputPath.resolve(projectName)

            if (projectDir.exists()) {
                return InitResult(false, projectDir, emptyList(), "目录已存在: $projectDir")
            }

            projectDir.createDirectories()
            val generatedFiles = mutableListOf<Path>()

            // 生成构建文件
            generatedFiles.addAll(generateBuildFiles(projectDir, projectName, options))

            // 生成源代码目录结构
            generatedFiles.addAll(generateSourceStructure(projectDir, options))

            // 生成配置文件
            generatedFiles.addAll(generateConfigFiles(projectDir, options))

            // 生成数据库迁移目录
            if (Feature.LIQUIBASE in options.features) {
                generatedFiles.addAll(generateMigrationStructure(projectDir))
            }

            // 生成 README
            generatedFiles.add(generateReadme(projectDir, projectName, options))

            return InitResult(
                success = true,
                projectPath = projectDir,
                generatedFiles = generatedFiles,
                message = "项目初始化成功: $projectDir\n生成了 ${generatedFiles.size} 个文件"
            )
        } catch (e: Exception) {
            return InitResult(false, outputPath, emptyList(), "初始化失败: ${e.message}")
        }
    }

    private fun generateBuildFiles(projectDir: Path, projectName: String, options: ProjectInitOptions): List<Path> {
        return when (options.buildTool) {
            BuildTool.GRADLE_KOTLIN -> listOf(
                generateBuildGradleKts(projectDir, projectName, options),
                generateSettingsGradleKts(projectDir, projectName),
                generateGradleWrapper(projectDir)
            )
            BuildTool.GRADLE_GROOVY -> listOf(
                generateBuildGradle(projectDir, projectName, options),
                generateSettingsGradle(projectDir, projectName),
                generateGradleWrapper(projectDir)
            )
            BuildTool.MAVEN -> listOf(
                generatePomXml(projectDir, projectName, options)
            )
        }
    }

    private fun generateBuildGradleKts(projectDir: Path, projectName: String, options: ProjectInitOptions): Path {
        val file = projectDir.resolve("build.gradle.kts")
        val content = buildString {
            appendLine("plugins {")
            appendLine("    id(\"org.springframework.boot\") version \"${options.springBootVersion}\"")
            appendLine("    id(\"io.spring.dependency-management\") version \"1.1.7\"")
            appendLine("    id(\"java\")")
            if (Feature.LIQUIBASE in options.features) {
                appendLine("    id(\"io.github.afg-projects.plugin\") version \"${options.afgFrameworkVersion}\"")
            }
            appendLine("}")
            appendLine()
            appendLine("group = \"${options.basePackage}\"")
            appendLine("version = \"0.0.1-SNAPSHOT\"")
            appendLine()
            appendLine("java {")
            appendLine("    sourceCompatibility = JavaVersion.VERSION_${options.javaVersion}")
            appendLine("}")
            appendLine()
            appendLine("configurations {")
            appendLine("    compileOnly {")
            appendLine("        extendsFrom(configurations.annotationProcessor.get())")
            appendLine("    }")
            appendLine("}")
            appendLine()
            appendLine("repositories {")
            appendLine("    mavenCentral()")
            appendLine("}")
            appendLine()
            appendLine("dependencies {")
            appendLine("    // AFG Framework")
            appendLine("    implementation(\"io.github.afg-projects:afg-framework-core:${options.afgFrameworkVersion}\")")
            appendLine("    implementation(\"io.github.afg-projects:afg-framework-data-jdbc:${options.afgFrameworkVersion}\")")
            if (Feature.LIQUIBASE in options.features) {
                appendLine("    implementation(\"io.github.afg-projects:afg-framework-data-liquibase:${options.afgFrameworkVersion}\")")
            }
            appendLine()
            appendLine("    // Spring Boot")
            appendLine("    implementation(\"org.springframework.boot:spring-boot-starter-data-jdbc\")")
            if (Feature.SECURITY in options.features) {
                appendLine("    implementation(\"org.springframework.boot:spring-boot-starter-security\")")
            }
            if (Feature.VALIDATION in options.features) {
                appendLine("    implementation(\"org.springframework.boot:spring-boot-starter-validation\")")
            }
            if (Feature.ACTUATOR in options.features) {
                appendLine("    implementation(\"org.springframework.boot:spring-boot-starter-actuator\")")
            }
            if (Feature.OPENAPI in options.features) {
                appendLine("    implementation(\"org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.0\")")
            }
            appendLine()
            appendLine("    // Database")
            when (options.databaseType) {
                DatabaseType.MYSQL -> {
                    appendLine("    runtimeOnly(\"com.mysql:mysql-connector-j\")")
                }
                DatabaseType.POSTGRESQL -> {
                    appendLine("    runtimeOnly(\"org.postgresql:postgresql\")")
                }
                DatabaseType.ORACLE -> {
                    appendLine("    runtimeOnly(\"com.oracle.database.jdbc:ojdbc11\")")
                }
                DatabaseType.H2 -> {
                    appendLine("    runtimeOnly(\"com.h2database:h2\")")
                }
            }
            appendLine()
            appendLine("    // Lombok")
            appendLine("    compileOnly(\"org.projectlombok:lombok\")")
            appendLine("    annotationProcessor(\"org.projectlombok:lombok\")")
            appendLine()
            appendLine("    // Test")
            appendLine("    testImplementation(\"org.springframework.boot:spring-boot-starter-test\")")
            if (Feature.TESTCONTAINERS in options.features) {
                appendLine("    testImplementation(\"org.testcontainers:junit-jupiter\")")
                appendLine("    testImplementation(\"org.testcontainers:${options.databaseType.name.lowercase()}\")")
            }
            appendLine("}")
            appendLine()
            appendLine("tasks.withType<Test> {")
            appendLine("    useJUnitPlatform()")
            appendLine("}")
        }
        file.writeText(content)
        return file
    }

    private fun generateSettingsGradleKts(projectDir: Path, projectName: String): Path {
        val file = projectDir.resolve("settings.gradle.kts")
        file.writeText("""
rootProject.name = "$projectName"

            """.trimIndent())
        return file
    }

    private fun generateBuildGradle(projectDir: Path, projectName: String, options: ProjectInitOptions): Path {
        val file = projectDir.resolve("build.gradle")
        val content = buildString {
            appendLine("plugins {")
            appendLine("    id 'org.springframework.boot' version '${options.springBootVersion}'")
            appendLine("    id 'io.spring.dependency-management' version '1.1.7'")
            appendLine("    id 'java'")
            appendLine("}")
            appendLine()
            appendLine("group = '${options.basePackage}'")
            appendLine("version = '0.0.1-SNAPSHOT'")
            appendLine()
            appendLine("java {")
            appendLine("    sourceCompatibility = '${options.javaVersion}'")
            appendLine("}")
            appendLine()
            appendLine("repositories {")
            appendLine("    mavenCentral()")
            appendLine("}")
            appendLine()
            appendLine("dependencies {")
            appendLine("    implementation 'org.springframework.boot:spring-boot-starter-data-jdbc'")
            appendLine("    implementation 'io.github.afg-projects:afg-framework-core:${options.afgFrameworkVersion}'")
            appendLine("    testImplementation 'org.springframework.boot:spring-boot-starter-test'")
            appendLine("}")
        }
        file.writeText(content)
        return file
    }

    private fun generateSettingsGradle(projectDir: Path, projectName: String): Path {
        val file = projectDir.resolve("settings.gradle")
        file.writeText("rootProject.name = '$projectName'\n")
        return file
    }

    private fun generateGradleWrapper(projectDir: Path): Path {
        val gradleDir = projectDir.resolve("gradle/wrapper")
        gradleDir.createDirectories()

        val wrapperProps = gradleDir.resolve("gradle-wrapper.properties")
        wrapperProps.writeText("""
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.0-bin.zip
networkTimeout=10000
validateDistributionUrl=true

            """.trimIndent())

        return wrapperProps
    }

    private fun generatePomXml(projectDir: Path, projectName: String, options: ProjectInitOptions): Path {
        val file = projectDir.resolve("pom.xml")
        val content = buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            appendLine("<project xmlns=\"http://maven.apache.org/POM/4.0.0\">")
            appendLine("    <modelVersion>4.0.0</modelVersion>")
            appendLine()
            appendLine("    <parent>")
            appendLine("        <groupId>org.springframework.boot</groupId>")
            appendLine("        <artifactId>spring-boot-starter-parent</artifactId>")
            appendLine("        <version>${options.springBootVersion}</version>")
            appendLine("    </parent>")
            appendLine()
            appendLine("    <groupId>${options.basePackage}</groupId>")
            appendLine("    <artifactId>$projectName</artifactId>")
            appendLine("    <version>0.0.1-SNAPSHOT</version>")
            appendLine()
            appendLine("    <properties>")
            appendLine("        <java.version>${options.javaVersion}</java.version>")
            appendLine("    </properties>")
            appendLine()
            appendLine("    <dependencies>")
            appendLine("        <dependency>")
            appendLine("            <groupId>org.springframework.boot</groupId>")
            appendLine("            <artifactId>spring-boot-starter-data-jdbc</artifactId>")
            appendLine("        </dependency>")
            appendLine("        <dependency>")
            appendLine("            <groupId>io.github.afg-projects</groupId>")
            appendLine("            <artifactId>afg-framework-core</artifactId>")
            appendLine("            <version>${options.afgFrameworkVersion}</version>")
            appendLine("        </dependency>")
            appendLine("        <dependency>")
            appendLine("            <groupId>org.springframework.boot</groupId>")
            appendLine("            <artifactId>spring-boot-starter-test</artifactId>")
            appendLine("            <scope>test</scope>")
            appendLine("        </dependency>")
            appendLine("    </dependencies>")
            appendLine()
            appendLine("    <build>")
            appendLine("        <plugins>")
            appendLine("            <plugin>")
            appendLine("                <groupId>org.springframework.boot</groupId>")
            appendLine("                <artifactId>spring-boot-maven-plugin</artifactId>")
            appendLine("            </plugin>")
            appendLine("        </plugins>")
            appendLine("    </build>")
            appendLine("</project>")
        }
        file.writeText(content)
        return file
    }

    private fun generateSourceStructure(projectDir: Path, options: ProjectInitOptions): List<Path> {
        val files = mutableListOf<Path>()
        val packagePath = options.basePackage.replace('.', '/')

        val mainJava = projectDir.resolve("src/main/java/$packagePath")
        mainJava.createDirectories()

        // 生成 Application 主类
        val appFile = mainJava.resolve("Application.java")
        appFile.writeText(generateApplicationClass(options))
        files.add(appFile)

        // 生成目录结构
        listOf("entity", "repository", "service", "controller").forEach { dir ->
            val dirPath = mainJava.resolve(dir)
            dirPath.createDirectories()

            // 生成 package-info.java
            val packageInfo = dirPath.resolve("package-info.java")
            packageInfo.writeText("""
/**
 * ${dir.replaceFirstChar { it.uppercase() }} package
 */
package ${options.basePackage}.$dir;

            """.trimIndent())
            files.add(packageInfo)
        }

        // 生成测试目录
        val testJava = projectDir.resolve("src/test/java/$packagePath")
        testJava.createDirectories()

        return files
    }

    private fun generateApplicationClass(options: ProjectInitOptions): String {
        return """
package ${options.basePackage};

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

    private fun generateConfigFiles(projectDir: Path, options: ProjectInitOptions): List<Path> {
        val files = mutableListOf<Path>()
        val resources = projectDir.resolve("src/main/resources")
        resources.createDirectories()

        // application.yml
        val appYml = resources.resolve("application.yml")
        appYml.writeText(generateApplicationYml(options))
        files.add(appYml)

        // application-dev.yml
        val devYml = resources.resolve("application-dev.yml")
        devYml.writeText("""
spring:
  datasource:
    url: ${options.databaseType.urlTemplate.replace("{db}", "devdb")}
    username: dev
    password: dev

logging:
  level:
    ${options.basePackage}: DEBUG

        """.trimIndent())
        files.add(devYml)

        // .gitignore
        val gitignore = projectDir.resolve(".gitignore")
        gitignore.writeText("""
# Gradle
.gradle/
build/

# IDE
.idea/
*.iml
.vscode/

# OS
.DS_Store
Thumbs.db

# Logs
logs/
*.log

        """.trimIndent())
        files.add(gitignore)

        return files
    }

    private fun generateApplicationYml(options: ProjectInitOptions): String {
        return buildString {
            appendLine("spring:")
            appendLine("  application:")
            appendLine("    name: ${options.basePackage.substringAfterLast('.')}")
            appendLine()
            appendLine("  datasource:")
            appendLine("    driver-class-name: ${options.databaseType.driver}")
            appendLine("    url: ${options.databaseType.urlTemplate.replace("{db}", "testdb")}")
            appendLine("    username: sa")
            appendLine("    password: ")
            appendLine()
            if (Feature.LIQUIBASE in options.features) {
                appendLine("  liquibase:")
                appendLine("    enabled: true")
                appendLine("    change-log: classpath:db/changelog.xml")
                appendLine()
            }
            if (Feature.ACTUATOR in options.features) {
                appendLine("management:")
                appendLine("  endpoints:")
                appendLine("    web:")
                appendLine("      exposure:")
                appendLine("        include: health,info,metrics")
                appendLine()
            }
            appendLine("logging:")
            appendLine("  level:")
            appendLine("    root: INFO")
            appendLine("    ${options.basePackage}: INFO")
        }
    }

    private fun generateMigrationStructure(projectDir: Path): List<Path> {
        val files = mutableListOf<Path>()
        val dbDir = projectDir.resolve("src/main/resources/db")
        dbDir.createDirectories()

        // changelog.xml
        val changelog = dbDir.resolve("changelog.xml")
        changelog.writeText("""
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.26.xsd">

    <!-- Include generated changesets here -->
    <include file="db/changelog/init.xml" relativeToChangelogFile="true"/>

</databaseChangeLog>

        """.trimIndent())
        files.add(changelog)

        // init.xml
        val init = dbDir.resolve("changelog/init.xml")
        init.parent.createDirectories()
        init.writeText("""
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.26.xsd">

    <!-- Initial schema -->

</databaseChangeLog>

        """.trimIndent())
        files.add(init)

        return files
    }

    private fun generateReadme(projectDir: Path, projectName: String, options: ProjectInitOptions): Path {
        val file = projectDir.resolve("README.md")
        file.writeText("""
# $projectName

基于 AFG Framework 构建的 Spring Boot 应用。

## 技术栈

- Java ${options.javaVersion}
- Spring Boot ${options.springBootVersion}
- AFG Framework ${options.afgFrameworkVersion}
- Database: ${options.databaseType.name}

## 快速开始

### 构建项目

```bash
./gradlew build
```

### 运行应用

```bash
./gradlew bootRun
```

### 运行测试

```bash
./gradlew test
```

## 项目结构

```
src/
├── main/
│   ├── java/${options.basePackage.replace('.', '/')}
│   │   ├── Application.java
│   │   ├── entity/
│   │   ├── repository/
│   │   ├── service/
│   │   └── controller/
│   └── resources/
│       ├── application.yml
│       └── db/
│           └── changelog.xml
└── test/
    └── java/${options.basePackage.replace('.', '/')}
```

## AFG Framework 插件命令

```bash
# 生成迁移脚本
./gradlew generateMigration --entityPackages=${options.basePackage}.entity

# 执行数据库迁移
./gradlew dbMigrate --jdbcUrl=jdbc:h2:mem:testdb

# 从数据库逆向生成实体
./gradlew generateEntityFromDb --jdbcUrl=jdbc:h2:mem:testdb --basePackage=${options.basePackage}.entity
```

        """.trimIndent())
        return file
    }
}