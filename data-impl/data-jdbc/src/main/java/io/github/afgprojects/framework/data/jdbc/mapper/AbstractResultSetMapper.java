package io.github.afgprojects.framework.data.jdbc.mapper;

import io.github.afgprojects.framework.data.core.mapper.ResultMapper;
import io.github.afgprojects.framework.data.core.mapper.TypeHandlerRegistry;
import lombok.extern.slf4j.Slf4j;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * ResultSet 映射抽象基类
 * <p>
 * 提供 ResultSet 到对象映射的公共能力：
 * <ul>
 *   <li>数据库特定类型规范化（PGobject、DateTimeOffset 等）</li>
 *   <li>安全 LOB 读取（CLOB/BLOB，支持超大字段）</li>
 *   <li>列名到索引映射（大小写不敏感）</li>
 * </ul>
 *
 * @param <R> 映射目标类型
 */
@Slf4j
public abstract class AbstractResultSetMapper<R> implements ResultMapper<R> {

    protected final TypeHandlerRegistry typeHandlerRegistry;

    protected AbstractResultSetMapper(TypeHandlerRegistry typeHandlerRegistry) {
        this.typeHandlerRegistry = typeHandlerRegistry;
    }

    // ==================== 数据库特定类型规范化 ====================

    /**
     * 规范化数据库特定的返回值类型
     * <p>
     * 不同数据库 JDBC 驱动对同一列类型可能返回不同的 Java 类型：
     * <ul>
     *   <li>PostgreSQL JSON/JSONB → org.postgresql.util.PGobject</li>
     *   <li>SQL Server DATETIMEOFFSET → microsoft.sql.DateTimeOffset</li>
     * </ul>
     * 此方法将这些类型统一规范化为标准 Java 类型，后续 TypeHandler 可以正常处理。
     *
     * @param value 原始值
     * @return 规范化后的值
     */
    protected Object normalizeDatabaseSpecificValue(Object value) {
        if (value == null) return null;
        String className = value.getClass().getName();

        // PostgreSQL PGobject -> String（JSON 值）
        if ("org.postgresql.util.PGobject".equals(className)) {
            try {
                return value.getClass().getMethod("getValue").invoke(value);
            } catch (Exception e) {
                log.debug("Failed to normalize PGobject", e);
            }
        }

        // SQL Server DateTimeOffset -> OffsetDateTime
        if ("microsoft.sql.DateTimeOffset".equals(className)) {
            try {
                return value.getClass().getMethod("getOffsetDateTime").invoke(value);
            } catch (Exception e) {
                log.debug("Failed to normalize DateTimeOffset", e);
            }
        }

        return value;
    }

    // ==================== 安全 LOB 读取 ====================

    /**
     * 安全读取 CLOB 值
     * <p>
     * 处理超大 CLOB（超过 Integer.MAX_VALUE 字符），使用流式读取避免溢出。
     * 读取完成后调用 {@link Clob#free()} 释放资源。
     *
     * @param clob CLOB 对象
     * @return 字符串值
     * @throws SQLException 数据库异常
     */
    protected String safeReadClob(Clob clob) throws SQLException {
        try {
            long length = clob.length();
            if (length > Integer.MAX_VALUE) {
                try (var reader = clob.getCharacterStream()) {
                    StringBuilder sb = new StringBuilder();
                    char[] buffer = new char[8192];
                    int read;
                    while ((read = reader.read(buffer)) != -1) {
                        sb.append(buffer, 0, read);
                    }
                    return sb.toString();
                } catch (java.io.IOException e) {
                    throw new SQLException("Failed to read CLOB stream", e);
                }
            }
            return clob.getSubString(1, (int) length);
        } finally {
            try { clob.free(); } catch (Exception ignored) {}
        }
    }

    /**
     * 安全读取 BLOB 值
     * <p>
     * 处理超大 BLOB（超过 Integer.MAX_VALUE 字节），使用流式读取避免溢出。
     * 读取完成后调用 {@link Blob#free()} 释放资源。
     *
     * @param blob BLOB 对象
     * @return 字节数组
     * @throws SQLException 数据库异常
     */
    protected byte[] safeReadBlob(Blob blob) throws SQLException {
        try {
            long length = blob.length();
            if (length > Integer.MAX_VALUE) {
                try (var is = blob.getBinaryStream()) {
                    return is.readAllBytes();
                } catch (java.io.IOException e) {
                    throw new SQLException("Failed to read BLOB stream", e);
                }
            }
            return blob.getBytes(1, (int) length);
        } finally {
            try { blob.free(); } catch (Exception ignored) {}
        }
    }

    // ==================== 列名映射 ====================

    /**
     * 构建 ResultSet 列名到列索引的映射（大小写不敏感）
     * <p>
     * 不同数据库对列名大小写处理方式不同：
     * <ul>
     *   <li>Oracle 默认返回大写列名</li>
     *   <li>PostgreSQL 默认返回小写列名</li>
     *   <li>MySQL 保留原始大小写</li>
     * </ul>
     * 使用小写 key 确保跨数据库兼容。
     *
     * @param rs ResultSet
     * @return 列名（小写）→ 列索引的映射
     * @throws SQLException 数据库异常
     */
    protected Map<String, Integer> buildColumnIndexMap(ResultSet rs) throws SQLException {
        Map<String, Integer> map = new HashMap<>();
        ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String label = getColumnLabel(metaData, i);
            if (label != null && !label.isEmpty()) {
                map.put(label.toLowerCase(), i);
            }
        }
        return map;
    }

    /**
     * 获取列标签（优先使用别名）
     *
     * @param metaData    ResultSet 元数据
     * @param columnIndex 列索引
     * @return 列标签
     * @throws SQLException 数据库异常
     */
    protected String getColumnLabel(ResultSetMetaData metaData, int columnIndex) throws SQLException {
        try {
            return metaData.getColumnLabel(columnIndex);
        } catch (Exception e) {
            return metaData.getColumnName(columnIndex);
        }
    }

    // ==================== 值读取与规范化 ====================

    /**
     * 从 ResultSet 读取值并进行数据库特定类型规范化
     * <p>
     * 处理顺序：
     * 1. 获取原始值（rs.getObject）
     * 2. CLOB → String（安全读取）
     * 3. BLOB → byte[]（安全读取）
     * 4. 数据库特定类型规范化（PGobject、DateTimeOffset 等）
     *
     * @param rs  ResultSet
     * @param i   列索引
     * @return 规范化后的值
     * @throws SQLException 数据库异常
     */
    protected Object readAndNormalizeValue(ResultSet rs, int i) throws SQLException {
        Object value = rs.getObject(i);

        // CLOB 处理
        if (value instanceof Clob clob) {
            value = safeReadClob(clob);
        }
        // BLOB 处理
        else if (value instanceof Blob blob) {
            value = safeReadBlob(blob);
        }

        // 数据库特定类型规范化
        value = normalizeDatabaseSpecificValue(value);

        return value;
    }
}
