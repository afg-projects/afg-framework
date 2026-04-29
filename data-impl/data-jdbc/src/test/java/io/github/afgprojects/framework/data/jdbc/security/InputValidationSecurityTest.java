package io.github.afgprojects.framework.data.jdbc.security;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * 输入验证安全测试
 */
@DisplayName("输入验证安全测试")
@Tag("security")
class InputValidationSecurityTest extends SecurityTestBase {

    @Nested
    @DisplayName("特殊字符处理测试")
    class SpecialCharacterTests {

        @Test
        @DisplayName("应正确处理单引号")
        void shouldHandleSingleQuotes() {
            SecurityTestUser user = new SecurityTestUser();
            user.setName("O'Brien");
            user.setEmail("obrien@test.com");

            SecurityTestUser inserted = userProxy.insert(user);
            SecurityTestUser found = userProxy.findById(inserted.getId()).orElseThrow();

            assertThat(found.getName()).isEqualTo("O'Brien");
        }

        @Test
        @DisplayName("应正确处理双引号")
        void shouldHandleDoubleQuotes() {
            SecurityTestUser user = new SecurityTestUser();
            user.setName("Test \"User\"");
            user.setEmail("test@test.com");

            SecurityTestUser inserted = userProxy.insert(user);
            SecurityTestUser found = userProxy.findById(inserted.getId()).orElseThrow();

            assertThat(found.getName()).isEqualTo("Test \"User\"");
        }

        @Test
        @DisplayName("应正确处理反斜杠")
        void shouldHandleBackslashes() {
            SecurityTestUser user = new SecurityTestUser();
            user.setName("path\\to\\user");
            user.setEmail("path@test.com");

            SecurityTestUser inserted = userProxy.insert(user);
            SecurityTestUser found = userProxy.findById(inserted.getId()).orElseThrow();

            assertThat(found.getName()).isEqualTo("path\\to\\user");
        }

        @Test
        @DisplayName("应正确处理换行符")
        void shouldHandleNewlines() {
            SecurityTestUser user = new SecurityTestUser();
            user.setName("line1\nline2");
            user.setEmail("newline@test.com");

            SecurityTestUser inserted = userProxy.insert(user);
            SecurityTestUser found = userProxy.findById(inserted.getId()).orElseThrow();

            assertThat(found.getName()).isEqualTo("line1\nline2");
        }

        @Test
        @DisplayName("应正确处理 Unicode 字符")
        void shouldHandleUnicodeCharacters() {
            SecurityTestUser user = new SecurityTestUser();
            user.setName("中文日本語한국어🎉");
            user.setEmail("unicode@test.com");

            SecurityTestUser inserted = userProxy.insert(user);
            SecurityTestUser found = userProxy.findById(inserted.getId()).orElseThrow();

            assertThat(found.getName()).isEqualTo("中文日本語한국어🎉");
        }
    }

    @Nested
    @DisplayName("NULL 值处理测试")
    class NullValueTests {

        @Test
        @DisplayName("应正确处理 NULL email")
        void shouldHandleNullEmail() {
            SecurityTestUser user = new SecurityTestUser();
            user.setName("null-email-user");
            user.setEmail(null);

            SecurityTestUser inserted = userProxy.insert(user);
            SecurityTestUser found = userProxy.findById(inserted.getId()).orElseThrow();

            assertThat(found.getName()).isEqualTo("null-email-user");
            assertThat(found.getEmail()).isNull();
        }

        @Test
        @DisplayName("应正确处理空字符串")
        void shouldHandleEmptyString() {
            SecurityTestUser user = new SecurityTestUser();
            user.setName("empty-email-user");
            user.setEmail("");

            SecurityTestUser inserted = userProxy.insert(user);
            SecurityTestUser found = userProxy.findById(inserted.getId()).orElseThrow();

            assertThat(found.getEmail()).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("边界值测试")
    class BoundaryTests {

        @Test
        @DisplayName("应正确处理最大长度字符串")
        void shouldHandleMaxLengthString() {
            SecurityTestUser user = new SecurityTestUser();
            user.setName("a".repeat(100)); // 最大长度
            user.setEmail("max@test.com");

            assertThatNoException().isThrownBy(() -> userProxy.insert(user));
        }

        @Test
        @DisplayName("应正确处理最小值 ID")
        void shouldHandleMinId() {
            assertThat(userProxy.findById(0L)).isEmpty();
            assertThat(userProxy.findById(-1L)).isEmpty();
        }

        @Test
        @DisplayName("应正确处理超大 ID")
        void shouldHandleLargeId() {
            assertThat(userProxy.findById(Long.MAX_VALUE)).isEmpty();
        }
    }

    @Nested
    @DisplayName("批量操作限制测试")
    class BatchLimitTests {

        @Test
        @DisplayName("应正确处理大批量插入")
        void shouldHandleLargeBatchInsert() {
            int batchSize = 1000;
            java.util.List<SecurityTestUser> users = IntStream.range(0, batchSize)
                .mapToObj(i -> {
                    SecurityTestUser user = new SecurityTestUser();
                    user.setName("batch-" + i);
                    user.setEmail("batch" + i + "@test.com");
                    return user;
                })
                .toList();

            java.util.List<SecurityTestUser> inserted = userProxy.insertAll(users);

            assertThat(inserted).hasSize(batchSize);
            assertThat(inserted).allMatch(u -> u.getId() != null);
        }
    }

    @Nested
    @DisplayName("类型安全测试")
    class TypeSafetyTests {

        @Test
        @DisplayName("字符串 ID 查询应安全处理")
        void shouldHandleStringIdSafely() {
            // 尝试用字符串查询数字 ID 字段
            assertThatNoException().isThrownBy(() ->
                userProxy.findOne(Conditions.eq("name", "123"))
            );
        }

        @Test
        @DisplayName("特殊格式字符串应安全处理")
        void shouldHandleSpecialFormatStrings() {
            String[] specialStrings = {
                "null",
                "undefined",
                "NaN",
                "Infinity",
                "-Infinity",
                "0x123",
                "1e10",
                "1.2.3"
            };

            for (String s : specialStrings) {
                assertThatNoException().isThrownBy(() ->
                    userProxy.findAll(Conditions.like("name", s))
                );
            }
        }
    }
}