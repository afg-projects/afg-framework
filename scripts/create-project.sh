#!/bin/bash
#
# AFG Framework 项目脚手架
# 一键创建基于 AFG Framework 的项目
#
# 使用方式:
#   curl -fsSL https://raw.githubusercontent.com/afg-projects/afg-framework/main/scripts/create-project.sh | bash -s -- --name=my-project
#
# 或:
#   ./create-project.sh --name=my-project --package=com.example --db=mysql
#

set -e

# 默认配置
PROJECT_NAME="my-project"
BASE_PACKAGE="com.example"
PROJECT_GROUP="com.example"
PROJECT_VERSION="1.0.0"
SPRING_BOOT_VERSION="4.0.6"
FRAMEWORK_VERSION="1.0.0-SNAPSHOT"
MODULE_TYPE="data"
DATABASE_TYPE="h2"
GENERATE_EXAMPLE="true"
OUTPUT_DIR="."

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印帮助信息
print_help() {
    echo "AFG Framework 项目脚手架"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  --name=NAME           项目名称 (默认: my-project)"
    echo "  --package=PACKAGE     基础包名 (默认: com.example)"
    echo "  --group=GROUP         项目 Group (默认: com.example)"
    echo "  --version=VERSION     项目版本 (默认: 1.0.0)"
    echo "  --spring-boot=VERSION Spring Boot 版本 (默认: 4.0.6)"
    echo "  --framework=VERSION   AFG Framework 版本 (默认: 1.0.0-SNAPSHOT)"
    echo "  --module=TYPE         模块类型: starter/data/integration (默认: data)"
    echo "  --db=TYPE             数据库类型: h2/mysql/postgresql (默认: h2)"
    echo "  --no-example          不生成示例代码"
    echo "  --output=DIR          输出目录 (默认: 当前目录)"
    echo "  --help                显示帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 --name=my-app --package=com.mycompany --db=mysql"
    echo ""
    echo "快速开始:"
    echo "  curl -fsSL https://raw.githubusercontent.com/afg-projects/afg-framework/main/scripts/create-project.sh | bash"
}

# 解析参数
parse_args() {
    for arg in "$@"; do
        case $arg in
            --name=*)
                PROJECT_NAME="${arg#*=}"
                shift
                ;;
            --package=*)
                BASE_PACKAGE="${arg#*=}"
                PROJECT_GROUP="${arg#*=}"
                shift
                ;;
            --group=*)
                PROJECT_GROUP="${arg#*=}"
                shift
                ;;
            --version=*)
                PROJECT_VERSION="${arg#*=}"
                shift
                ;;
            --spring-boot=*)
                SPRING_BOOT_VERSION="${arg#*=}"
                shift
                ;;
            --framework=*)
                FRAMEWORK_VERSION="${arg#*=}"
                shift
                ;;
            --module=*)
                MODULE_TYPE="${arg#*=}"
                shift
                ;;
            --db=*)
                DATABASE_TYPE="${arg#*=}"
                shift
                ;;
            --no-example)
                GENERATE_EXAMPLE="false"
                shift
                ;;
            --output=*)
                OUTPUT_DIR="${arg#*=}"
                shift
                ;;
            --help)
                print_help
                exit 0
                ;;
            *)
                echo -e "${RED}未知参数: $arg${NC}"
                print_help
                exit 1
                ;;
        esac
    done
}

# 创建目录
create_directories() {
    local BASE_DIR="$OUTPUT_DIR/$PROJECT_NAME"
    local PACKAGE_PATH=$(echo "$BASE_PACKAGE" | tr '.' '/')

    mkdir -p "$BASE_DIR/gradle/wrapper"
    mkdir -p "$BASE_DIR/src/main/java/$PACKAGE_PATH/entity"
    mkdir -p "$BASE_DIR/src/main/java/$PACKAGE_PATH/controller"
    mkdir -p "$BASE_DIR/src/main/resources/db/changelog"
    mkdir -p "$BASE_DIR/src/test/java/$PACKAGE_PATH"
    mkdir -p "$BASE_DIR/src/test/resources"

    echo "$BASE_DIR"
}

