package io.github.afgprojects.framework.data.jdbc.encryption;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.jdbc.entity.TestEncryptedUser;
import io.github.afgprojects.framework.data.jdbc.test.BasePostgresTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 字段加密集成测试
 * <p>
 * 测试 AES-GCM 加密/解密往返、盲索引查询、加密字段操作符验证。
 * 使用真实 PostgreSQL Testcontainers 数据库。
 */
@Transactional
@ActiveProfiles("test")
@DisplayName("字段加密集成测试")
class FieldEncryptionIntegrationTest extends BasePostgresTest {

    @Autowired
    DataManager dataManager;

    @Nested
    @DisplayName("加密/解密往返")
    class EncryptDecryptRoundTrip {

        @Test
        @DisplayName("INSERT 加密 → SELECT 解密后应得到原始明文")
        void shouldDecryptToOriginalPlaintext_afterInsert() {
            TestEncryptedUser user = TestEncryptedUser.create("alice", "13800138000");

            TestEncryptedUser saved = dataManager.save(TestEncryptedUser.class, user);

            assertThat(saved.getPhone()).isEqualTo("13800138000");

            TestEncryptedUser found = dataManager.findById(TestEncryptedUser.class, saved.getId()).orElseThrow();
            assertThat(found.getPhone()).isEqualTo("13800138000");
        }

        @Test
        @DisplayName("加密后数据库存储密文")
        void shouldStoreCiphertextInDatabase() {
            TestEncryptedUser user = TestEncryptedUser.create("bob", "13900139000");
            TestEncryptedUser saved = dataManager.save(TestEncryptedUser.class, user);

            // 通过原始 SQL 查询验证数据库存储的是密文
            String dbPhone = dataManager.queryForList(
                "SELECT phone FROM test_encrypted_user WHERE id = ?",
                List.of(saved.getId()),
                (rs, rowNum) -> rs.getString("phone")
            ).get(0);

            assertThat(dbPhone).isNotNull();
            assertThat(dbPhone).isNotEqualTo("13900139000");
            // 密文应为 Base64 格式（包含 A-Za-z0-9+/=）
            assertThat(dbPhone).containsPattern("[A-Za-z0-9+/=]+");
        }

        @Test
        @DisplayName("加密字段相同明文产生不同密文（AES-GCM 随机 IV）")
        void shouldProduceDifferentCiphertextForSamePlaintext() {
            TestEncryptedUser user1 = TestEncryptedUser.create("charlie", "13700137000");
            TestEncryptedUser user2 = TestEncryptedUser.create("dave", "13700137000");

            dataManager.save(TestEncryptedUser.class, user1);
            dataManager.save(TestEncryptedUser.class, user2);

            List<String> phones = dataManager.queryForList(
                "SELECT phone FROM test_encrypted_user WHERE username IN (?, ?) ORDER BY username",
                List.of("charlie", "dave"),
                (rs, rowNum) -> rs.getString("phone")
            );

            assertThat(phones).hasSize(2);
            // AES-GCM 使用随机 IV，相同明文应产生不同密文
            assertThat(phones.get(0)).isNotEqualTo(phones.get(1));
        }
    }

    @Nested
    @DisplayName("盲索引查询")
    class BlindIndexQuery {

        @Test
        @DisplayName("EQ 条件查询通过盲索引匹配")
        void shouldMatchViaBlindIndex_forEqCondition() {
            dataManager.save(TestEncryptedUser.class, TestEncryptedUser.create("alice", "13800138000"));
            dataManager.save(TestEncryptedUser.class, TestEncryptedUser.create("bob", "13900139000"));

            Condition condition = Conditions.builder(TestEncryptedUser.class)
                .eq(TestEncryptedUser::getPhone, "13800138000")
                .build();

            List<TestEncryptedUser> result = dataManager.findList(TestEncryptedUser.class, condition);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUsername()).isEqualTo("alice");
            assertThat(result.get(0).getPhone()).isEqualTo("13800138000");
        }

        @Test
        @DisplayName("IN 条件查询通过盲索引匹配")
        void shouldMatchViaBlindIndex_forInCondition() {
            dataManager.save(TestEncryptedUser.class, TestEncryptedUser.create("alice", "13800138000"));
            dataManager.save(TestEncryptedUser.class, TestEncryptedUser.create("bob", "13900139000"));
            dataManager.save(TestEncryptedUser.class, TestEncryptedUser.create("charlie", "13700137000"));

            Condition condition = Conditions.builder(TestEncryptedUser.class)
                .in(TestEncryptedUser::getPhone, List.of("13800138000", "13700137000"))
                .build();

            List<TestEncryptedUser> result = dataManager.findList(TestEncryptedUser.class, condition);

            assertThat(result).hasSize(2);
            assertThat(result.stream().map(TestEncryptedUser::getUsername))
                .containsExactlyInAnyOrder("alice", "charlie");
        }

