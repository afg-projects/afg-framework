package io.github.afgprojects.framework.data.core.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sort 测试
 */
@DisplayName("Sort 测试")
class SortTest {

    @Nested
    @DisplayName("unsorted 方法")
    class UnsortedTests {

        @Test
        @DisplayName("unsorted() 应返回空排序")
        void shouldReturnEmptySort_whenUnsorted() {
            Sort sort = Sort.unsorted();
            assertThat(sort.isUnsorted()).isTrue();
            assertThat(sort.isSorted()).isFalse();
            assertThat(sort.getOrders()).isEmpty();
        }
    }

    @Nested
    @DisplayName("asc 方法")
    class AscTests {

        @Test
        @DisplayName("asc(\"name\") 应返回升序排序")
        void shouldReturnAscSort_whenAsc() {
            Sort sort = Sort.asc("name");
            assertThat(sort.isSorted()).isTrue();
            assertThat(sort.getOrders()).hasSize(1);
            assertThat(sort.getOrders().get(0).getProperty()).isEqualTo("name");
            assertThat(sort.getOrders().get(0).getDirection()).isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("asc(\"name\", \"age\") 应返回两个升序排序项")
        void shouldReturnTwoAscOrders_whenAscWithMultipleProperties() {
            Sort sort = Sort.asc("name", "age");
            assertThat(sort.getOrders()).hasSize(2);
            assertThat(sort.getOrders().get(0).getProperty()).isEqualTo("name");
            assertThat(sort.getOrders().get(0).isAscending()).isTrue();
            assertThat(sort.getOrders().get(1).getProperty()).isEqualTo("age");
            assertThat(sort.getOrders().get(1).isAscending()).isTrue();
        }
    }

    @Nested
    @DisplayName("desc 方法")
    class DescTests {

        @Test
        @DisplayName("desc(\"name\") 应返回降序排序")
        void shouldReturnDescSort_whenDesc() {
            Sort sort = Sort.desc("name");
            assertThat(sort.isSorted()).isTrue();
            assertThat(sort.getOrders()).hasSize(1);
            assertThat(sort.getOrders().get(0).getProperty()).isEqualTo("name");
            assertThat(sort.getOrders().get(0).getDirection()).isEqualTo(Sort.Direction.DESC);
        }
    }

    @Nested
    @DisplayName("and 方法")
    class AndTests {

        @Test
        @DisplayName("and(sort) 应追加排序")
        void shouldAppendSort_whenAnd() {
            Sort sort1 = Sort.asc("name");
            Sort sort2 = Sort.desc("age");
            Sort combined = sort1.and(sort2);

            assertThat(combined.getOrders()).hasSize(2);
            assertThat(combined.getOrders().get(0).getProperty()).isEqualTo("name");
            assertThat(combined.getOrders().get(0).isAscending()).isTrue();
            assertThat(combined.getOrders().get(1).getProperty()).isEqualTo("age");
            assertThat(combined.getOrders().get(1).isDescending()).isTrue();
        }

        @Test
        @DisplayName("and(UNSORTED) 应返回原排序")
        void shouldReturnOriginal_whenAndUnsorted() {
            Sort sort = Sort.asc("name");
            Sort result = sort.and(Sort.unsorted());
            assertThat(result).isSameAs(sort);
        }

        @Test
        @DisplayName("UNSORTED.and(sort) 应返回传入排序")
        void shouldReturnPassedSort_whenUnsortedAnd() {
            Sort sort = Sort.asc("name");
            Sort result = Sort.unsorted().and(sort);
            assertThat(result).isSameAs(sort);
        }
    }

    @Nested
    @DisplayName("Order 类")
    class OrderTests {

        @Test
        @DisplayName("Order.asc(\"name\") 应创建升序排序项")
        void shouldCreateAscOrder_whenAsc() {
            Sort.Order order = Sort.Order.asc("name");
            assertThat(order.getProperty()).isEqualTo("name");
            assertThat(order.isAscending()).isTrue();
            assertThat(order.isDescending()).isFalse();
        }

