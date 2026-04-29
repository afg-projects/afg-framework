package io.github.afgprojects.framework.data.jdbc.security;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SQL 注入安全测试
 * <p>
 * 测试系统对各种 SQL 注入攻击的防护能力。
 * 安全响应可以是：
 * 1. 优雅处理并返回空结果（无异常）
 * 2. 显式拒绝无效输入（抛出异常，如类型转换失败）
 * 两种响应都是安全的，关键是数据库完整性不被破坏。
 */
@DisplayName("SQL 注入安全测试")
@Tag("security")
class SqlInjectionSecurityTest extends SecurityTestBase {

    @Nested
    @DisplayName("经典 SQL 注入测试")
    class ClassicInjectionTests {

        @ParameterizedTest(name = "[{index}] Payload: {0}")
        @MethodSource("io.github.afgprojects.framework.data.jdbc.security.payload.SqlInjectionPayloads#classic")
        @DisplayName("findById 应防止经典注入")
        void shouldPreventClassicInjectionInFindById(String payload) {
            // 执行查询，异常表示显式拒绝无效输入，是安全的
            try {
                userProxy.findById(payload);
            } catch (Exception e) {
                // 类型转换异常是预期行为，表示系统拒绝无效输入
            }
            // 关键验证：数据库完整性未被破坏
            assertDatabaseIntegrity();
        }

        @ParameterizedTest(name = "[{index}] Payload: {0}")
        @MethodSource("io.github.afgprojects.framework.data.jdbc.security.payload.SqlInjectionPayloads#classic")
        @DisplayName("条件查询应防止经典注入")
        void shouldPreventClassicInjectionInCondition(String payload) {
            try {
                userProxy.findOne(Conditions.eq("name", payload));
            } catch (Exception e) {
                // 异常表示系统拒绝可疑输入，是安全的
            }
            assertDatabaseIntegrity();
        }

        @ParameterizedTest(name = "[{index}] Payload: {0}")
        @MethodSource("io.github.afgprojects.framework.data.jdbc.security.payload.SqlInjectionPayloads#classic")
        @DisplayName("LIKE 查询应防止经典注入")
        void shouldPreventClassicInjectionInLike(String payload) {
            try {
                userProxy.findAll(Conditions.like("email", payload));
            } catch (Exception e) {
                // 异常表示系统拒绝可疑输入，是安全的
            }
            assertDatabaseIntegrity();
        }
    }

    @Nested
    @DisplayName("UNION 注入测试")
    class UnionInjectionTests {

        @ParameterizedTest(name = "[{index}] Payload: {0}")
        @MethodSource("io.github.afgprojects.framework.data.jdbc.security.payload.SqlInjectionPayloads#union")
        @DisplayName("应防止 UNION 注入")
        void shouldPreventUnionInjection(String payload) {
            try {
                userProxy.findOne(Conditions.eq("name", payload));
            } catch (Exception e) {
                // 异常表示系统拒绝可疑输入，是安全的
            }
            assertDatabaseIntegrity();
            assertDataUnchanged();
        }
    }

    @Nested
    @DisplayName("时间盲注测试")
    class TimeBasedInjectionTests {

        @ParameterizedTest(name = "[{index}] Payload: {0}")
        @MethodSource("io.github.afgprojects.framework.data.jdbc.security.payload.SqlInjectionPayloads#timeBased")
        @DisplayName("应检测时间盲注")
        void shouldDetectTimeBasedInjection(String payload) {
            long start = System.currentTimeMillis();
            try {
                userProxy.findById(payload);
            } catch (Exception e) {
                // 异常表示系统拒绝可疑输入，是安全的
            }
            long duration = System.currentTimeMillis() - start;

            // 如果执行时间超过 2 秒，可能存在时间盲注漏洞
            assertThat(duration)
                .as("时间盲注检测：执行时间不应超过 2 秒")
                .isLessThan(2000);

            assertDatabaseIntegrity();
        }
    }

    @Nested
    @DisplayName("错误注入测试")
    class ErrorBasedInjectionTests {

        @ParameterizedTest(name = "[{index}] Payload: {0}")
        @MethodSource("io.github.afgprojects.framework.data.jdbc.security.payload.SqlInjectionPayloads#errorBased")
        @DisplayName("应防止错误注入")
        void shouldPreventErrorBasedInjection(String payload) {
            try {
                userProxy.findOne(Conditions.eq("name", payload));
            } catch (Exception e) {
                // 异常表示系统拒绝可疑输入，是安全的
            }
            assertDatabaseIntegrity();
        }
    }

    @Nested
    @DisplayName("堆叠查询测试")
    class StackedQueryTests {

        @ParameterizedTest(name = "[{index}] Payload: {0}")
        @MethodSource("io.github.afgprojects.framework.data.jdbc.security.payload.SqlInjectionPayloads#stacked")
        @DisplayName("应防止堆叠查询注入")
        void shouldPreventStackedQueryInjection(String payload) {
            try {
                userProxy.findOne(Conditions.eq("name", payload));
            } catch (Exception e) {
                // 异常表示系统拒绝可疑输入，是安全的
            }

            // 验证表未被删除或修改
            assertDatabaseIntegrity();
            assertDataUnchanged();
        }
    }

    @Nested
    @DisplayName("注释绕过测试")
    class CommentBypassTests {

        @ParameterizedTest(name = "[{index}] Payload: {0}")
        @MethodSource("io.github.afgprojects.framework.data.jdbc.security.payload.SqlInjectionPayloads#commentBypass")
        @DisplayName("应防止注释绕过注入")
        void shouldPreventCommentBypassInjection(String payload) {
            try {
                userProxy.findOne(Conditions.eq("name", payload));
            } catch (Exception e) {
                // 异常表示系统拒绝可疑输入，是安全的
            }
            assertDatabaseIntegrity();
        }
    }
}
