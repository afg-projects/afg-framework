package io.github.afgprojects.framework.core.web.security.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SensitiveDataMasker")
class SensitiveDataMaskerTest {

    @BeforeEach
    void setUp() {
        SensitiveDataMasker.clearExtendedFields();
    }

    @AfterEach
    void tearDown() {
        SensitiveDataMasker.clearExtendedFields();
    }

    @Nested
    @DisplayName("isSensitive")
    class IsSensitive {

        @Nested
        @DisplayName("authentication credentials")
        class AuthenticationCredentials {

            @Test
            @DisplayName("should detect password")
            void shouldDetectPassword() {
                assertThat(SensitiveDataMasker.isSensitive("password")).isTrue();
            }

            @Test
            @DisplayName("should detect pwd")
            void shouldDetectPwd() {
                assertThat(SensitiveDataMasker.isSensitive("pwd")).isTrue();
            }

            @Test
            @DisplayName("should detect token")
            void shouldDetectToken() {
                assertThat(SensitiveDataMasker.isSensitive("token")).isTrue();
            }

            @Test
            @DisplayName("should detect secret")
            void shouldDetectSecret() {
                assertThat(SensitiveDataMasker.isSensitive("secret")).isTrue();
            }

            @Test
            @DisplayName("should detect apikey")
            void shouldDetectApikey() {
                assertThat(SensitiveDataMasker.isSensitive("apikey")).isTrue();
            }

            @Test
            @DisplayName("should detect credential")
            void shouldDetectCredential() {
                assertThat(SensitiveDataMasker.isSensitive("credential")).isTrue();
            }

            @Test
            @DisplayName("should detect accesstoken")
            void shouldDetectAccesstoken() {
                assertThat(SensitiveDataMasker.isSensitive("accesstoken")).isTrue();
            }

            @Test
            @DisplayName("should detect refreshtoken")
            void shouldDetectRefreshtoken() {
                assertThat(SensitiveDataMasker.isSensitive("refreshtoken")).isTrue();
            }

            @Test
            @DisplayName("should detect privatekey")
            void shouldDetectPrivatekey() {
                assertThat(SensitiveDataMasker.isSensitive("privatekey")).isTrue();
            }

            @Test
            @DisplayName("should detect sessionid")
            void shouldDetectSessionid() {
                assertThat(SensitiveDataMasker.isSensitive("sessionid")).isTrue();
            }
        }

        @Nested
        @DisplayName("personal identity information")
        class PersonalIdentity {

            @Test
            @DisplayName("should detect ssn")
            void shouldDetectSsn() {
                assertThat(SensitiveDataMasker.isSensitive("ssn")).isTrue();
            }

            @Test
            @DisplayName("should detect idcard")
            void shouldDetectIdcard() {
                assertThat(SensitiveDataMasker.isSensitive("idcard")).isTrue();
            }

            @Test
            @DisplayName("should detect idnumber")
            void shouldDetectIdnumber() {
                assertThat(SensitiveDataMasker.isSensitive("idnumber")).isTrue();
            }

            @Test
            @DisplayName("should detect passport")
            void shouldDetectPassport() {
                assertThat(SensitiveDataMasker.isSensitive("passport")).isTrue();
            }

            @Test
            @DisplayName("should detect driverlicense")
            void shouldDetectDriverlicense() {
                assertThat(SensitiveDataMasker.isSensitive("driverlicense")).isTrue();
            }

            @Test
            @DisplayName("should detect birthday")
            void shouldDetectBirthday() {
                assertThat(SensitiveDataMasker.isSensitive("birthday")).isTrue();
            }
        }

        @Nested
        @DisplayName("financial information")
        class Financial {

            @Test
            @DisplayName("should detect creditcard")
            void shouldDetectCreditcard() {
                assertThat(SensitiveDataMasker.isSensitive("creditcard")).isTrue();
            }

            @Test
            @DisplayName("should detect bankcard")
            void shouldDetectBankcard() {
                assertThat(SensitiveDataMasker.isSensitive("bankcard")).isTrue();
            }

            @Test
            @DisplayName("should detect bankaccount")
            void shouldDetectBankaccount() {
                assertThat(SensitiveDataMasker.isSensitive("bankaccount")).isTrue();
            }

