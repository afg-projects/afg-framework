package io.github.afgprojects.framework.core.security.datascope;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * DataScopeHelper 测试
 */
@DisplayName("DataScopeHelper 测试")
class DataScopeHelperTest {

    @Nested
    @DisplayName("注解解析测试")
    class AnnotationParsingTests {

        @Test
        @DisplayName("应该从方法上获取单个注解")
        void shouldGetSingleAnnotationFromMethod() throws NoSuchMethodException {
            // given
            Method method = TestMapper.class.getMethod("singleScope");
            Class<?> targetClass = TestMapper.class;

            // when
            List<DataScope> dataScopes = DataScopeHelper.getDataScopes(method, targetClass);

            // then
            assertThat(dataScopes).hasSize(1);
            assertThat(dataScopes.get(0).table()).isEqualTo("sys_user");
            assertThat(dataScopes.get(0).scopeType()).isEqualTo(DataScopeType.DEPT);
        }

        @Test
        @DisplayName("应该从方法上获取多个注解")
        void shouldGetMultipleAnnotationsFromMethod() throws NoSuchMethodException {
            // given
            Method method = TestMapper.class.getMethod("multipleScopes");
            Class<?> targetClass = TestMapper.class;

            // when
            List<DataScope> dataScopes = DataScopeHelper.getDataScopes(method, targetClass);

            // then
            assertThat(dataScopes).hasSize(2);
        }

        @Test
        @DisplayName("应该从类上获取注解")
        void shouldGetAnnotationFromClass() throws NoSuchMethodException {
            // given
            Method method = ClassScopeMapper.class.getMethod("noScope");
            Class<?> targetClass = ClassScopeMapper.class;

            // when
            List<DataScope> dataScopes = DataScopeHelper.getDataScopes(method, targetClass);

            // then
            assertThat(dataScopes).hasSize(1);
            assertThat(dataScopes.get(0).table()).isEqualTo("sys_dept");
        }
    }

    @Nested
    @DisplayName("SQL 条件生成测试")
    class ConditionBuildingTests {

        private final DataScopeProperties properties = new DataScopeProperties();

        @Test
        @DisplayName("应该生成本部门条件")
        void shouldBuildDeptCondition() {
            // given
            DataScope dataScope = createDataScope("sys_user", "dept_id", DataScopeType.DEPT);
            DataScopeContext context = DataScopeContext.builder()
                    .userId(100L)
                    .deptId(10L)
                    .build();

            // when
            String condition = DataScopeHelper.buildDataScopeCondition(dataScope, context, properties);

            // then
            assertThat(condition).isEqualTo("sys_user.dept_id = 10");
        }

        @Test
        @DisplayName("应该生成本人数据条件")
        void shouldBuildSelfCondition() {
            // given
            DataScope dataScope = createDataScope("sys_user", "create_by", DataScopeType.SELF);
            DataScopeContext context = DataScopeContext.builder()
                    .userId(100L)
                    .build();

            // when
            String condition = DataScopeHelper.buildDataScopeCondition(dataScope, context, properties);

            // then
            assertThat(condition).isEqualTo("sys_user.create_by = 100");
        }

        @Test
        @DisplayName("全部数据权限应该返回 null")
        void shouldReturnNullForAllScope() {
            // given
            DataScope dataScope = createDataScope("sys_user", "dept_id", DataScopeType.ALL);
            DataScopeContext context = DataScopeContext.builder()
                    .userId(100L)
                    .deptId(10L)
                    .build();

            // when
            String condition = DataScopeHelper.buildDataScopeCondition(dataScope, context, properties);

            // then
            assertThat(condition).isNull();
        }

        @Test
        @DisplayName("全部权限用户应该返回 null")
        void shouldReturnNullForAllPermissionUser() {
            // given
            DataScope dataScope = createDataScope("sys_user", "dept_id", DataScopeType.DEPT);
            DataScopeContext context = DataScopeContext.allPermission(100L);

            // when
            String condition = DataScopeHelper.buildDataScopeCondition(dataScope, context, properties);

            // then
            assertThat(condition).isNull();
        }

