package io.github.afgprojects.framework.data.jdbc.security.payload;

import java.util.List;
import java.util.stream.Stream;

/**
 * 数据库特定注入 Payload 库
 */
public final class DatabaseSpecificPayloads {

    private DatabaseSpecificPayloads() {}

    // ==================== H2 数据库 ====================

    public static final List<String> H2_INJECTION = List.of(
        "'; CREATE ALIAS SLEEP AS 'void sleep(long ms) { try { Thread.sleep(ms); } catch (Exception e) {} }'--",
        "'; CALL RANDOM()--",
        "'; CALL CSVREAD('/etc/passwd')--",
        "'; CALL CSVWRITE('/tmp/data.csv', 'SELECT * FROM users')--"
    );

    // ==================== PostgreSQL ====================

    public static final List<String> POSTGRESQL_INJECTION = List.of(
        "'; SELECT pg_sleep(5)--",
        "'; COPY users TO '/tmp/users.txt'--",
        "'; SELECT pg_read_file('/etc/passwd')--",
        "'; SELECT pg_read_binary_file('/etc/passwd')--",
        "'; DROP FUNCTION IF EXISTS malicious_function; CREATE FUNCTION malicious_function() RETURNS void AS $$ BEGIN EXECUTE 'DROP TABLE users'; END; $$ LANGUAGE plpgsql;--"
    );

    // ==================== MySQL ====================

    public static final List<String> MYSQL_INJECTION = List.of(
        "'; SELECT SLEEP(5)--",
        "'; LOAD_FILE('/etc/passwd')--",
        "' INTO OUTFILE '/tmp/data.txt'--",
        "'; SELECT ... INTO OUTFILE '/tmp/users.csv' FIELDS TERMINATED BY ','--",
        "'; SHOW VARIABLES LIKE '%version%'--"
    );

    // ==================== SQL Server ====================

    public static final List<String> SQLSERVER_INJECTION = List.of(
        "'; WAITFOR DELAY '0:0:5'--",
        "'; EXEC xp_cmdshell('dir')--",
        "'; EXEC sp_executesql N'SELECT * FROM users'--",
        "'; SELECT * FROM OPENROWSET('SQLOLEDB', 'server'; 'user'; 'pass', 'SELECT * FROM users')--"
    );

    // ==================== 按数据库类型获取 ====================

    public static Stream<String> h2() {
        return H2_INJECTION.stream();
    }

    public static Stream<String> postgresql() {
        return POSTGRESQL_INJECTION.stream();
    }

    public static Stream<String> mysql() {
        return MYSQL_INJECTION.stream();
    }

    public static Stream<String> sqlserver() {
        return SQLSERVER_INJECTION.stream();
    }

    /**
     * 根据数据库类型获取对应 Payload
     */
    public static Stream<String> byDatabase(String databaseType) {
        return switch (databaseType.toLowerCase()) {
            case "h2" -> h2();
            case "postgresql", "postgres" -> postgresql();
            case "mysql" -> mysql();
            case "sqlserver", "mssql" -> sqlserver();
            default -> Stream.empty();
        };
    }
}