        @Test
        @DisplayName("Order.desc(\"name\") 应创建降序排序项")
        void shouldCreateDescOrder_whenDesc() {
            Sort.Order order = Sort.Order.desc("name");
            assertThat(order.getProperty()).isEqualTo("name");
            assertThat(order.isDescending()).isTrue();
            assertThat(order.isAscending()).isFalse();
        }

        @Test
        @DisplayName("ignoreCase() 应返回新 Order 且 ignoreCase=true")
        void shouldReturnNewOrderWithIgnoreCase_whenIgnoreCase() {
            Sort.Order order = Sort.Order.asc("name");
            Sort.Order ignoreCaseOrder = order.ignoreCase();

            assertThat(order.isIgnoreCase()).isFalse();
            assertThat(ignoreCaseOrder.isIgnoreCase()).isTrue();
            assertThat(ignoreCaseOrder.getProperty()).isEqualTo("name");
            assertThat(ignoreCaseOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
        }
    }

    @Nested
    @DisplayName("属性名验证")
    class PropertyValidationTests {

        @Test
        @DisplayName("空属性名应抛出 IllegalArgumentException")
        void shouldThrowException_whenEmptyProperty() {
            assertThatThrownBy(() -> Sort.asc(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Property name must not be empty");
        }

        @Test
        @DisplayName("非法字符应抛出 IllegalArgumentException")
        void shouldThrowException_whenInvalidProperty() {
            assertThatThrownBy(() -> Sort.asc("name; DROP TABLE user"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid property name");
        }

        @Test
        @DisplayName("以数字开头的属性名应抛出 IllegalArgumentException")
        void shouldThrowException_whenPropertyStartsWithDigit() {
            assertThatThrownBy(() -> Sort.asc("1name"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid property name");
        }

        @Test
        @DisplayName("合法属性名应正常工作")
        void shouldWork_whenValidProperty() {
            assertThat(Sort.asc("name")).isNotNull();
            assertThat(Sort.asc("_name")).isNotNull();
            assertThat(Sort.asc("user_name")).isNotNull();
            assertThat(Sort.asc("user.name")).isNotNull();
        }
    }

    @Nested
    @DisplayName("Direction 枚举")
    class DirectionTests {

        @Test
        @DisplayName("ASC symbol 应为 ASC")
        void shouldReturnAscSymbol() {
            assertThat(Sort.Direction.ASC.getSymbol()).isEqualTo("ASC");
        }

        @Test
        @DisplayName("DESC symbol 应为 DESC")
        void shouldReturnDescSymbol() {
            assertThat(Sort.Direction.DESC.getSymbol()).isEqualTo("DESC");
        }
    }

    @Nested
    @DisplayName("toString 方法")
    class ToStringTests {

        @Test
        @DisplayName("UNSORTED.toString() 应返回 UNSORTED")
        void shouldReturnUnsorted_whenToString() {
            assertThat(Sort.unsorted().toString()).isEqualTo("UNSORTED");
        }

        @Test
        @DisplayName("排序 toString 应返回属性和方向")
        void shouldReturnPropertyAndDirection_whenToString() {
            Sort sort = Sort.asc("name");
            assertThat(sort.toString()).isEqualTo("name ASC");
        }

        @Test
        @DisplayName("多字段排序 toString 应返回逗号分隔")
        void shouldReturnCommaSeparated_whenMultipleOrders() {
            Sort sort = Sort.asc("name", "age");
            assertThat(sort.toString()).isEqualTo("name ASC, age ASC");
        }

        @Test
        @DisplayName("ignoreCase toString 应包含 IGNORE CASE")
        void shouldIncludeIgnoreCase_whenToString() {
            Sort.Order order = Sort.Order.asc("name").ignoreCase();
            assertThat(order.toString()).isEqualTo("name ASC IGNORE CASE");
        }
    }
}
