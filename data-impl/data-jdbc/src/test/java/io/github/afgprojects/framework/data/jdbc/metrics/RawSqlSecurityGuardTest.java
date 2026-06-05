package io.github.afgprojects.framework.data.jdbc.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RawSqlSecurityGuard 纯单元测试
 *
 * @see RawSqlSecurityGuard
 */
@DisplayName("RawSqlSecurityGuard")
class RawSqlSecurityGuardTest {

    @Nested
    @DisplayName("MODERATE 模式")
    class ModerateMode {

        private final RawSqlSecurityGuard guard = new RawSqlSecurityGuard(RawSqlSecurityGuard.Mode.MODERATE);

        @Test
        @DisplayName("SELECT 通过")
        void shouldPass_whenSelect() {
            assertThatNoException()
                    .isThrownBy(() -> guard.check("SELECT * FROM sys_user", "TestCaller"));
        }

        @Test
        @DisplayName("INSERT 通过")
        void shouldPass_whenInsert() {
            assertThatNoException()
                    .isThrownBy(() -> guard.check("INSERT INTO sys_user (username) VALUES ('admin')", "TestCaller"));
        }

        @Test
        @DisplayName("UPDATE 通过")
        void shouldPass_whenUpdate() {
            assertThatNoException()
                    .isThrownBy(() -> guard.check("UPDATE sys_user SET status = 0 WHERE id = 1", "TestCaller"));
        }

        @Test
        @DisplayName("DELETE 通过")
        void shouldPass_whenDelete() {
            assertThatNoException()
                    .isThrownBy(() -> guard.check("DELETE FROM sys_user WHERE id = 1", "TestCaller"));
        }

        @Test
        @DisplayName("WITH (CTE) 通过")
        void shouldPass_whenWithCte() {
            assertThatNoException()
                    .isThrownBy(() -> guard.check("WITH active_users AS (SELECT * FROM sys_user WHERE status = 1) SELECT * FROM active_users", "TestCaller"));
        }

        @Test
        @DisplayName("DROP TABLE 拒绝")
        void shouldReject_whenDropTable() {
            assertThatThrownBy(() -> guard.check("DROP TABLE sys_user", "TestCaller"))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("DDL operations are not allowed")
                    .hasMessageContaining("DROP");
        }

        @Test
        @DisplayName("ALTER TABLE 拒绝")
        void shouldReject_whenAlterTable() {
            assertThatThrownBy(() -> guard.check("ALTER TABLE sys_user ADD COLUMN phone VARCHAR(20)", "TestCaller"))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("DDL operations are not allowed")
                    .hasMessageContaining("ALTER");
        }

        @Test
        @DisplayName("CREATE TABLE 拒绝")
        void shouldReject_whenCreateTable() {
            assertThatThrownBy(() -> guard.check("CREATE TABLE sys_log (id BIGINT PRIMARY KEY)", "TestCaller"))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("DDL operations are not allowed")
                    .hasMessageContaining("CREATE");
        }

        @Test
        @DisplayName("TRUNCATE TABLE 拒绝")
        void shouldReject_whenTruncateTable() {
            assertThatThrownBy(() -> guard.check("TRUNCATE TABLE sys_user", "TestCaller"))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("DDL operations are not allowed")
                    .hasMessageContaining("TRUNCATE");
        }

        @Test
        @DisplayName("GRANT 拒绝")
        void shouldReject_whenGrant() {
            assertThatThrownBy(() -> guard.check("GRANT SELECT ON sys_user TO admin", "TestCaller"))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("DDL operations are not allowed")
                    .hasMessageContaining("GRANT");
        }

        @Test
        @DisplayName("REVOKE 拒绝")
        void shouldReject_whenRevoke() {
            assertThatThrownBy(() -> guard.check("REVOKE SELECT ON sys_user FROM admin", "TestCaller"))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("DDL operations are not allowed")
                    .hasMessageContaining("REVOKE");
        }

        @Test
        @DisplayName("多语句注入拒绝")
        void shouldReject_whenMultiStatementInjection() {
            assertThatThrownBy(() -> guard.check("SELECT * FROM sys_user; DROP TABLE sys_user", "TestCaller"))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("multi-statement SQL is not allowed");
        }

        @Test
        @DisplayName("空白 SQL 不检查")
        void shouldPass_whenBlankSql() {
            assertThatNoException()
                    .isThrownBy(() -> guard.check("   ", "TestCaller"));
        }

        @Test
        @DisplayName("null SQL 不检查")
        void shouldPass_whenNullSql() {
            assertThatNoException()
                    .isThrownBy(() -> guard.check(null, "TestCaller"));
        }

        @Test
        @DisplayName("分号后无关键字不拒绝")
        void shouldPass_whenSemicolonWithoutFollowingKeyword() {
            assertThatNoException()
                    .isThrownBy(() -> guard.check("SELECT * FROM sys_user;", "TestCaller"));
        }
    }