# 生成 build.gradle.kts
generate_build_gradle() {
    local BASE_DIR="$1"

    cat > "$BASE_DIR/build.gradle.kts" << EOF
plugins {
    id("io.github.afg-projects.afg-plugin") version "$FRAMEWORK_VERSION"
}

group = "$PROJECT_GROUP"
version = "$PROJECT_VERSION"

afg {
    springBootVersion.set("$SPRING_BOOT_VERSION")
    frameworkVersion.set("$FRAMEWORK_VERSION")
    moduleType.set("$MODULE_TYPE")
    deploymentMode.set("module")
    useLombok.set(true)
    useValidation.set(true)

    migration {
        entityPackages.set(listOf("$BASE_PACKAGE.entity"))
        author.set("afg")
        checkDatabase.set(true)
        checkChangeLog.set(true)
    }
}
EOF
}

# 生成 settings.gradle.kts
generate_settings_gradle() {
    local BASE_DIR="$1"

    cat > "$BASE_DIR/settings.gradle.kts" << EOF
rootProject.name = "$PROJECT_NAME"

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
EOF
}

# 生成 gradle.properties
generate_gradle_properties() {
    local BASE_DIR="$1"

    cat > "$BASE_DIR/gradle.properties" << EOF
org.gradle.jvmargs=-Xmx2g -XX:+HeapDumpOnOutOfMemoryError
org.gradle.parallel=true
org.gradle.caching=true
kotlin.code.style=official
EOF
}

# 生成 gradle-wrapper.properties
generate_gradle_wrapper() {
    local BASE_DIR="$1"

    cat > "$BASE_DIR/gradle/wrapper/gradle-wrapper.properties" << EOF
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.14-bin.zip
networkTimeout=10000
validateDistributionUrl=true
EOF
}

# 生成主应用类
generate_application() {
    local BASE_DIR="$1"
    local PACKAGE_PATH=$(echo "$BASE_PACKAGE" | tr '.' '/')
    local CLASS_NAME=$(echo "$PROJECT_NAME" | sed 's/-\|_/\n/g' | sed 's/\b\(.\)/\u\1/g' | tr -d '\n')Application

    cat > "$BASE_DIR/src/main/java/$PACKAGE_PATH/${CLASS_NAME}.java" << EOF
package $BASE_PACKAGE;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * $PROJECT_NAME 主应用
 */
@SpringBootApplication
public class ${CLASS_NAME} {

    public static void main(String[] args) {
        SpringApplication.run(${CLASS_NAME}.class, args);
    }
}
EOF
}

# 生成 application.yml
generate_application_yml() {
    local BASE_DIR="$1"

    # 根据数据库类型设置配置
    local DRIVER="org.h2.Driver"
    local URL="jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
    local USERNAME="sa"
    local PASSWORD=""
    local H2_CONSOLE="true"

    if [ "$DATABASE_TYPE" = "mysql" ]; then
        DRIVER="com.mysql.cj.jdbc.Driver"
        URL="jdbc:mysql://localhost:3306/${PROJECT_NAME//-/_}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
        USERNAME="root"
        PASSWORD="password"
        H2_CONSOLE="false"
    elif [ "$DATABASE_TYPE" = "postgresql" ]; then
        DRIVER="org.postgresql.Driver"
        URL="jdbc:postgresql://localhost:5432/${PROJECT_NAME//-/_}"
        USERNAME="postgres"
        PASSWORD="password"
        H2_CONSOLE="false"
    fi

    cat > "$BASE_DIR/src/main/resources/application.yml" << EOF
spring:
  application:
    name: $PROJECT_NAME

  datasource:
    driver-class-name: $DRIVER
    url: $URL
    username: $USERNAME
    password: $PASSWORD
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/changelog.xml

  h2:
    console:
      enabled: $H2_CONSOLE

afg:
  data:
    data-scope:
      enabled: true
    tenant:
      enabled: false
    soft-delete:
      enabled: true
      field-name: deleted

logging:
  level:
    root: INFO
    $BASE_PACKAGE: DEBUG
EOF
}

# 生成 Liquibase 配置
generate_liquibase() {
    local BASE_DIR="$1"

    # changelog.xml
    cat > "$BASE_DIR/src/main/resources/db/changelog.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.26.xsd">

    <includeAll path="changelog/" relativeToChangelogFile="true"/>

</databaseChangeLog>
EOF

    # init.xml
    cat > "$BASE_DIR/src/main/resources/db/changelog/init.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.26.xsd">

    <!-- 使用 ./gradlew generateMigration 从实体自动生成迁移脚本 -->

</databaseChangeLog>
EOF
}

