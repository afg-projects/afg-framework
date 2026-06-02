package io.github.afgprojects.framework.data.jdbc.mapper;

import io.github.afgprojects.framework.data.core.mapper.MappingField;
import io.github.afgprojects.framework.data.core.mapper.TypeHandlerRegistry;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DtoMapperTest {

    @Test
    void shouldMapRecordFromResultSet() throws Exception {
        // Setup mock ResultSet
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(rs.getMetaData()).thenReturn(meta);
        when(meta.getColumnCount()).thenReturn(2);
        when(meta.getColumnLabel(1)).thenReturn("id");
        when(meta.getColumnLabel(2)).thenReturn("name");
        // DtoMapper uses readAndNormalizeValue which calls getObject(index)
        when(rs.getObject(1)).thenReturn(1L);
        when(rs.getObject(2)).thenReturn("test");

        DtoMapper<UserRecord> mapper = new DtoMapper<>(UserRecord.class, TypeHandlerRegistry.defaultRegistry());
        UserRecord result = mapper.map(rs, 1);

        assertEquals(1L, result.id());
        assertEquals("test", result.name());
    }

    @Test
    void shouldMapPojoFromResultSet() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(rs.getMetaData()).thenReturn(meta);
        when(meta.getColumnCount()).thenReturn(2);
        when(meta.getColumnLabel(1)).thenReturn("id");
        when(meta.getColumnLabel(2)).thenReturn("username");
        // DtoMapper uses readAndNormalizeValue which calls getObject(index)
        when(rs.getObject(1)).thenReturn(1L);
        when(rs.getObject(2)).thenReturn("test_user");

        DtoMapper<UserPojo> mapper = new DtoMapper<>(UserPojo.class, TypeHandlerRegistry.defaultRegistry());
        UserPojo result = mapper.map(rs, 1);

        assertEquals(1L, result.id);
        assertEquals("test_user", result.username);
    }

    @Test
    void shouldMapPojoWithMappingFieldAnnotation() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(rs.getMetaData()).thenReturn(meta);
        when(meta.getColumnCount()).thenReturn(2);
        when(meta.getColumnLabel(1)).thenReturn("id");
        when(meta.getColumnLabel(2)).thenReturn("user_name");
        // DtoMapper uses readAndNormalizeValue which calls getObject(index)
        when(rs.getObject(1)).thenReturn(1L);
        when(rs.getObject(2)).thenReturn("annotated");

        DtoMapper<AnnotatedPojo> mapper = new DtoMapper<>(AnnotatedPojo.class, TypeHandlerRegistry.defaultRegistry());
        AnnotatedPojo result = mapper.map(rs, 1);

        assertEquals(1L, result.id);
        assertEquals("annotated", result.displayName);
    }

    @Test
    void shouldMapSnakeCaseColumnsToCamelCaseFields() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(rs.getMetaData()).thenReturn(meta);
        when(meta.getColumnCount()).thenReturn(2);
        when(meta.getColumnLabel(1)).thenReturn("id");
        when(meta.getColumnLabel(2)).thenReturn("user_name");
        // DtoMapper uses readAndNormalizeValue which calls getObject(index)
        when(rs.getObject(1)).thenReturn(1L);
        when(rs.getObject(2)).thenReturn("snake_case");

        DtoMapper<SnakeCasePojo> mapper = new DtoMapper<>(SnakeCasePojo.class, TypeHandlerRegistry.defaultRegistry());
        SnakeCasePojo result = mapper.map(rs, 1);

        assertEquals(1L, result.id);
        assertEquals("snake_case", result.userName);
    }

    // ==================== Test DTOs ====================

    record UserRecord(Long id, String name) {}

    static class UserPojo {
        Long id;
        String username;

        public UserPojo() {}
    }

    static class AnnotatedPojo {
        Long id;

        @MappingField(column = "user_name")
        String displayName;

        public AnnotatedPojo() {}
    }

    static class SnakeCasePojo {
        Long id;
        String userName;

        public SnakeCasePojo() {}
    }
}