    @Nested
    @DisplayName("STRICT 模式")
    class StrictMode {

        private final RawSqlSecurityGuard guard = new RawSqlSecurityGuard(RawSqlSecurityGuard.Mode.STRICT);

        @Test
        @DisplayName("SELECT 通过")
        void shouldPass_whenSelect() {
            assertThatNoException()
                    .isThrownBy(() -> guard.check("SELECT * FROM sys_user", "TestCaller"));
        }

        @Test
        @DisplayName("WITH 通过")
        void shouldPass_whenWithCte() {
            assertThatNoException()
                    .isThrownBy(() -> guard.check("WITH active_users AS (SELECT * FROM sys_user WHERE status = 1) SELECT * FROM active_users", "TestCaller"));
        }

        @Test
        @DisplayName("INSERT 拒绝")
        void shouldReject_whenInsert() {
            assertThatThrownBy(() -> guard.check("INSERT INTO sys_user (username) VALUES ('admin')", "TestCaller"))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("STRICT mode")
                    .hasMessageContaining("only SELECT queries are allowed");
        }

        @Test
        @DisplayName("UPDATE 拒绝")
        void shouldReject_whenUpdate() {
            assertThatThrownBy(() -> guard.check("UPDATE sys_user SET status = 0 WHERE id = 1", "TestCaller"))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("STRICT mode")
                    .hasMessageContaining("only SELECT queries are allowed");
        }

        @Test
        @DisplayName("DELETE 拒绝")
        void shouldReject_whenDelete() {
            assertThatThrownBy(() -> guard.check("DELETE FROM sys_user WHERE id = 1", "TestCaller"))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("STRICT mode")
                    .hasMessageContaining("only SELECT queries are allowed");
        }

        @Test
        @DisplayName("DROP 拒绝")
        void shouldReject_whenDrop() {
            assertThatThrownBy(() -> guard.check("DROP TABLE sys_user", "TestCaller"))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("STRICT mode")
                    .hasMessageContaining("only SELECT queries are allowed");
        }
    }

    @Nested
    @DisplayName("PERMISSIVE 模式")
    class PermissiveMode {

        private final RawSqlSecurityGuard guard = new RawSqlSecurityGuard(RawSqlSecurityGuard.Mode.PERMISSIVE);

        @Test
        @DisplayName("SELECT 通过")
        void shouldPass_whenSelect() {
            assertThatNoException()
                    .isThrownBy(() -> guard.check("SELECT * FROM sys_user", "TestCaller"));
        }

        @Test
        @DisplayName("INSERT 通过")
        void shouldPass_whenInsert() {
            assertThatNoException()
                    .isThrownBy(() -> guard.check("INSERT INTO sys_user (username) VALUES ('admin')", "TestCaller"));
        }

        @Test
        @DisplayName("DROP 通过（允许一切）")
        void shouldPass_whenDrop() {
            assertThatNoException()
                    .isThrownBy(() -> guard.check("DROP TABLE sys_user", "TestCaller"));
        }
    }

    @Nested
    @DisplayName("Mode 切换")
    class ModeSwitching {

        @Test
        @DisplayName("默认模式为 MODERATE")
        void shouldDefaultToModerate() {
            RawSqlSecurityGuard guard = new RawSqlSecurityGuard();
            assertThat(guard.getMode()).isEqualTo(RawSqlSecurityGuard.Mode.MODERATE);
        }

        @Test
        @DisplayName("构造函数设置模式")
        void shouldSetModeViaConstructor() {
            RawSqlSecurityGuard guard = new RawSqlSecurityGuard(RawSqlSecurityGuard.Mode.STRICT);
            assertThat(guard.getMode()).isEqualTo(RawSqlSecurityGuard.Mode.STRICT);
        }

        @Test
        @DisplayName("setMode 切换模式")
        void shouldSwitchModeViaSetter() {
            RawSqlSecurityGuard guard = new RawSqlSecurityGuard(RawSqlSecurityGuard.Mode.MODERATE);
            guard.setMode(RawSqlSecurityGuard.Mode.STRICT);
            assertThat(guard.getMode()).isEqualTo(RawSqlSecurityGuard.Mode.STRICT);

            // 验证切换后行为变化：MODERATE 允许 INSERT，STRICT 拒绝
            assertThatThrownBy(() -> guard.check("INSERT INTO t (id) VALUES (1)", "TestCaller"))
                    .isInstanceOf(SecurityException.class);

            guard.setMode(RawSqlSecurityGuard.Mode.PERMISSIVE);
            assertThat(guard.getMode()).isEqualTo(RawSqlSecurityGuard.Mode.PERMISSIVE);
            assertThatNoException()
                    .isThrownBy(() -> guard.check("DROP TABLE t", "TestCaller"));
        }
    }
}
