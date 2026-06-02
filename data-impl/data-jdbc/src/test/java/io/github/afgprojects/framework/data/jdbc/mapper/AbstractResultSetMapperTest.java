package io.github.afgprojects.framework.data.jdbc.mapper;

import io.github.afgprojects.framework.data.core.mapper.ResultMapper;
import io.github.afgprojects.framework.data.core.mapper.TypeHandlerRegistry;
import microsoft.sql.DateTimeOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.postgresql.util.PGobject;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

/**
 * AbstractResultSetMapper 单元测试
 */
@DisplayName("AbstractResultSetMapper 测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AbstractResultSetMapperTest {

    private TestableResultSetMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TestableResultSetMapper(TypeHandlerRegistry.defaultRegistry());
    }

    // ==================== 具体子类实现，暴露 protected 方法 ====================

    /**
     * 可测试的 AbstractResultSetMapper 具体子类
     */
    static class TestableResultSetMapper extends AbstractResultSetMapper<Object> {

        TestableResultSetMapper(TypeHandlerRegistry typeHandlerRegistry) {
            super(typeHandlerRegistry);
        }

        @Override
        public Object map(ResultSet rs, int rowNum) throws SQLException {
            return null;
        }

        // 暴露 protected 方法供测试
        @Override
        public Object normalizeDatabaseSpecificValue(Object value) {
            return super.normalizeDatabaseSpecificValue(value);
        }

        @Override
        public String safeReadClob(Clob clob) throws SQLException {
            return super.safeReadClob(clob);
        }

        @Override
        public byte[] safeReadBlob(Blob blob) throws SQLException {
            return super.safeReadBlob(blob);
        }

        @Override
        public Map<String, Integer> buildColumnIndexMap(ResultSet rs) throws SQLException {
            return super.buildColumnIndexMap(rs);
        }

        @Override
        public Object readAndNormalizeValue(ResultSet rs, int i) throws SQLException {
            return super.readAndNormalizeValue(rs, i);
        }

        @Override
        public String getColumnLabel(ResultSetMetaData metaData, int columnIndex) throws SQLException {
            return super.getColumnLabel(metaData, columnIndex);
        }
    }

    // ==================== normalizeDatabaseSpecificValue 测试 ====================

    @Nested
    @DisplayName("normalizeDatabaseSpecificValue 测试")
    class NormalizeDatabaseSpecificValueTests {

        @Test
        @DisplayName("null 输入应返回 null")
        void shouldReturnNullForNullInput() {
            assertThat(mapper.normalizeDatabaseSpecificValue(null)).isNull();
        }

        @Test
        @DisplayName("普通 String 应原样返回")
        void shouldReturnStringUnchanged() {
            assertThat(mapper.normalizeDatabaseSpecificValue("hello")).isEqualTo("hello");
        }

        @Test
        @DisplayName("普通 Integer 应原样返回")
        void shouldReturnIntegerUnchanged() {
            assertThat(mapper.normalizeDatabaseSpecificValue(42)).isEqualTo(42);
        }

        @Test
        @DisplayName("PGobject 应提取 getValue() 的值")
        void shouldExtractValueFromPGobject() throws Exception {
            // Given: 使用测试替身创建 PGobject
            PGobject pgObject = new PGobject("json", "{\"key\":\"value\"}");

            // When
            Object result = mapper.normalizeDatabaseSpecificValue(pgObject);

            // Then
            assertThat(result).isEqualTo("{\"key\":\"value\"}");
        }

        @Test
        @DisplayName("PGobject JSONB 类型应提取值")
        void shouldExtractValueFromJsonbPGobject() throws Exception {
            PGobject pgObject = new PGobject("jsonb", "[1,2,3]");

            Object result = mapper.normalizeDatabaseSpecificValue(pgObject);

            assertThat(result).isEqualTo("[1,2,3]");
        }

        @Test
        @DisplayName("DateTimeOffset 应转换为 OffsetDateTime")
        void shouldConvertDateTimeOffsetToOffsetDateTime() {
            // Given
            OffsetDateTime expected = OffsetDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneOffset.ofHours(8));
            DateTimeOffset dto = DateTimeOffset.valueOf(expected);

            // When
            Object result = mapper.normalizeDatabaseSpecificValue(dto);

            // Then
            assertThat(result).isInstanceOf(OffsetDateTime.class);
            assertThat((OffsetDateTime) result).isEqualTo(expected);
        }

        @Test
        @DisplayName("PGobject getValue() 抛异常时应原样返回")
        void shouldReturnOriginalValueWhenPGobjectGetValueThrows() throws Exception {
            // Given: 创建一个 getValue() 抛异常的 PGobject
            PGobject pgObject = spy(new PGobject("json", "test"));
            doThrow(new RuntimeException("simulated error")).when(pgObject).getValue();

            // When
            Object result = mapper.normalizeDatabaseSpecificValue(pgObject);

            // Then: 应返回原始对象（降级处理）
            assertThat(result).isSameAs(pgObject);
        }

        @Test
        @DisplayName("不认识的类型应原样返回")
        void shouldReturnUnknownTypeUnchanged() {
            Object customObject = new Object();
            assertThat(mapper.normalizeDatabaseSpecificValue(customObject)).isSameAs(customObject);
        }

        @Test
        @DisplayName("Long 类型应原样返回")
        void shouldReturnLongUnchanged() {
            assertThat(mapper.normalizeDatabaseSpecificValue(123456L)).isEqualTo(123456L);
        }

        @Test
        @DisplayName("java.time.LocalDateTime 应原样返回")
        void shouldReturnLocalDateTimeUnchanged() {
            java.time.LocalDateTime ldt = java.time.LocalDateTime.of(2024, 1, 1, 0, 0);
            assertThat(mapper.normalizeDatabaseSpecificValue(ldt)).isEqualTo(ldt);
        }
    }

    // ==================== safeReadClob 测试 ====================

    @Nested
    @DisplayName("safeReadClob 测试")
    class SafeReadClobTests {

        @Test
        @DisplayName("正常大小的 CLOB 应使用 getSubString 读取")
        void shouldReadNormalClobWithGetSubString() throws Exception {
            // Given
            Clob clob = mock(Clob.class);
            when(clob.length()).thenReturn(5L);
            when(clob.getSubString(1, 5)).thenReturn("hello");

            // When
            String result = mapper.safeReadClob(clob);

            // Then
            assertThat(result).isEqualTo("hello");
            verify(clob).free();
        }

        @Test
        @DisplayName("超大 CLOB 应使用流式读取")
        void shouldReadLargeClobWithStream() throws Exception {
            // Given: length > Integer.MAX_VALUE
            Clob clob = mock(Clob.class);
            when(clob.length()).thenReturn((long) Integer.MAX_VALUE + 1);

            String content = "large content";
            Reader reader = new StringReader(content);
            when(clob.getCharacterStream()).thenReturn(reader);

            // When
            String result = mapper.safeReadClob(clob);

            // Then
            assertThat(result).isEqualTo("large content");
            verify(clob).free();
        }

        @Test
        @DisplayName("CLOB 读取完成后应调用 free()")
        void shouldCallFreeAfterReading() throws Exception {
            // Given
            Clob clob = mock(Clob.class);
            when(clob.length()).thenReturn(3L);
            when(clob.getSubString(1, 3)).thenReturn("abc");

            // When
            mapper.safeReadClob(clob);

            // Then
            verify(clob).free();
        }

        @Test
        @DisplayName("CLOB free() 抛异常不应影响结果")
        void shouldNotFailWhenFreeThrows() throws Exception {
            // Given
            Clob clob = mock(Clob.class);
            when(clob.length()).thenReturn(3L);
            when(clob.getSubString(1, 3)).thenReturn("abc");
            doThrow(new SQLException("free failed")).when(clob).free();

            // When & Then: 不应抛异常
            assertThatCode(() -> mapper.safeReadClob(clob))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("空 CLOB 应返回空字符串")
        void shouldReturnEmptyStringForEmptyClob() throws Exception {
            // Given
            Clob clob = mock(Clob.class);
            when(clob.length()).thenReturn(0L);
            when(clob.getSubString(1, 0)).thenReturn("");

            // When
            String result = mapper.safeReadClob(clob);

            // Then
            assertThat(result).isEmpty();
            verify(clob).free();
        }

        @Test
        @DisplayName("CLOB 流读取时 IOException 应包装为 SQLException")
        void shouldWrapIOExceptionAsSQLException() throws Exception {
            // Given: 超大 CLOB 但流读取失败
            Clob clob = mock(Clob.class);
            when(clob.length()).thenReturn((long) Integer.MAX_VALUE + 1);

            Reader brokenReader = mock(Reader.class);
            when(brokenReader.read(any(char[].class))).thenThrow(new IOException("stream broken"));
            when(clob.getCharacterStream()).thenReturn(brokenReader);

            // When & Then
            assertThatCode(() -> mapper.safeReadClob(clob))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("Failed to read CLOB stream")
                    .hasCauseInstanceOf(IOException.class);
        }
    }

    // ==================== safeReadBlob 测试 ====================

    @Nested
    @DisplayName("safeReadBlob 测试")
    class SafeReadBlobTests {

        @Test
        @DisplayName("正常大小的 BLOB 应使用 getBytes 读取")
        void shouldReadNormalBlobWithGetBytes() throws Exception {
            // Given
            byte[] data = new byte[]{1, 2, 3, 4, 5};
            Blob blob = mock(Blob.class);
            when(blob.length()).thenReturn(5L);
            when(blob.getBytes(1, 5)).thenReturn(data);

            // When
            byte[] result = mapper.safeReadBlob(blob);

            // Then
            assertThat(result).containsExactly(1, 2, 3, 4, 5);
            verify(blob).free();
        }

        @Test
        @DisplayName("超大 BLOB 应使用流式读取")
        void shouldReadLargeBlobWithStream() throws Exception {
            // Given: length > Integer.MAX_VALUE
            Blob blob = mock(Blob.class);
            when(blob.length()).thenReturn((long) Integer.MAX_VALUE + 1);

            byte[] data = new byte[]{10, 20, 30};
            java.io.InputStream inputStream = new java.io.ByteArrayInputStream(data);
            when(blob.getBinaryStream()).thenReturn(inputStream);

            // When
            byte[] result = mapper.safeReadBlob(blob);

            // Then
            assertThat(result).containsExactly(10, 20, 30);
            verify(blob).free();
        }

        @Test
        @DisplayName("BLOB 读取完成后应调用 free()")
        void shouldCallFreeAfterReading() throws Exception {
            // Given
            Blob blob = mock(Blob.class);
            when(blob.length()).thenReturn(2L);
            when(blob.getBytes(1, 2)).thenReturn(new byte[]{1, 2});

            // When
            mapper.safeReadBlob(blob);

            // Then
            verify(blob).free();
        }

        @Test
        @DisplayName("BLOB free() 抛异常不应影响结果")
        void shouldNotFailWhenFreeThrows() throws Exception {
            // Given
            Blob blob = mock(Blob.class);
            when(blob.length()).thenReturn(2L);
            when(blob.getBytes(1, 2)).thenReturn(new byte[]{1, 2});
            doThrow(new SQLException("free failed")).when(blob).free();

            // When & Then
            assertThatCode(() -> mapper.safeReadBlob(blob))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("空 BLOB 应返回空字节数组")
        void shouldReturnEmptyBytesForEmptyBlob() throws Exception {
            // Given
            Blob blob = mock(Blob.class);
            when(blob.length()).thenReturn(0L);
            when(blob.getBytes(1, 0)).thenReturn(new byte[]{});

            // When
            byte[] result = mapper.safeReadBlob(blob);

            // Then
            assertThat(result).isEmpty();
            verify(blob).free();
        }

        @Test
        @DisplayName("BLOB 流读取时 IOException 应包装为 SQLException")
        void shouldWrapIOExceptionAsSQLException() throws Exception {
            // Given: 超大 BLOB 但流读取失败
            Blob blob = mock(Blob.class);
            when(blob.length()).thenReturn((long) Integer.MAX_VALUE + 1);

            java.io.InputStream brokenStream = mock(java.io.InputStream.class);
            when(brokenStream.readAllBytes()).thenThrow(new IOException("stream broken"));
            when(blob.getBinaryStream()).thenReturn(brokenStream);

            // When & Then
            assertThatCode(() -> mapper.safeReadBlob(blob))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("Failed to read BLOB stream")
                    .hasCauseInstanceOf(IOException.class);
        }
    }

    // ==================== buildColumnIndexMap 测试 ====================

    @Nested
    @DisplayName("buildColumnIndexMap 测试")
    class BuildColumnIndexMapTests {

        @Test
        @DisplayName("应构建列名到索引的映射")
        void shouldBuildColumnIndexMap() throws Exception {
            // Given
            ResultSet rs = mock(ResultSet.class);
            ResultSetMetaData metaData = mock(ResultSetMetaData.class);
            when(rs.getMetaData()).thenReturn(metaData);
            when(metaData.getColumnCount()).thenReturn(3);
            when(metaData.getColumnLabel(1)).thenReturn("id");
            when(metaData.getColumnLabel(2)).thenReturn("name");
            when(metaData.getColumnLabel(3)).thenReturn("email");

            // When
            Map<String, Integer> map = mapper.buildColumnIndexMap(rs);

            // Then
            assertThat(map).hasSize(3);
            assertThat(map.get("id")).isEqualTo(1);
            assertThat(map.get("name")).isEqualTo(2);
            assertThat(map.get("email")).isEqualTo(3);
        }

        @Test
        @DisplayName("列名应转为小写键")
        void shouldUseLowercaseKeys() throws Exception {
            // Given: 模拟 Oracle 返回大写列名
            ResultSet rs = mock(ResultSet.class);
            ResultSetMetaData metaData = mock(ResultSetMetaData.class);
            when(rs.getMetaData()).thenReturn(metaData);
            when(metaData.getColumnCount()).thenReturn(3);
            when(metaData.getColumnLabel(1)).thenReturn("ID");
            when(metaData.getColumnLabel(2)).thenReturn("USER_NAME");
            when(metaData.getColumnLabel(3)).thenReturn("EMAIL");

            // When
            Map<String, Integer> map = mapper.buildColumnIndexMap(rs);

            // Then: 键应为小写
            assertThat(map).containsKey("id");
            assertThat(map).containsKey("user_name");
            assertThat(map).containsKey("email");
            assertThat(map.get("id")).isEqualTo(1);
            assertThat(map.get("user_name")).isEqualTo(2);
            assertThat(map.get("email")).isEqualTo(3);
        }

        @Test
        @DisplayName("空列标签应跳过")
        void shouldSkipEmptyColumnLabel() throws Exception {
            // Given
            ResultSet rs = mock(ResultSet.class);
            ResultSetMetaData metaData = mock(ResultSetMetaData.class);
            when(rs.getMetaData()).thenReturn(metaData);
            when(metaData.getColumnCount()).thenReturn(3);
            when(metaData.getColumnLabel(1)).thenReturn("id");
            when(metaData.getColumnLabel(2)).thenReturn("");
            when(metaData.getColumnLabel(3)).thenReturn("name");

            // When
            Map<String, Integer> map = mapper.buildColumnIndexMap(rs);

            // Then
            assertThat(map).hasSize(2);
            assertThat(map).containsKey("id");
            assertThat(map).containsKey("name");
            assertThat(map).doesNotContainKey("");
        }

        @Test
        @DisplayName("null 列标签应跳过")
        void shouldSkipNullColumnLabel() throws Exception {
            // Given
            ResultSet rs = mock(ResultSet.class);
            ResultSetMetaData metaData = mock(ResultSetMetaData.class);
            when(rs.getMetaData()).thenReturn(metaData);
            when(metaData.getColumnCount()).thenReturn(2);
            when(metaData.getColumnLabel(1)).thenReturn(null);
            when(metaData.getColumnLabel(2)).thenReturn("name");

            // When
            Map<String, Integer> map = mapper.buildColumnIndexMap(rs);

            // Then
            assertThat(map).hasSize(1);
            assertThat(map).containsKey("name");
        }

        @Test
        @DisplayName("大小写混合列名应统一为小写键")
        void shouldNormalizeMixedCaseColumns() throws Exception {
            // Given
            ResultSet rs = mock(ResultSet.class);
            ResultSetMetaData metaData = mock(ResultSetMetaData.class);
            when(rs.getMetaData()).thenReturn(metaData);
            when(metaData.getColumnCount()).thenReturn(2);
            when(metaData.getColumnLabel(1)).thenReturn("UserId");
            when(metaData.getColumnLabel(2)).thenReturn("CreateTime");

            // When
            Map<String, Integer> map = mapper.buildColumnIndexMap(rs);

            // Then
            assertThat(map).containsKey("userid");
            assertThat(map).containsKey("createtime");
        }

        @Test
        @DisplayName("空 ResultSet 应返回空映射")
        void shouldReturnEmptyMapForEmptyResultSet() throws Exception {
            // Given
            ResultSet rs = mock(ResultSet.class);
            ResultSetMetaData metaData = mock(ResultSetMetaData.class);
            when(rs.getMetaData()).thenReturn(metaData);
            when(metaData.getColumnCount()).thenReturn(0);

            // When
            Map<String, Integer> map = mapper.buildColumnIndexMap(rs);

            // Then
            assertThat(map).isEmpty();
        }
    }

    // ==================== getColumnLabel 测试 ====================

    @Nested
    @DisplayName("getColumnLabel 测试")
    class GetColumnLabelTests {

        @Test
        @DisplayName("getColumnLabel 正常时应返回列标签")
        void shouldReturnColumnLabel() throws Exception {
            ResultSetMetaData metaData = mock(ResultSetMetaData.class);
            when(metaData.getColumnLabel(1)).thenReturn("alias_name");

            String result = mapper.getColumnLabel(metaData, 1);

            assertThat(result).isEqualTo("alias_name");
        }

        @Test
        @DisplayName("getColumnLabel 失败时应回退到 getColumnName")
        void shouldFallbackToColumnNameWhenGetColumnLabelFails() throws Exception {
            ResultSetMetaData metaData = mock(ResultSetMetaData.class);
            when(metaData.getColumnLabel(1)).thenThrow(new SQLException("not supported"));
            when(metaData.getColumnName(1)).thenReturn("original_name");

            String result = mapper.getColumnLabel(metaData, 1);

            assertThat(result).isEqualTo("original_name");
        }
    }

    // ==================== readAndNormalizeValue 测试 ====================

    @Nested
    @DisplayName("readAndNormalizeValue 测试")
    class ReadAndNormalizeValueTests {

        @Test
        @DisplayName("普通值应原样读取和规范化")
        void shouldReadAndNormalizeRegularValue() throws Exception {
            // Given
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn("hello");

            // When
            Object result = mapper.readAndNormalizeValue(rs, 1);

            // Then
            assertThat(result).isEqualTo("hello");
        }

        @Test
        @DisplayName("CLOB 值应安全读取为字符串")
        void shouldReadClobAsString() throws Exception {
            // Given
            ResultSet rs = mock(ResultSet.class);
            Clob clob = mock(Clob.class);
            when(rs.getObject(1)).thenReturn(clob);
            when(clob.length()).thenReturn(5L);
            when(clob.getSubString(1, 5)).thenReturn("clob!");

            // When
            Object result = mapper.readAndNormalizeValue(rs, 1);

            // Then
            assertThat(result).isInstanceOf(String.class).isEqualTo("clob!");
        }

        @Test
        @DisplayName("BLOB 值应安全读取为字节数组")
        void shouldReadBlobAsByteArray() throws Exception {
            // Given
            ResultSet rs = mock(ResultSet.class);
            Blob blob = mock(Blob.class);
            when(rs.getObject(1)).thenReturn(blob);
            byte[] data = new byte[]{1, 2, 3};
            when(blob.length()).thenReturn(3L);
            when(blob.getBytes(1, 3)).thenReturn(data);

            // When
            Object result = mapper.readAndNormalizeValue(rs, 1);

            // Then
            assertThat(result).isInstanceOf(byte[].class);
            assertThat((byte[]) result).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("PGobject 应规范化为字符串")
        void shouldNormalizePGobjectValue() throws Exception {
            // Given
            ResultSet rs = mock(ResultSet.class);
            PGobject pgObject = new PGobject("json", "{\"name\":\"test\"}");
            when(rs.getObject(1)).thenReturn(pgObject);

            // When
            Object result = mapper.readAndNormalizeValue(rs, 1);

            // Then
            assertThat(result).isInstanceOf(String.class).isEqualTo("{\"name\":\"test\"}");
        }

        @Test
        @DisplayName("DateTimeOffset 应规范化为 OffsetDateTime")
        void shouldNormalizeDateTimeOffsetValue() throws Exception {
            // Given
            ResultSet rs = mock(ResultSet.class);
            OffsetDateTime expected = OffsetDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneOffset.ofHours(8));
            DateTimeOffset dto = DateTimeOffset.valueOf(expected);
            when(rs.getObject(1)).thenReturn(dto);

            // When
            Object result = mapper.readAndNormalizeValue(rs, 1);

            // Then
            assertThat(result).isInstanceOf(OffsetDateTime.class);
            assertThat((OffsetDateTime) result).isEqualTo(expected);
        }

        @Test
        @DisplayName("null 值应返回 null")
        void shouldReturnNullForNullValue() throws Exception {
            // Given
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(1)).thenReturn(null);

            // When
            Object result = mapper.readAndNormalizeValue(rs, 1);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("CLOB 读取后仍应进行数据库特定类型规范化")
        void shouldStillNormalizeAfterClobRead() throws Exception {
            // Given: CLOB 读取返回的 String 不需要规范化，但验证流程
            ResultSet rs = mock(ResultSet.class);
            Clob clob = mock(Clob.class);
            when(rs.getObject(1)).thenReturn(clob);
            when(clob.length()).thenReturn(4L);
            when(clob.getSubString(1, 4)).thenReturn("text");

            // When
            Object result = mapper.readAndNormalizeValue(rs, 1);

            // Then: CLOB 被读取为 String，再经过 normalizeDatabaseSpecificValue（String 直接返回）
            assertThat(result).isEqualTo("text");
        }

        @Test
        @DisplayName("应使用正确的列索引读取值")
        void shouldReadWithCorrectColumnIndex() throws Exception {
            // Given
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject(3)).thenReturn("column_3_value");

            // When
            Object result = mapper.readAndNormalizeValue(rs, 3);

            // Then
            verify(rs).getObject(3);
            assertThat(result).isEqualTo("column_3_value");
        }
    }
}