            @Test
            @DisplayName("should detect salary")
            void shouldDetectSalary() {
                assertThat(SensitiveDataMasker.isSensitive("salary")).isTrue();
            }

            @Test
            @DisplayName("should detect income")
            void shouldDetectIncome() {
                assertThat(SensitiveDataMasker.isSensitive("income")).isTrue();
            }
        }

        @Nested
        @DisplayName("contact information")
        class Contact {

            @Test
            @DisplayName("should detect phone")
            void shouldDetectPhone() {
                assertThat(SensitiveDataMasker.isSensitive("phone")).isTrue();
            }

            @Test
            @DisplayName("should detect mobile")
            void shouldDetectMobile() {
                assertThat(SensitiveDataMasker.isSensitive("mobile")).isTrue();
            }

            @Test
            @DisplayName("should detect email")
            void shouldDetectEmail() {
                assertThat(SensitiveDataMasker.isSensitive("email")).isTrue();
            }

            @Test
            @DisplayName("should detect address")
            void shouldDetectAddress() {
                assertThat(SensitiveDataMasker.isSensitive("address")).isTrue();
            }
        }

        @Nested
        @DisplayName("case insensitive")
        class CaseInsensitive {

            @Test
            @DisplayName("should detect uppercase")
            void shouldDetectUppercase() {
                assertThat(SensitiveDataMasker.isSensitive("PASSWORD")).isTrue();
                assertThat(SensitiveDataMasker.isSensitive("TOKEN")).isTrue();
            }

            @Test
            @DisplayName("should detect mixed case")
            void shouldDetectMixedCase() {
                assertThat(SensitiveDataMasker.isSensitive("Password")).isTrue();
                assertThat(SensitiveDataMasker.isSensitive("Token")).isTrue();
            }
        }

        @Nested
        @DisplayName("underscore handling")
        class UnderscoreHandling {

            @Test
            @DisplayName("should ignore underscores and match base keyword")
            void shouldIgnoreUnderscores_andMatchBaseKeyword() {
                // "pass_word" normalizes to "password" which is in the default set
                assertThat(SensitiveDataMasker.isSensitive("pass_word")).isTrue();
                // "access_token" normalizes to "accesstoken" which is in the default set
                assertThat(SensitiveDataMasker.isSensitive("access_token")).isTrue();
            }

            @Test
            @DisplayName("should not match compound names without base keyword")
            void shouldNotMatchCompoundNames_withoutBaseKeyword() {
                // "user_password" normalizes to "userpassword" which is NOT in the default set
                // (only "password" is)
                assertThat(SensitiveDataMasker.isSensitive("user_password")).isFalse();
            }
        }

        @Nested
        @DisplayName("non-sensitive fields")
        class NonSensitiveFields {

            @Test
            @DisplayName("should return false for non-sensitive fields")
            void shouldReturnFalse_forNonSensitiveFields() {
                assertThat(SensitiveDataMasker.isSensitive("username")).isFalse();
                assertThat(SensitiveDataMasker.isSensitive("name")).isFalse();
                assertThat(SensitiveDataMasker.isSensitive("age")).isFalse();
                assertThat(SensitiveDataMasker.isSensitive("description")).isFalse();
            }
        }

        @Nested
        @DisplayName("null and blank handling")
        class NullAndBlank {

            @Test
            @DisplayName("should return false for null")
            void shouldReturnFalse_forNull() {
                assertThat(SensitiveDataMasker.isSensitive(null)).isFalse();
            }