        @Test
        @DisplayName("忽略权限上下文应该返回 null")
        void shouldReturnNullForIgnoredContext() {
            // given
            DataScope dataScope = createDataScope("sys_user", "dept_id", DataScopeType.DEPT);
            DataScopeContext context = DataScopeContext.builder()
                    .userId(100L)
                    .deptId(10L)
                    .ignoreDataScope(true)
                    .build();

            // when
            String condition = DataScopeHelper.buildDataScopeCondition(dataScope, context, properties);

            // then
            assertThat(condition).isNull();
        }
    }

    @Nested
    @DisplayName("条件合并测试")
    class ConditionMergingTests {

        @Test
        @DisplayName("应该合并多个条件")
        void shouldMergeConditions() {
            // given
            List<String> conditions = List.of("a = 1", "b = 2", "c = 3");

            // when
            String merged = DataScopeHelper.mergeConditions(conditions);

            // then
            assertThat(merged).isEqualTo("a = 1 AND b = 2 AND c = 3");
        }

        @Test
        @DisplayName("应该过滤 null 条件")
        void shouldFilterNullConditions() {
            // given
            List<String> conditions = new java.util.ArrayList<>();
            conditions.add("a = 1");
            conditions.add(null);
            conditions.add("b = 2");
            conditions.add("");

            // when
            String merged = DataScopeHelper.mergeConditions(conditions);

            // then
            assertThat(merged).isEqualTo("a = 1 AND b = 2");
        }

        @Test
        @DisplayName("空列表应该返回 null")
        void shouldReturnNullForEmptyList() {
            assertThat(DataScopeHelper.mergeConditions(List.of())).isNull();
            assertThat(DataScopeHelper.mergeConditions(null)).isNull();
        }
    }

    @Nested
    @DisplayName("忽略列表测试")
    class IgnoreListTests {

        @Test
        @DisplayName("应该识别忽略的表")
        void shouldIdentifyIgnoredTable() {
            // given
            DataScopeProperties properties = new DataScopeProperties();
            properties.setIgnoreTables(new String[]{"sys_config", "sys_dict"});

            // then
            assertThat(DataScopeHelper.isIgnoredTable("sys_config", properties)).isTrue();
            assertThat(DataScopeHelper.isIgnoredTable("sys_dict", properties)).isTrue();
            assertThat(DataScopeHelper.isIgnoredTable("sys_user", properties)).isFalse();
        }

        @Test
        @DisplayName("应该识别忽略的方法")
        void shouldIdentifyIgnoredMethod() {
            // given
            DataScopeProperties properties = new DataScopeProperties();
            properties.setIgnoreMethods(new String[]{"select*", "get*"});

            // then
            assertThat(DataScopeHelper.isIgnoredMethod("selectList", properties)).isTrue();
            assertThat(DataScopeHelper.isIgnoredMethod("selectById", properties)).isTrue();
            assertThat(DataScopeHelper.isIgnoredMethod("getUser", properties)).isTrue();
            assertThat(DataScopeHelper.isIgnoredMethod("saveUser", properties)).isFalse();
        }
    }

    // ==================== 测试辅助 ====================

    /**
     * 创建 DataScope 注解实例
     */
    private DataScope createDataScope(String table, String column, DataScopeType scopeType) {
        return new DataScope() {
            @Override
            public String table() {
                return table;
            }

            @Override
            public String column() {
                return column;
            }

            @Override
            public DataScopeType scopeType() {
                return scopeType;
            }

            @Override
            public String customCondition() {
                return "";
            }

            @Override
            public String deptTable() {
                return "sys_dept";
            }

            @Override
            public String deptIdColumn() {
                return "id";
            }

            @Override
            public String deptParentColumn() {
                return "parent_id";
            }

            @Override
            public String userIdColumn() {
                return "create_by";
            }

            @Override
            public boolean ignoreTenant() {
                return false;
            }

            @Override
            public String aliasPrefix() {
                return "";
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return DataScope.class;
            }
        };
    }

    // ==================== 测试用 Mapper ====================

    interface TestMapper {
        @DataScope(table = "sys_user", column = "dept_id", scopeType = DataScopeType.DEPT)
        void singleScope();

        @DataScope(table = "sys_user", column = "dept_id", scopeType = DataScopeType.DEPT)
        @DataScope(table = "sys_order", column = "user_id", scopeType = DataScopeType.SELF)
        void multipleScopes();
    }

    @DataScope(table = "sys_dept", column = "id", scopeType = DataScopeType.DEPT)
    interface ClassScopeMapper {
        void noScope();
    }
}