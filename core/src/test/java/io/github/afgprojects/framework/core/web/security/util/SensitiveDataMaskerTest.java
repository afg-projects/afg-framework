package io.github.afgprojects.framework.core.web.security.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SensitiveDataMaskerTest {

    @Nested
    @DisplayName("isSensitive 测试")
    class IsSensitiveTests {

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "password",
                    "PASSWORD",
                    "Password",
                    "token",
                    "secret",
                    "apiKey",
                    "credential",
                    "accessToken",
                    "refreshToken",
                    // 个人身份信息类
                    "ssn",
                    "SSN",
                    "idCard",
                    "ID_CARD",
                    "idNumber",
                    "ID_NUMBER",
                    "passport",
                    "PASSPORT",
                    "driverLicense",
                    "DRIVER_LICENSE",
                    "licenseNumber",
                    "LICENSE_NUMBER",
                    "birthday",
                    "BIRTHDAY",
                    "birthDate",
                    "BIRTH_DATE",
                    // 金融信息类
                    "creditCard",
                    "CREDIT_CARD",
                    "bankCard",
                    "BANK_CARD",
                    "bankAccount",
                    "BANK_ACCOUNT",
                    "accountNumber",
                    "ACCOUNT_NUMBER",
                    "taxId",
                    "TAX_ID",
                    // 联系方式类
                    "phone",
                    "PHONE",
                    "mobile",
                    "MOBILE",
                    "telephone",
                    "TELEPHONE",
                    "email",
                    "EMAIL",
                    "address",
                    "ADDRESS",
                    "homeAddress",
                    "HOME_ADDRESS",
                    "workAddress",
                    "WORK_ADDRESS",
                    // 其他敏感信息
                    "salary",
                    "SALARY",
                    "income",
                    "INCOME",
                    "realName",
                    "REAL_NAME",
                    "trueName",
                    "TRUE_NAME"
                })
        @DisplayName("敏感字段应返回 true")
        void shouldReturnTrueForSensitiveFields(String fieldName) {
            assertThat(SensitiveDataMasker.isSensitive(fieldName)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"name", "username", "id", "age", "description", "title", "status"})
        @DisplayName("非敏感字段应返回 false")
        void shouldReturnFalseForNonSensitiveFields(String fieldName) {
            assertThat(SensitiveDataMasker.isSensitive(fieldName)).isFalse();
        }

        @Test
        @DisplayName("null 字段名应返回 false")
        void shouldReturnFalseForNullFieldName() {
            assertThat(SensitiveDataMasker.isSensitive(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("mask 测试")
    class MaskTests {

        @Test
        @DisplayName("敏感字段值应脱敏")
        void shouldMaskSensitiveValue() {
            String masked = SensitiveDataMasker.mask("password", "mySecret123");

            assertThat(masked).isEqualTo("myS***");
        }

        @Test
        @DisplayName("短值应完全隐藏")
        void shouldFullyMaskShortValue() {
            String masked = SensitiveDataMasker.mask("token", "abc");

            assertThat(masked).isEqualTo("***");
        }

        @Test
        @DisplayName("非敏感字段值不脱敏")
        void shouldNotMaskNonSensitiveValue() {
            String masked = SensitiveDataMasker.mask("name", "张三");

            assertThat(masked).isEqualTo("张三");
        }

        @Test
        @DisplayName("null 值应返回 null")
        void shouldReturnNullForNullValue() {
            assertThat(SensitiveDataMasker.mask("password", null)).isNull();
        }

        @Test
        @DisplayName("空字符串应返回空字符串")
        void shouldReturnEmptyForEmptyValue() {
            assertThat(SensitiveDataMasker.mask("password", "")).isEmpty();
        }
    }
}