        @Test
        @DisplayName("NE 条件查询通过盲索引排除")
        void shouldExcludeViaBlindIndex_forNeCondition() {
            dataManager.save(TestEncryptedUser.class, TestEncryptedUser.create("alice", "13800138000"));
            dataManager.save(TestEncryptedUser.class, TestEncryptedUser.create("bob", "13900139000"));

            Condition condition = Conditions.builder(TestEncryptedUser.class)
                .ne(TestEncryptedUser::getPhone, "13800138000")
                .build();

            List<TestEncryptedUser> result = dataManager.findList(TestEncryptedUser.class, condition);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUsername()).isEqualTo("bob");
        }

        @Test
        @DisplayName("查询不存在的盲索引值应返回空")
        void shouldReturnEmpty_forNonExistentBlindIndex() {
            dataManager.save(TestEncryptedUser.class, TestEncryptedUser.create("alice", "13800138000"));

            Condition condition = Conditions.builder(TestEncryptedUser.class)
                .eq(TestEncryptedUser::getPhone, "00000000000")
                .build();

            List<TestEncryptedUser> result = dataManager.findList(TestEncryptedUser.class, condition);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("不支持的操作符")
    class UnsupportedOperators {

        @Test
        @DisplayName("加密字段不支持 LIKE 查询")
        void shouldThrowExceptionForLikeOnEncryptedField() {
            dataManager.save(TestEncryptedUser.class, TestEncryptedUser.create("alice", "13800138000"));

            Condition condition = Conditions.builder(TestEncryptedUser.class)
                .like(TestEncryptedUser::getPhone, "138%")
                .build();

            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                dataManager.findList(TestEncryptedUser.class, condition)
            ).hasMessageContaining("Encrypted field");
        }
    }

    @Nested
    @DisplayName("空值处理")
    class NullHandling {

        @Test
        @DisplayName("加密字段为 null 时 INSERT 和 SELECT 均正常")
        void shouldHandleNullEncryptedField() {
            TestEncryptedUser user = new TestEncryptedUser();
            user.setUsername("no-phone");
            user.setPhone(null);
            user.setStatus(1);

            TestEncryptedUser saved = dataManager.save(TestEncryptedUser.class, user);
            assertThat(saved.getPhone()).isNull();

            TestEncryptedUser found = dataManager.findById(TestEncryptedUser.class, saved.getId()).orElseThrow();
            assertThat(found.getPhone()).isNull();
        }

        @Test
        @DisplayName("加密字段为空字符串时 INSERT 和 SELECT 均正常")
        void shouldHandleEmptyEncryptedField() {
            TestEncryptedUser user = new TestEncryptedUser();
            user.setUsername("empty-phone");
            user.setPhone("");
            user.setStatus(1);

            TestEncryptedUser saved = dataManager.save(TestEncryptedUser.class, user);
            assertThat(saved.getPhone()).isEmpty();

            TestEncryptedUser found = dataManager.findById(TestEncryptedUser.class, saved.getId()).orElseThrow();
            assertThat(found.getPhone()).isEmpty();
        }
    }

    @Nested
    @DisplayName("UPDATE 操作")
    class UpdateOperation {

        @Test
        @DisplayName("UPDATE 加密字段后盲索引值应同步更新")
        void shouldUpdateBlindIndex_whenEncryptedFieldUpdated() {
            TestEncryptedUser user = dataManager.save(TestEncryptedUser.class,
                TestEncryptedUser.create("alice", "13800138000"));

            user.setPhone("13700137000");
            dataManager.save(TestEncryptedUser.class, user);

            // 用旧值查不到
            Condition oldCondition = Conditions.builder(TestEncryptedUser.class)
                .eq(TestEncryptedUser::getPhone, "13800138000")
                .build();
            assertThat(dataManager.findList(TestEncryptedUser.class, oldCondition)).isEmpty();

            // 用新值可以查到
            Condition newCondition = Conditions.builder(TestEncryptedUser.class)
                .eq(TestEncryptedUser::getPhone, "13700137000")
                .build();
            var found = dataManager.findList(TestEncryptedUser.class, newCondition);
            assertThat(found).hasSize(1);
            assertThat(found.get(0).getPhone()).isEqualTo("13700137000");
        }
    }
}
