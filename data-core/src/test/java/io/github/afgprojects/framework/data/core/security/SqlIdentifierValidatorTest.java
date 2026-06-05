/*
 * Copyright 2024 AFG Projects.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.afgprojects.framework.data.core.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * SqlIdentifierValidator 单元测试
 * <p>
 * 测试 SQL 标识符验证和 SQL 条件片段安全校验。
 */
class SqlIdentifierValidatorTest {

    // ==================== 列名验证 ====================

    @Nested
    @DisplayName("列名验证 (COLUMN)")
    class ColumnNameValidation {

        @Test
        @DisplayName("should accept valid column name when well formed")
        void shouldAcceptValidColumnName_whenWellFormed() {
            assertThatCode(() -> SqlIdentifierValidator.validateColumn("user_name"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should accept column name with dot when table prefix included")
        void shouldAcceptColumnNameWithDot_whenTablePrefixIncluded() {
            assertThatCode(() -> SqlIdentifierValidator.validateColumn("table.column"))
                .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {"user_name", "userName", "USER_NAME", "_private", "col1", "table.column", "t.status"})
        @DisplayName("should return true for valid column name when isValidColumn called")
        void shouldReturnTrueForValidColumnName_whenIsValidColumnCalled(String column) {
            assertThat(SqlIdentifierValidator.isValidColumn(column)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"123column", "user-name", "user name", "user;name"})
        @DisplayName("should return false for invalid column name when isValidColumn called")
        void shouldReturnFalseForInvalidColumnName_whenIsValidColumnCalled(String column) {
            assertThat(SqlIdentifierValidator.isValidColumn(column)).isFalse();
        }

        @Test
        @DisplayName("should reject column name when starts with digit")
        void shouldRejectColumnName_whenStartsWithDigit() {
            assertThatThrownBy(() -> SqlIdentifierValidator.validateColumn("123column"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid column name");
        }

        @Test
        @DisplayName("should reject column name when contains special chars")
        void shouldRejectColumnName_whenContainsSpecialChars() {
            assertThatThrownBy(() -> SqlIdentifierValidator.validateColumn("user-name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid column name");
        }

        @Test
        @DisplayName("should reject null column name when validate")
        void shouldRejectNullColumnName_whenValidate() {
            assertThatThrownBy(() -> SqlIdentifierValidator.validateColumn(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
        }

        @Test
        @DisplayName("should reject empty column name when validate")
        void shouldRejectEmptyColumnName_whenValidate() {
            assertThatThrownBy(() -> SqlIdentifierValidator.validateColumn(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
        }
    }

    // ==================== 表名验证 ====================

    @Nested
    @DisplayName("表名验证 (TABLE)")
    class TableNameValidation {

        @Test
        @DisplayName("should accept valid table name when well formed")
        void shouldAcceptValidTableName_whenWellFormed() {
            assertThatCode(() -> SqlIdentifierValidator.validateTable("user"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should accept table name with hyphen when allowed")
        void shouldAcceptTableNameWithHyphen_whenAllowed() {
            assertThatCode(() -> SqlIdentifierValidator.validateTable("my-table"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should accept table name with dot when schema prefix included")
        void shouldAcceptTableNameWithDot_whenSchemaPrefixIncluded() {
            assertThatCode(() -> SqlIdentifierValidator.validateTable("schema.table"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should accept table name with consecutive hyphens")
        void shouldAcceptTableNameWithConsecutiveHyphens() {
            // TABLE_PATTERN allows hyphens, so "table--name" is valid per the regex
            assertThatCode(() -> SqlIdentifierValidator.validateTable("table--name"))
                .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {"user", "user_table", "my-table", "schema.table", "USER", "_table", "table--name"})
        @DisplayName("should return true for valid table name when isValidTable called")
        void shouldReturnTrueForValidTableName_whenIsValidTableCalled(String table) {
            assertThat(SqlIdentifierValidator.isValidTable(table)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"123table", "table name", "table;name"})
        @DisplayName("should return false for invalid table name when isValidTable called")
        void shouldReturnFalseForInvalidTableName_whenIsValidTableCalled(String table) {
            assertThat(SqlIdentifierValidator.isValidTable(table)).isFalse();
        }

        @Test
        @DisplayName("should reject table name when starts with digit")
        void shouldRejectTableName_whenStartsWithDigit() {
            assertThatThrownBy(() -> SqlIdentifierValidator.validateTable("123table"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid table name");
        }
    }

    // ==================== 别名验证 ====================

    @Nested
    @DisplayName("别名验证 (ALIAS)")
    class AliasValidation {

        @Test
        @DisplayName("should accept valid alias when well formed")
        void shouldAcceptValidAlias_whenWellFormed() {
            assertThatCode(() -> SqlIdentifierValidator.validateAlias("u"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should accept alias with underscore")
        void shouldAcceptAliasWithUnderscore() {
            assertThatCode(() -> SqlIdentifierValidator.validateAlias("user_alias"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should reject alias when contains dot")
        void shouldRejectAlias_whenContainsDot() {
            assertThatThrownBy(() -> SqlIdentifierValidator.validateAlias("user.alias"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid alias");
        }

        @Test
        @DisplayName("should reject alias when contains hyphen")
        void shouldRejectAlias_whenContainsHyphen() {
            assertThatThrownBy(() -> SqlIdentifierValidator.validateAlias("user-alias"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid alias");
        }
    }

    // ==================== CTE 名称验证 ====================

    @Nested
    @DisplayName("CTE 名称验证 (CTE_NAME)")
    class CteNameValidation {

        @Test
        @DisplayName("should accept valid CTE name when well formed")
        void shouldAcceptValidCteName_whenWellFormed() {
            assertThatCode(() -> SqlIdentifierValidator.validateCteName("cte_users"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should reject CTE name when contains dot")
        void shouldRejectCteName_whenContainsDot() {
            assertThatThrownBy(() -> SqlIdentifierValidator.validateCteName("cte.users"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid CTE name");
        }
    }

    // ==================== 简单标识符验证 ====================

    @Nested
    @DisplayName("简单标识符验证 (SIMPLE_IDENTIFIER)")
    class SimpleIdentifierValidation {

        @Test
        @DisplayName("should accept valid simple identifier when well formed")
        void shouldAcceptValidSimpleIdentifier_whenWellFormed() {
            assertThatCode(() -> SqlIdentifierValidator.validateSimpleIdentifier("column_name"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should reject simple identifier when contains dot")
        void shouldRejectSimpleIdentifier_whenContainsDot() {
            // validateSimpleIdentifier uses type "identifier", not "simple identifier"
            assertThatThrownBy(() -> SqlIdentifierValidator.validateSimpleIdentifier("table.column"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid identifier");
        }

        @Test
        @DisplayName("should accept simple identifier with custom type description")
        void shouldAcceptSimpleIdentifierWithCustomTypeDescription() {
            assertThatCode(() -> SqlIdentifierValidator.validateSimpleIdentifier("column_name", "table name"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should reject simple identifier with custom type description when invalid")
        void shouldRejectSimpleIdentifierWithCustomTypeDescription_whenInvalid() {
            assertThatThrownBy(() -> SqlIdentifierValidator.validateSimpleIdentifier("table.column", "table name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid table name");
        }

        @Test
        @DisplayName("should return true for valid simple identifier when isValidSimpleIdentifier called")
        void shouldReturnTrueForValidSimpleIdentifier_whenIsValidSimpleIdentifierCalled() {
            assertThat(SqlIdentifierValidator.isValidSimpleIdentifier("column_name")).isTrue();
        }

        @Test
        @DisplayName("should return false for invalid simple identifier when isValidSimpleIdentifier called")
        void shouldReturnFalseForInvalidSimpleIdentifier_whenIsValidSimpleIdentifierCalled() {
            assertThat(SqlIdentifierValidator.isValidSimpleIdentifier("table.column")).isFalse();
        }
    }

    // ==================== SQL 条件片段安全校验 ====================

    @Nested
    @DisplayName("SQL 条件片段安全校验")
    class SqlConditionFragmentValidation {

        // ---------- 安全片段（应通过） ----------

        @Nested
        @DisplayName("安全片段")
        class SafeFragments {

            @Test
            @DisplayName("should accept safe SQL condition fragment with placeholder")
            void shouldAcceptSafeSqlConditionFragment_withPlaceholder() {
                assertThatCode(() -> SqlIdentifierValidator.validateSqlConditionFragment("user_id = #{userId}"))
                    .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("should accept safe SQL condition fragment with placeholder on left side")
            void shouldAcceptSafeSqlConditionFragment_withPlaceholderOnLeft() {
                assertThatCode(() -> SqlIdentifierValidator.validateSqlConditionFragment("#{userId} = 1"))
                    .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("should accept safe SQL condition fragment with simple equality")
            void shouldAcceptSafeSqlConditionFragment_withSimpleEquality() {
                assertThatCode(() -> SqlIdentifierValidator.validateSqlConditionFragment("status = 1"))
                    .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("should accept safe SQL condition fragment with AND operator")
            void shouldAcceptSafeSqlConditionFragment_withAndOperator() {
                assertThatCode(() -> SqlIdentifierValidator.validateSqlConditionFragment("user_id = #{userId} AND status = 1"))
                    .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("should accept safe SQL condition fragment with OR operator")
            void shouldAcceptSafeSqlConditionFragment_withOrOperator() {
                assertThatCode(() -> SqlIdentifierValidator.validateSqlConditionFragment("status = 1 OR role_id = 2"))
                    .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("should accept safe SQL condition fragment with IN operator")
            void shouldAcceptSafeSqlConditionFragment_withInOperator() {
                assertThatCode(() -> SqlIdentifierValidator.validateSqlConditionFragment("user_id IN (1, 2, 3)"))
                    .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("should accept safe SQL condition fragment with BETWEEN operator")
            void shouldAcceptSafeSqlConditionFragment_withBetweenOperator() {
                assertThatCode(() -> SqlIdentifierValidator.validateSqlConditionFragment("age BETWEEN 18 AND 65"))
                    .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("should accept safe SQL condition fragment with NOT BETWEEN operator")
            void shouldAcceptSafeSqlConditionFragment_withNotBetweenOperator() {
                assertThatCode(() -> SqlIdentifierValidator.validateSqlConditionFragment("age NOT BETWEEN 10 AND 20"))
                    .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("should accept safe SQL condition fragment with NOT IN operator")
            void shouldAcceptSafeSqlConditionFragment_withNotInOperator() {
                assertThatCode(() -> SqlIdentifierValidator.validateSqlConditionFragment("status NOT IN (0, 9)"))
                    .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("should accept safe SQL condition fragment with IS NOT NULL")
            void shouldAcceptSafeSqlConditionFragment_withIsNotNull() {
                assertThatCode(() -> SqlIdentifierValidator.validateSqlConditionFragment("status IS NOT NULL"))
                    .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("should accept safe SQL condition fragment with LIKE operator using placeholder")
            void shouldAcceptSafeSqlConditionFragment_withLikeOperatorPlaceholder() {
                // String literals with quotes are not in the safe token pattern;
                // use placeholders instead for LIKE values
                assertThatCode(() -> SqlIdentifierValidator.validateSqlConditionFragment("name LIKE #{pattern}"))
                    .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("should accept safe SQL condition fragment with NOT LIKE operator using placeholder")
            void shouldAcceptSafeSqlConditionFragment_withNotLikeOperatorPlaceholder() {
                assertThatCode(() -> SqlIdentifierValidator.validateSqlConditionFragment("name NOT LIKE #{pattern}"))
                    .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("should accept safe SQL condition fragment with table-prefixed column")
            void shouldAcceptSafeSqlConditionFragment_withTablePrefixedColumn() {
                assertThatCode(() -> SqlIdentifierValidator.validateSqlConditionFragment("t.status = 1"))
                    .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("should accept safe SQL condition fragment with multiple placeholders")
            void shouldAcceptSafeSqlConditionFragment_withMultiplePlaceholders() {
                assertThatCode(() -> SqlIdentifierValidator.validateSqlConditionFragment("dept_id = #{deptId} AND status = 1"))
                    .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("should accept safe SQL condition fragment with comparison operators")
            void shouldAcceptSafeSqlConditionFragment_withComparisonOperators() {
                assertThatCode(() -> SqlIdentifierValidator.validateSqlConditionFragment("age >= 18"))
                    .doesNotThrowAnyException();
                assertThatCode(() -> SqlIdentifierValidator.validateSqlConditionFragment("age <= 65"))
                    .doesNotThrowAnyException();
                assertThatCode(() -> SqlIdentifierValidator.validateSqlConditionFragment("age <> 0"))
                    .doesNotThrowAnyException();
                assertThatCode(() -> SqlIdentifierValidator.validateSqlConditionFragment("age != 0"))
                    .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("should accept safe SQL condition fragment with decimal numbers")
            void shouldAcceptSafeSqlConditionFragment_withDecimalNumbers() {
                assertThatCode(() -> SqlIdentifierValidator.validateSqlConditionFragment("price = 9.99"))
                    .doesNotThrowAnyException();
            }
        }

        // ---------- 禁止关键字（DDL / 危险操作） ----------

        @Nested
        @DisplayName("DDL 和危险操作关键字")
        class ForbiddenKeywords {

            @ParameterizedTest
            @ValueSource(strings = {"DROP TABLE users", "drop table users"})
            @DisplayName("should reject SQL condition fragment when contains DROP keyword")
            void shouldRejectSqlConditionFragment_whenContainsDropKeyword(String fragment) {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment(fragment))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("DROP");
            }

            @ParameterizedTest
            @ValueSource(strings = {"ALTER TABLE users", "alter table users"})
            @DisplayName("should reject SQL condition fragment when contains ALTER keyword")
            void shouldRejectSqlConditionFragment_whenContainsAlterKeyword(String fragment) {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment(fragment))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("ALTER");
            }

            @ParameterizedTest
            @ValueSource(strings = {"CREATE TABLE hacked (id INT)", "create table hacked"})
            @DisplayName("should reject SQL condition fragment when contains CREATE keyword")
            void shouldRejectSqlConditionFragment_whenContainsCreateKeyword(String fragment) {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment(fragment))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("CREATE");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when contains TRUNCATE keyword")
            void shouldRejectSqlConditionFragment_whenContainsTruncateKeyword() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("TRUNCATE TABLE users"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("TRUNCATE");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when contains GRANT keyword")
            void shouldRejectSqlConditionFragment_whenContainsGrantKeyword() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("GRANT ALL PRIVILEGES"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("GRANT");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when contains REVOKE keyword")
            void shouldRejectSqlConditionFragment_whenContainsRevokeKeyword() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("REVOKE SELECT ON users"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("REVOKE");
            }
        }

        // ---------- UNION / 集合操作关键字 ----------

        @Nested
        @DisplayName("UNION 和集合操作关键字")
        class SetOperationKeywords {

            @Test
            @DisplayName("should reject SQL condition fragment when contains UNION keyword")
            void shouldRejectSqlConditionFragment_whenContainsUnionKeyword() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("1 = 1 UNION SELECT * FROM users"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("UNION");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when contains INTERSECT keyword")
            void shouldRejectSqlConditionFragment_whenContainsIntersectKeyword() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("1 INTERSECT SELECT 2"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("INTERSECT");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when contains EXCEPT keyword")
            void shouldRejectSqlConditionFragment_whenContainsExceptKeyword() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("1 EXCEPT SELECT 2"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("EXCEPT");
            }
        }

        // ---------- 存储过程 / 危险函数 ----------

        @Nested
        @DisplayName("存储过程和危险函数关键字")
        class DangerousFunctionKeywords {

            @Test
            @DisplayName("should reject SQL condition fragment when contains EXEC keyword")
            void shouldRejectSqlConditionFragment_whenContainsExecKeyword() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("EXEC sp_hacked"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("EXEC");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when contains EXECUTE keyword")
            void shouldRejectSqlConditionFragment_whenContainsExecuteKeyword() {
                // EXECUTE contains EXEC as substring, so EXEC is reported
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("EXECUTE sp_hacked"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("EXEC");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when contains XP_ prefix")
            void shouldRejectSqlConditionFragment_whenContainsXpPrefix() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("XP_CMDSHELL"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("XP_");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when contains SP_ prefix")
            void shouldRejectSqlConditionFragment_whenContainsSpPrefix() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("SP_HACKED"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("SP_");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when contains SLEEP keyword")
            void shouldRejectSqlConditionFragment_whenContainsSleep() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("SLEEP(5)"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("SLEEP");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when contains BENCHMARK keyword")
            void shouldRejectSqlConditionFragment_whenContainsBenchmark() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("BENCHMARK(1000000, SHA1(#{test}))"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("BENCHMARK");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when contains WAITFOR keyword")
            void shouldRejectSqlConditionFragment_whenContainsWaitfor() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("WAITFOR DELAY #{delay}"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("WAITFOR");
            }
        }

        // ---------- 文件操作关键字 ----------

        @Nested
        @DisplayName("文件操作关键字")
        class FileOperationKeywords {

            @Test
            @DisplayName("should reject SQL condition fragment when contains INTO keyword")
            void shouldRejectSqlConditionFragment_whenContainsIntoKeyword() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("1 = 1 INTO OUTFILE #{path}"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("INTO");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when contains OUTFILE keyword")
            void shouldRejectSqlConditionFragment_whenContainsOutfileKeyword() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("OUTFILE #{path}"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("OUTFILE");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when contains DUMPFILE keyword")
            void shouldRejectSqlConditionFragment_whenContainsDumpfileKeyword() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("INTO DUMPFILE #{path}"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("DUMPFILE");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when contains LOAD_FILE keyword")
            void shouldRejectSqlConditionFragment_whenContainsLoadFileKeyword() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("LOAD_FILE(#{path})"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("LOAD_FILE");
            }
        }

        // ---------- 系统表关键字 ----------

        @Nested
        @DisplayName("系统表关键字")
        class SystemTableKeywords {

            @Test
            @DisplayName("should reject SQL condition fragment when contains INFORMATION_SCHEMA keyword")
            void shouldRejectSqlConditionFragment_whenContainsInformationSchema() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("INFORMATION_SCHEMA.TABLES"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("INFORMATION_SCHEMA");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when contains PG_CATALOG keyword")
            void shouldRejectSqlConditionFragment_whenContainsPgCatalog() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("PG_CATALOG.USERS"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("PG_CATALOG");
            }
        }

        // ---------- 子查询 / DML 检测（步骤3） ----------

        @Nested
        @DisplayName("子查询和 DML 检测")
        class SubqueryAndDmlDetection {

            @Test
            @DisplayName("should reject SQL condition fragment when contains SELECT subquery")
            void shouldRejectSqlConditionFragment_whenContainsSelectSubquery() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("id IN (SELECT id FROM other)"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("subquery or DML");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when contains UPDATE keyword")
            void shouldRejectSqlConditionFragment_whenContainsUpdateKeyword() {
                // UPDATE is detected by step 3 (subquery/DML), not by FORBIDDEN_KEYWORDS
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("UPDATE users SET name = #{val}"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("subquery or DML");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when contains DELETE keyword")
            void shouldRejectSqlConditionFragment_whenContainsDeleteKeyword() {
                // DELETE is detected by step 3 (subquery/DML), not by FORBIDDEN_KEYWORDS
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("DELETE FROM users"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("subquery or DML");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when contains INSERT keyword via INTO forbidden keyword")
            void shouldRejectSqlConditionFragment_whenContainsInsertKeyword() {
                // INSERT itself is not in FORBIDDEN_KEYWORDS, but INTO is.
                // So "INSERT INTO ..." is caught by INTO in step 1.
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("INSERT INTO users VALUES (1)"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("INTO");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when column name contains DELETE substring")
            void shouldRejectSqlConditionFragment_whenColumnNameContainsDeleteSubstring() {
                // "deleted_at" contains "DELETE" as substring, caught by step 3
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("deleted_at IS NULL"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("subquery or DML");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when column name contains UPDATE substring")
            void shouldRejectSqlConditionFragment_whenColumnNameContainsUpdateSubstring() {
                // "update_time" contains "UPDATE" as substring, caught by step 3
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("update_time > 0"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("subquery or DML");
            }
        }

        // ---------- 注释和分号 ----------

        @Nested
        @DisplayName("注释和分号")
        class CommentAndSemicolonDetection {

            @Test
            @DisplayName("should reject SQL condition fragment when contains single line comment")
            void shouldRejectSqlConditionFragment_whenContainsSingleLineComment() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("1 = 1 -- comment"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("comment or semicolon");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when contains multi line comment")
            void shouldRejectSqlConditionFragment_whenContainsMultiLineComment() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("1 = 1 /* comment */"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("comment or semicolon");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when contains semicolon")
            void shouldRejectSqlConditionFragment_whenContainsSemicolon() {
                // Semicolons without forbidden keywords are caught by step 2
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("1 = 1; "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("comment or semicolon");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when contains semicolon with DROP after")
            void shouldRejectSqlConditionFragment_whenContainsSemicolonWithDrop() {
                // "1 = 1; DROP TABLE users" — DROP is a forbidden keyword checked in step 1,
                // which runs before the semicolon check in step 2
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("1 = 1; DROP TABLE users"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("DROP");
            }
        }

        // ---------- 不安全令牌（步骤4 token 化） ----------

        @Nested
        @DisplayName("不安全令牌检测")
        class UnrecognizedTokenDetection {

            @Test
            @DisplayName("should reject SQL condition fragment when contains string literals with single quotes")
            void shouldRejectSqlConditionFragment_whenContainsStringLiterals() {
                // Single quotes are not in SAFE_CONDITION_TOKEN_PATTERN
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("role = 'admin'"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unrecognized tokens");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when LIKE value contains quotes")
            void shouldRejectSqlConditionFragment_whenLikeValueContainsQuotes() {
                // Single quotes and % are not in SAFE_CONDITION_TOKEN_PATTERN
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("name LIKE '%test%'"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unrecognized tokens");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when contains negative numbers with minus sign")
            void shouldRejectSqlConditionFragment_whenContainsNegativeNumbers() {
                // Hyphen/minus is not in SAFE_CONDITION_TOKEN_PATTERN as a standalone token
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("status NOT IN (0, -1)"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unrecognized tokens");
            }
        }

        // ---------- 空值和空白片段 ----------

        @Nested
        @DisplayName("空值和空白片段")
        class NullAndBlankFragments {

            @Test
            @DisplayName("should accept null fragment when validate")
            void shouldAcceptNullFragment_whenValidate() {
                assertThatCode(() -> SqlIdentifierValidator.validateSqlConditionFragment(null))
                    .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("should accept empty fragment when validate")
            void shouldAcceptEmptyFragment_whenValidate() {
                assertThatCode(() -> SqlIdentifierValidator.validateSqlConditionFragment(""))
                    .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("should accept blank fragment when validate")
            void shouldAcceptBlankFragment_whenValidate() {
                assertThatCode(() -> SqlIdentifierValidator.validateSqlConditionFragment("   "))
                    .doesNotThrowAnyException();
            }
        }

        // ---------- 大小写不敏感 ----------

        @Nested
        @DisplayName("大小写不敏感检测")
        class CaseInsensitiveDetection {

            @Test
            @DisplayName("should reject SQL condition fragment when forbidden keyword in lowercase")
            void shouldRejectSqlConditionFragment_whenForbiddenKeywordInLowercase() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("drop table users"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("DROP");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when forbidden keyword in mixed case")
            void shouldRejectSqlConditionFragment_whenForbiddenKeywordInMixedCase() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("Drop Table users"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden keyword")
                    .hasMessageContaining("DROP");
            }

            @Test
            @DisplayName("should reject SQL condition fragment when DML keyword in lowercase")
            void shouldRejectSqlConditionFragment_whenDmlKeywordInLowercase() {
                assertThatThrownBy(() -> SqlIdentifierValidator.validateSqlConditionFragment("update users set name = #{val}"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("subquery or DML");
            }
        }
    }
}
