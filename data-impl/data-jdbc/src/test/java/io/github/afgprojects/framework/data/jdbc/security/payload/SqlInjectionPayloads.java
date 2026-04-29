package io.github.afgprojects.framework.data.jdbc.security.payload;

import java.util.List;
import java.util.stream.Stream;

/**
 * SQL 注入 Payload 库
 * <p>
 * 包含各类 SQL 注入攻击向量，用于安全测试。
 * </p>
 */
public final class SqlInjectionPayloads {

    private SqlInjectionPayloads() {}

    /**
     * 经典 SQL 注入 Payload
     */
    public static final List<String> CLASSIC = List.of(
        "' OR '1'='1",
        "' OR '1'='1'--",
        "' OR '1'='1'/*",
        "1' OR '1'='1",
        "1 OR 1=1",
        "1 OR 1=1--",
        "' OR ''='",
        "1' OR '1'='1'+'",
        "admin'--",
        "admin' #"
    );

    /**
     * UNION 注入 Payload
     */
    public static final List<String> UNION = List.of(
        "' UNION SELECT NULL--",
        "' UNION SELECT NULL, NULL--",
        "' UNION SELECT NULL, NULL, NULL--",
        "' UNION SELECT username, password FROM users--",
        "' UNION ALL SELECT NULL--",
        "1 UNION SELECT NULL--",
        "' UNION SELECT table_name FROM information_schema.tables--"
    );

    /**
     * 时间盲注 Payload
     */
    public static final List<String> TIME_BASED = List.of(
        "'; SELECT SLEEP(5)--",
        "'; SELECT pg_sleep(5)--",
        "'; WAITFOR DELAY '0:0:5'--",
        "'; SELECT dbms_pipe.receive_message('a',5) FROM dual--",
        "'; BENCHMARK(10000000,SHA1('test'))--"
    );

    /**
     * 错误注入 Payload
     */
    public static final List<String> ERROR_BASED = List.of(
        "' AND EXTRACTVALUE(1, CONCAT(0x7e, VERSION()))--",
        "' AND UPDATEXML(1, CONCAT(0x7e, VERSION()), 1)--",
        "' AND 1=CONVERT(int, @@version)--",
        "' AND EXP(~(SELECT * FROM (SELECT VERSION())a))--"
    );

    /**
     * 堆叠查询 Payload
     */
    public static final List<String> STACKED = List.of(
        "'; DROP TABLE users--",
        "'; INSERT INTO users VALUES ('hacker', 'password')--",
        "'; UPDATE users SET password='hacked' WHERE '1'='1",
        "'; DELETE FROM users WHERE '1'='1--"
    );

    /**
     * 注释绕过 Payload
     */
    public static final List<String> COMMENT_BYPASS = List.of(
        "'/**/OR/**/1=1--",
        "'%0aOR%0a1=1--",
        "'\nOR\n1=1--",
        "'\tOR\t1=1--",
        "'/*!OR*/1=1--"
    );

    /**
     * 获取所有经典注入 Payload 流
     */
    public static Stream<String> classic() {
        return CLASSIC.stream();
    }

    /**
     * 获取所有 UNION 注入 Payload 流
     */
    public static Stream<String> union() {
        return UNION.stream();
    }

    /**
     * 获取所有时间盲注 Payload 流
     */
    public static Stream<String> timeBased() {
        return TIME_BASED.stream();
    }

    /**
     * 获取所有错误注入 Payload 流
     */
    public static Stream<String> errorBased() {
        return ERROR_BASED.stream();
    }

    /**
     * 获取所有堆叠查询 Payload 流
     */
    public static Stream<String> stacked() {
        return STACKED.stream();
    }

    /**
     * 获取所有注释绕过 Payload 流
     */
    public static Stream<String> commentBypass() {
        return COMMENT_BYPASS.stream();
    }

    /**
     * 获取所有 SQL 注入 Payload 流
     */
    public static Stream<String> all() {
        return Stream.of(
            CLASSIC, UNION, TIME_BASED, ERROR_BASED, STACKED, COMMENT_BYPASS
        ).flatMap(List::stream);
    }
}