            @Test
            @DisplayName("should return false for blank")
            void shouldReturnFalse_forBlank() {
                assertThat(SensitiveDataMasker.isSensitive("")).isFalse();
                assertThat(SensitiveDataMasker.isSensitive("   ")).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("mask")
    class Mask {

        @Test
        @DisplayName("should return null for null value")
        void shouldReturnNull_forNullValue() {
            assertThat(SensitiveDataMasker.mask("password", null)).isNull();
        }

        @Test
        @DisplayName("should return empty for empty value")
        void shouldReturnEmpty_forEmptyValue() {
            assertThat(SensitiveDataMasker.mask("password", "")).isEmpty();
        }

        @Test
        @DisplayName("should mask sensitive field")
        void shouldMaskSensitiveField() {
            assertThat(SensitiveDataMasker.mask("password", "secret123")).isEqualTo("sec***");
        }

        @Test
        @DisplayName("should return original value for non-sensitive field")
        void shouldReturnOriginalValue_forNonSensitiveField() {
            assertThat(SensitiveDataMasker.mask("username", "john_doe")).isEqualTo("john_doe");
        }

        @ParameterizedTest
        @CsvSource({"ab, ***", "abc, ***", "abcd, abc***", "12345678, 123***"})
        @DisplayName("should mask short values completely")
        void shouldMaskShortValuesCompletely(String value, String expected) {
            assertThat(SensitiveDataMasker.mask("password", value)).isEqualTo(expected);
        }

        @Test
        @DisplayName("should mask phone number")
        void shouldMaskPhoneNumber() {
            assertThat(SensitiveDataMasker.mask("phone", "13812345678")).isEqualTo("138***");
        }

        @Test
        @DisplayName("should mask email")
        void shouldMaskEmail() {
            assertThat(SensitiveDataMasker.mask("email", "user@example.com")).isEqualTo("use***");
        }

        @Test
        @DisplayName("should mask id card")
        void shouldMaskIdCard() {
            assertThat(SensitiveDataMasker.mask("idcard", "110101199001011234")).isEqualTo("110***");
        }
    }

    @Nested
    @DisplayName("registerSensitiveFields")
    class RegisterSensitiveFields {

        @Test
        @DisplayName("should register new sensitive fields")
        void shouldRegisterNewSensitiveFields() {
            SensitiveDataMasker.registerSensitiveFields(Set.of("customField", "anotherField"));

            assertThat(SensitiveDataMasker.isSensitive("customField")).isTrue();
            assertThat(SensitiveDataMasker.isSensitive("anotherField")).isTrue();
        }

        @Test
        @DisplayName("should normalize field names")
        void shouldNormalizeFieldNames() {
            SensitiveDataMasker.registerSensitiveFields(Set.of("CUSTOM_FIELD", "Another_Field"));

            assertThat(SensitiveDataMasker.isSensitive("customfield")).isTrue();
            assertThat(SensitiveDataMasker.isSensitive("anotherfield")).isTrue();
            assertThat(SensitiveDataMasker.isSensitive("CUSTOM_FIELD")).isTrue();
        }

        @Test
        @DisplayName("should handle null input")
        void shouldHandleNullInput() {
            SensitiveDataMasker.registerSensitiveFields(null);
            // Should not throw
        }

        @Test
        @DisplayName("should handle empty input")
        void shouldHandleEmptyInput() {
            SensitiveDataMasker.registerSensitiveFields(Set.of());
            // Should not throw
        }

        @Test
        @DisplayName("should handle null elements in input via ArrayList")
        void shouldHandleNullElementsInInput() {
            // Set.of() does not allow null, so use ArrayList-based set
            SensitiveDataMasker.registerSensitiveFields(new java.util.HashSet<>(java.util.Arrays.asList("valid", null, "alsoValid")));

            assertThat(SensitiveDataMasker.isSensitive("valid")).isTrue();
            assertThat(SensitiveDataMasker.isSensitive("alsoValid")).isTrue();
        }
    }

    @Nested
    @DisplayName("clearExtendedFields")
    class ClearExtendedFields {

        @Test
        @DisplayName("should clear extended fields")
        void shouldClearExtendedFields() {
            SensitiveDataMasker.registerSensitiveFields(Set.of("customField"));
            assertThat(SensitiveDataMasker.isSensitive("customField")).isTrue();

            SensitiveDataMasker.clearExtendedFields();

            assertThat(SensitiveDataMasker.isSensitive("customField")).isFalse();
        }

        @Test
        @DisplayName("should not affect default fields")
        void shouldNotAffectDefaultFields() {
            SensitiveDataMasker.clearExtendedFields();

            assertThat(SensitiveDataMasker.isSensitive("password")).isTrue();
            assertThat(SensitiveDataMasker.isSensitive("token")).isTrue();
        }
    }
}
