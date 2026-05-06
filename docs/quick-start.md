# 快速开始指南

本指南帮助你快速上手 AFG Framework。

## 环境要求

- Java 25+
- Spring Boot 4.0.5+
- Gradle 9.4+ 或 Maven 3.9+

## 第一步：添加依赖

**Gradle (Kotlin DSL):**

```kotlin
dependencies {
    implementation("io.github.afg-projects:afg-framework-spring-boot-starter:1.0.0")
    
    // 数据库驱动（根据你的数据库选择）
    runtimeOnly("org.postgresql:postgresql:42.7.4")
    // runtimeOnly("com.mysql:mysql-connector-j:9.1.0")
    // runtimeOnly("com.oracle.database.jdbc:ojdbc11:23.6.0.24.10")
}
```

**Maven:**

```xml
<dependencies>
    <dependency>
        <groupId>io.github.afg-projects</groupId>
        <artifactId>afg-framework-spring-boot-starter</artifactId>
        <version>1.0.0</version>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <version>42.7.4</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

## 第二步：配置数据源

`application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
```

## 第三步：定义实体

```java
package com.example.entity;

import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class User extends BaseEntity {
    
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String email;
    private UserStatus status = UserStatus.ACTIVE;
    
    public enum UserStatus {
        ACTIVE, DISABLED, LOCKED
    }
}
```

## 第四步：创建 Repository

```java
package com.example.repository;

import com.example.entity.User;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.EntityProxy;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {
    
    private final DataManager dataManager;
    
    private EntityProxy<User> entity() {
        return dataManager.entity(User.class);
    }
    
    public User save(User user) {
        return entity().save(user);
    }
    
    public Optional<User> findById(Long id) {
        return entity().findById(id);
    }
    
    public Optional<User> findByUsername(String username) {
        return entity().query()
            .where(Conditions.builder(User.class)
                .eq(User::getUsername, username)
                .build())
            .one();
    }
}
```

## 第五步：创建 Service

```java
package com.example.service;

import com.example.entity.User;
import com.example.repository.UserRepository;
import io.github.afgprojects.framework.data.core.exception.DuplicateEntityException;
import io.github.afgprojects.framework.data.core.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    @Transactional
    public User create(User user) {
        // 检查用户名是否已存在
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new DuplicateEntityException(User.class, "username", user.getUsername());
        }
        return userRepository.save(user);
    }
    
    public User getById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(User.class, id));
    }
    
    @Transactional
    public User update(Long id, User updates) {
        User user = getById(id);
        if (updates.getRealName() != null) {
            user.setRealName(updates.getRealName());
        }
        if (updates.getEmail() != null) {
            user.setEmail(updates.getEmail());
        }
        return userRepository.save(user);
    }
}
```

## 第六步：创建 Controller

```java
package com.example.controller;

import com.example.entity.User;
import com.example.service.UserService;
import io.github.afgprojects.framework.core.model.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @PostMapping
    public Result<User> create(@RequestBody User user) {
        return Result.success(userService.create(user));
    }
    
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }
    
    @PutMapping("/{id}")
    public Result<User> update(@PathVariable Long id, @RequestBody User user) {
        return Result.success(userService.update(id, user));
    }
}
```

## 第七步：启动应用

```java
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 下一步

- 阅读 [数据访问层文档](data/README.md) 了解更多高级特性
- 查看 [核心功能文档](core/README.md) 了解缓存、事件等功能
- 参考 [示例项目](../examples/) 获取完整示例代码