# 生成示例代码
generate_example() {
    local BASE_DIR="$1"
    local PACKAGE_PATH=$(echo "$BASE_PACKAGE" | tr '.' '/')

    # User Entity
    cat > "$BASE_DIR/src/main/java/$PACKAGE_PATH/entity/User.java" << EOF
package $BASE_PACKAGE.entity;

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
    @Index(name = "idx_user_username", columnList = "username")
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

    @Column(name = "status")
    private Integer status = 1;
}
EOF

    # UserController
    cat > "$BASE_DIR/src/main/java/$PACKAGE_PATH/controller/UserController.java" << EOF
package $BASE_PACKAGE.controller;

import $BASE_PACKAGE.entity.User;
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        dataManager.deleteById(User.class, id);
        return ResponseEntity.noContent().build();
    }
}
EOF

    # Migration
    cat > "$BASE_DIR/src/main/resources/db/changelog/001_sys_user.xml" << EOF
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
            <column name="status" type="SMALLINT" defaultValueNumeric="1" remarks="状态"/>
            <column name="created_at" type="TIMESTAMP" defaultValueDate="CURRENT_TIMESTAMP"/>
            <column name="updated_at" type="TIMESTAMP" defaultValueDate="CURRENT_TIMESTAMP"/>
            <column name="deleted" type="BOOLEAN" defaultValueBoolean="false"/>
        </createTable>

        <createIndex indexName="idx_user_username" tableName="sys_user">
            <column name="username"/>
        </createIndex>
    </changeSet>

</databaseChangeLog>
EOF
}

# 生成 .gitignore
generate_gitignore() {
    local BASE_DIR="$1"

    cat > "$BASE_DIR/.gitignore" << EOF
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar
.idea/
*.iml
*.log
.DS_Store
application-local.yml
EOF
}

# 生成 README.md
generate_readme() {
    local BASE_DIR="$1"

    cat > "$BASE_DIR/README.md" << EOF
# $PROJECT_NAME

基于 AFG Framework 构建的项目。

## 快速开始

\`\`\`bash
# 运行项目
./gradlew bootRun

# 构建项目
./gradlew build

# 生成迁移脚本
./gradlew generateMigration

# 执行数据库迁移
./gradlew dbMigrate
\`\`\`

## API

启动后访问: http://localhost:8080/api/users

## 相关链接

- [AFG Framework](https://github.com/afg-projects/afg-framework)
EOF
}

# 主函数
main() {
    parse_args "$@"

    echo -e "${BLUE}========================================${NC}"
    echo -e "${GREEN}AFG Framework 项目脚手架${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    echo -e "项目名称:    ${YELLOW}$PROJECT_NAME${NC}"
    echo -e "包名:        ${YELLOW}$BASE_PACKAGE${NC}"
    echo -e "模块类型:    ${YELLOW}$MODULE_TYPE${NC}"
    echo -e "数据库:      ${YELLOW}$DATABASE_TYPE${NC}"
    echo -e "框架版本:    ${YELLOW}$FRAMEWORK_VERSION${NC}"
    echo ""

    local BASE_DIR="$OUTPUT_DIR/$PROJECT_NAME"

    if [ -d "$BASE_DIR" ]; then
        echo -e "${RED}错误: 目录 $BASE_DIR 已存在${NC}"
        exit 1
    fi

    echo -e "${BLUE}创建项目结构...${NC}"
    create_directories "$BASE_DIR"

    echo -e "${BLUE}生成 Gradle 配置...${NC}"
    generate_build_gradle "$BASE_DIR"
    generate_settings_gradle "$BASE_DIR"
    generate_gradle_properties "$BASE_DIR"
    generate_gradle_wrapper "$BASE_DIR"

    echo -e "${BLUE}生成应用代码...${NC}"
    generate_application "$BASE_DIR"
    generate_application_yml "$BASE_DIR"
    generate_liquibase "$BASE_DIR"

    if [ "$GENERATE_EXAMPLE" = "true" ]; then
        echo -e "${BLUE}生成示例代码...${NC}"
        generate_example "$BASE_DIR"
    fi

    echo -e "${BLUE}生成其他文件...${NC}"
    generate_gitignore "$BASE_DIR"
    generate_readme "$BASE_DIR"

    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}✅ 项目创建成功！${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "下一步:"
    echo -e "  ${YELLOW}cd $PROJECT_NAME${NC}"
    echo -e "  ${YELLOW}./gradlew bootRun${NC}"
    echo ""
}

main "$@"
