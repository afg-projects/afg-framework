package io.github.afgprojects.framework.data.jdbc.security;

import io.github.afgprojects.framework.data.core.EntityProxy;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.jdbc.JdbcDataManager;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 安全测试基类
 * <p>
 * 提供测试数据库创建、数据初始化、完整性验证等通用功能。
 * </p>
 */
public abstract class SecurityTestBase {

    protected JdbcDataManager dataManager;
    protected DataSource dataSource;
    protected EntityProxy<SecurityTestUser> userProxy;
    protected long initialCount;

    @BeforeEach
    void setUpBase() {
        dataSource = createDataSource();
        dataManager = new JdbcDataManager(dataSource);
        createSecurityTestTables();
        insertTestData();
        userProxy = dataManager.entity(SecurityTestUser.class);
        initialCount = userProxy.count();
    }

    @AfterEach
    void tearDownBase() {
        dropSecurityTestTables();
    }

    /**
     * 创建 H2 数据源（PostgreSQL 兼容模式）
     */
    protected DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:securitytest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    /**
     * 创建安全测试表
     */
    protected void createSecurityTestTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE security_test_user (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(200),
                    password VARCHAR(200),
                    tenant_id VARCHAR(50),
                    dept_id VARCHAR(50),
                    create_time TIMESTAMP
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create security test tables", e);
        }
    }

    /**
     * 插入测试数据
     */
    protected void insertTestData() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO security_test_user (name, email, password, tenant_id, dept_id) VALUES ('alice', 'alice@test.com', 'pass123', 'tenant-a', 'dept-1')");
            stmt.execute("INSERT INTO security_test_user (name, email, password, tenant_id, dept_id) VALUES ('bob', 'bob@test.com', 'pass456', 'tenant-a', 'dept-2')");
            stmt.execute("INSERT INTO security_test_user (name, email, password, tenant_id, dept_id) VALUES ('charlie', 'charlie@test.com', 'pass789', 'tenant-b', 'dept-1')");
        } catch (Exception e) {
            throw new RuntimeException("Failed to insert test data", e);
        }
    }

    /**
     * 删除安全测试表
     */
    protected void dropSecurityTestTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS security_test_user");
        } catch (Exception ignored) {
        }
    }

    /**
     * 验证数据库完整性
     * <p>
     * 确保注入攻击没有破坏数据或插入恶意数据。
     * </p>
     */
    protected void assertDatabaseIntegrity() {
        // 验证表未被删除
        long currentCount = userProxy.count();
        assertThat(currentCount).isGreaterThanOrEqualTo(0);

        // 验证没有恶意用户被插入
        List<SecurityTestUser> malicious = userProxy.findAll(
            Conditions.eq("name", "hacker")
        );
        assertThat(malicious).isEmpty();
    }

    /**
     * 验证数据未变化
     */
    protected void assertDataUnchanged() {
        assertThat(userProxy.count()).isEqualTo(initialCount);
    }

    /**
     * 安全测试用户实体
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityTestUser {
        private Long id;
        private String name;
        private String email;
        private String password;
        private String tenantId;
        private String deptId;
        private java.time.LocalDateTime createTime;
    }
}
