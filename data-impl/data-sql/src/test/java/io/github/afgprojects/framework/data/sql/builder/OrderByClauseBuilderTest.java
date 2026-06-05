package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.dialect.MySQLDialect;
import io.github.afgprojects.framework.data.core.query.Sort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OrderByClauseBuilder 单元测试
 */
@DisplayName("OrderByClauseBuilder 测试")
class OrderByClauseBuilderTest {

    private final MySQLDialect dialect = new MySQLDialect();
    private OrderByClauseBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new OrderByClauseBuilder(dialect);
    }

    @Nested
    @DisplayName("空构建器")
    class EmptyBuilderTests {

        @Test
        @DisplayName("空构建器 build 返回空字符串")
        void shouldReturnEmptyString_whenEmptyBuilder() {
            assertThat(builder.build()).isEmpty();
        }

        @Test
        @DisplayName("空构建器 hasOrderBy 返回 false")
        void shouldReturnFalse_whenNoOrderBy() {
            assertThat(builder.hasOrderBy()).isFalse();
        }
    }

    @Nested
    @DisplayName("Sort 对象排序")
    class SortObjectTests {

        @Test
        @DisplayName("orderBy(Sort) 设置排序")
        void shouldSetOrderByWithSort() {
            Sort sort = Sort.asc("name");
            builder.orderBy(sort);

            String sql = builder.build();
            assertThat(sql).isEqualTo(" ORDER BY `name` ASC");
        }

        @Test
        @DisplayName("orderBy(Sort) 多列排序")
        void shouldSetOrderByWithMultiColumnSort() {
            Sort sort = Sort.by(Sort.Order.asc("name"), Sort.Order.desc("age"));
            builder.orderBy(sort);

            String sql = builder.build();
            assertThat(sql).isEqualTo(" ORDER BY `name` ASC, `age` DESC");
        }

        @Test
        @DisplayName("UNSORTED Sort 不生成 ORDER BY")
        void shouldNotGenerateOrderBy_whenUnsorted() {
            Sort sort = Sort.unsorted();
            builder.orderBy(sort);

            assertThat(builder.build()).isEmpty();
            assertThat(builder.hasOrderBy()).isFalse();
        }
    }

    @Nested
    @DisplayName("列+方向排序")
    class ColumnAndDirectionTests {

        @Test
        @DisplayName("orderBy(column, ASC) 生成升序排序")
        void shouldGenerateAscendingOrder() {
            builder.orderBy("name", Sort.Direction.ASC);

            String sql = builder.build();
            assertThat(sql).isEqualTo(" ORDER BY `name` ASC");
        }

        @Test
        @DisplayName("orderBy(column, DESC) 生成降序排序")
        void shouldGenerateDescendingOrder() {
            builder.orderBy("age", Sort.Direction.DESC);

            String sql = builder.build();
            assertThat(sql).isEqualTo(" ORDER BY `age` DESC");
        }
    }

    @Nested
    @DisplayName("hasOrderBy")
    class HasOrderByTests {

        @Test
        @DisplayName("设置排序后 hasOrderBy 返回 true")
        void shouldReturnTrue_whenOrderBySet() {
            builder.orderBy("name", Sort.Direction.ASC);
            assertThat(builder.hasOrderBy()).isTrue();
        }

        @Test
        @DisplayName("UNSORTED 后 hasOrderBy 返回 false")
        void shouldReturnFalse_whenUnsorted() {
            builder.orderBy(Sort.unsorted());
            assertThat(builder.hasOrderBy()).isFalse();
        }
    }

    @Nested
    @DisplayName("getSort 和 setSort")
    class AccessorTests {

        @Test
        @DisplayName("getSort 返回设置的 Sort 对象")
        void shouldReturnSortObject() {
            Sort sort = Sort.desc("created_at");
            builder.orderBy(sort);
            assertThat(builder.getSort()).isEqualTo(sort);
        }

        @Test
        @DisplayName("setSort 覆盖排序")
        void shouldOverrideSortViaSetter() {
            builder.orderBy("name", Sort.Direction.ASC);
            Sort newSort = Sort.desc("id");
            builder.setSort(newSort);

            String sql = builder.build();
            assertThat(sql).isEqualTo(" ORDER BY `id` DESC");
        }

        @Test
        @DisplayName("空构建器 getSort 返回 null")
        void shouldReturnNull_whenNoSort() {
            assertThat(builder.getSort()).isNull();
        }
    }

    @Nested
    @DisplayName("多排序组合")
    class MultipleSortTests {

        @Test
        @DisplayName("Sort.and() 组合多排序")
        void shouldCombineMultipleSorts() {
            Sort sort1 = Sort.asc("name");
            Sort sort2 = Sort.desc("age");
            Sort combined = sort1.and(sort2);

            builder.orderBy(combined);
            String sql = builder.build();
            assertThat(sql).isEqualTo(" ORDER BY `name` ASC, `age` DESC");
        }
    }
}
