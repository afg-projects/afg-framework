package io.github.afgprojects.framework.data.core.dialect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseTypeTest {

    @Test
    void shouldGetByCode() {
        assertThat(DatabaseType.fromCode("mysql")).isEqualTo(DatabaseType.MYSQL);
        assertThat(DatabaseType.fromCode("postgresql")).isEqualTo(DatabaseType.POSTGRESQL);
        assertThat(DatabaseType.fromCode("oceanbase")).isEqualTo(DatabaseType.OCEANBASE);
        assertThat(DatabaseType.fromCode("unknown")).isEqualTo(DatabaseType.UNKNOWN);
        assertThat(DatabaseType.fromCode(null)).isEqualTo(DatabaseType.UNKNOWN);
    }

    @Test
    void shouldCheckMySQLFamily() {
        assertThat(DatabaseType.MYSQL.isMySQLFamily()).isTrue();
        assertThat(DatabaseType.OCEANBASE.isMySQLFamily()).isTrue();
        assertThat(DatabaseType.POSTGRESQL.isMySQLFamily()).isFalse();
    }

    @Test
    void shouldCheckPostgreSQLFamily() {
        assertThat(DatabaseType.POSTGRESQL.isPostgreSQLFamily()).isTrue();
        assertThat(DatabaseType.OPENGAUSS.isPostgreSQLFamily()).isTrue();
        assertThat(DatabaseType.GAUSSDB.isPostgreSQLFamily()).isTrue();
        assertThat(DatabaseType.MYSQL.isPostgreSQLFamily()).isFalse();
    }

    @Test
    void shouldCheckChineseDatabase() {
        assertThat(DatabaseType.OCEANBASE.isChineseDatabase()).isTrue();
        assertThat(DatabaseType.OPENGAUSS.isChineseDatabase()).isTrue();
        assertThat(DatabaseType.DM.isChineseDatabase()).isTrue();
        assertThat(DatabaseType.KINGBASE.isChineseDatabase()).isTrue();
        assertThat(DatabaseType.GAUSSDB.isChineseDatabase()).isTrue();
        assertThat(DatabaseType.MYSQL.isChineseDatabase()).isFalse();
    }
}