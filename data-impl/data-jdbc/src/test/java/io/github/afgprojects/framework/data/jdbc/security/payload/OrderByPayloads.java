package io.github.afgprojects.framework.data.jdbc.security.payload;

import java.util.List;
import java.util.stream.Stream;

/**
 * ORDER BY 注入 Payload 库
 */
public final class OrderByPayloads {

    private OrderByPayloads() {}

    /**
     * ORDER BY 列名注入 Payload
     */
    public static final List<String> COLUMN_INJECTION = List.of(
        "id; DROP TABLE users--",
        "id; SELECT * FROM information_schema.tables--",
        "id; INSERT INTO users VALUES ('hacker', 'password')--",
        "id INTO OUTFILE '/tmp/data.txt'--",
        "id; LOAD_FILE('/etc/passwd')--"
    );

    /**
     * ORDER BY 条件注入 Payload
     */
    public static final List<String> CONDITION_INJECTION = List.of(
        "(SELECT CASE WHEN (1=1) THEN id ELSE name END)",
        "id ASC, IF(1=1, id, name)",
        "id ASC, (SELECT CASE WHEN (1=1) THEN 1 ELSE 1/0 END)",
        "(IF(SUBSTRING(user(),1,1)='r', id, name))"
    );

    /**
     * ORDER BY 时间盲注 Payload
     */
    public static final List<String> TIME_BASED = List.of(
        "id; SELECT SLEEP(5)--",
        "id; SELECT pg_sleep(5)--",
        "id; WAITFOR DELAY '0:0:5'--"
    );

    /**
     * ORDER BY 错误注入 Payload
     */
    public static final List<String> ERROR_BASED = List.of(
        "id AND EXTRACTVALUE(1, CONCAT(0x7e, VERSION()))--",
        "id AND UPDATEXML(1, CONCAT(0x7e, VERSION()), 1)--",
        "id; EXEC xp_cmdshell('dir')--"
    );

    public static Stream<String> columnInjection() {
        return COLUMN_INJECTION.stream();
    }

    public static Stream<String> conditionInjection() {
        return CONDITION_INJECTION.stream();
    }

    public static Stream<String> timeBased() {
        return TIME_BASED.stream();
    }

    public static Stream<String> errorBased() {
        return ERROR_BASED.stream();
    }

    public static Stream<String> all() {
        return Stream.of(
            COLUMN_INJECTION, CONDITION_INJECTION, TIME_BASED, ERROR_BASED
        ).flatMap(List::stream);
    }
}
