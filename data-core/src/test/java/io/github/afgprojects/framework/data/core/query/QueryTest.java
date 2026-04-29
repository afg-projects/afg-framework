package io.github.afgprojects.framework.data.core.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Query 包测试
 */
@DisplayName("Query 包测试")
class QueryTest {

    // ==================== Operator 枚举测试 ====================

    @Nested
    @DisplayName("Operator 枚举测试")
    class OperatorTest {

        @Test
        @DisplayName("应包含所有操作符")
        void shouldContainAllOperators() {
            assertThat(Operator.values()).containsExactly(
                    Operator.EQ, Operator.NE, Operator.GT, Operator.GE,
                    Operator.LT, Operator.LE, Operator.LIKE, Operator.LIKE_LEFT,
                    Operator.LIKE_RIGHT, Operator.NOT_LIKE, Operator.IN, Operator.NOT_IN,
                    Operator.IS_NULL, Operator.IS_NOT_NULL, Operator.BETWEEN, Operator.NOT_BETWEEN
            );
        }

        @Test
        @DisplayName("应返回正确的符号")
        void shouldReturnCorrectSymbol() {
            assertThat(Operator.EQ.getSymbol()).isEqualTo("=");
            assertThat(Operator.NE.getSymbol()).isEqualTo("!=");
            assertThat(Operator.GT.getSymbol()).isEqualTo(">");
            assertThat(Operator.GE.getSymbol()).isEqualTo(">=");
            assertThat(Operator.LT.getSymbol()).isEqualTo("<");
            assertThat(Operator.LE.getSymbol()).isEqualTo("<=");
            assertThat(Operator.LIKE.getSymbol()).isEqualTo("LIKE");
            assertThat(Operator.LIKE_LEFT.getSymbol()).isEqualTo("LIKE");
            assertThat(Operator.LIKE_RIGHT.getSymbol()).isEqualTo("LIKE");
            assertThat(Operator.NOT_LIKE.getSymbol()).isEqualTo("NOT LIKE");
            assertThat(Operator.IN.getSymbol()).isEqualTo("IN");
            assertThat(Operator.NOT_IN.getSymbol()).isEqualTo("NOT IN");
            assertThat(Operator.IS_NULL.getSymbol()).isEqualTo("IS NULL");
            assertThat(Operator.IS_NOT_NULL.getSymbol()).isEqualTo("IS NOT NULL");
            assertThat(Operator.BETWEEN.getSymbol()).isEqualTo("BETWEEN");
            assertThat(Operator.NOT_BETWEEN.getSymbol()).isEqualTo("NOT BETWEEN");
        }

        @Test
        @DisplayName("requiresValue 应正确判断是否需要值")
        void shouldCheckRequiresValueCorrectly() {
            // 一元操作符不需要值
            assertThat(Operator.IS_NULL.requiresValue()).isFalse();
            assertThat(Operator.IS_NOT_NULL.requiresValue()).isFalse();

            // 其他操作符需要值
            assertThat(Operator.EQ.requiresValue()).isTrue();
            assertThat(Operator.NE.requiresValue()).isTrue();
            assertThat(Operator.GT.requiresValue()).isTrue();
            assertThat(Operator.GE.requiresValue()).isTrue();
            assertThat(Operator.LT.requiresValue()).isTrue();
            assertThat(Operator.LE.requiresValue()).isTrue();
            assertThat(Operator.LIKE.requiresValue()).isTrue();
            assertThat(Operator.LIKE_LEFT.requiresValue()).isTrue();
            assertThat(Operator.LIKE_RIGHT.requiresValue()).isTrue();
            assertThat(Operator.NOT_LIKE.requiresValue()).isTrue();
            assertThat(Operator.IN.requiresValue()).isTrue();
            assertThat(Operator.NOT_IN.requiresValue()).isTrue();
            assertThat(Operator.BETWEEN.requiresValue()).isTrue();
            assertThat(Operator.NOT_BETWEEN.requiresValue()).isTrue();
        }

        @Test
        @DisplayName("应能通过名称获取枚举值")
        void shouldGetValueByName() {
            assertThat(Operator.valueOf("EQ")).isEqualTo(Operator.EQ);
            assertThat(Operator.valueOf("NE")).isEqualTo(Operator.NE);
            assertThat(Operator.valueOf("GT")).isEqualTo(Operator.GT);
            assertThat(Operator.valueOf("IS_NULL")).isEqualTo(Operator.IS_NULL);
            assertThat(Operator.valueOf("BETWEEN")).isEqualTo(Operator.BETWEEN);
        }

        @Test
        @DisplayName("LIKE 相关操作符应返回相同符号")
        void shouldReturnSameSymbolForLikeOperators() {
            assertThat(Operator.LIKE.getSymbol()).isEqualTo(Operator.LIKE_LEFT.getSymbol());
            assertThat(Operator.LIKE.getSymbol()).isEqualTo(Operator.LIKE_RIGHT.getSymbol());
        }
    }

    // ==================== LogicalOperator 枚举测试 ====================

    @Nested
    @DisplayName("LogicalOperator 枚举测试")
    class LogicalOperatorTest {

        @Test
        @DisplayName("应包含所有逻辑操作符")
        void shouldContainAllLogicalOperators() {
            assertThat(LogicalOperator.values()).containsExactly(
                    LogicalOperator.AND, LogicalOperator.OR
            );
        }

        @Test
        @DisplayName("应返回正确的符号")
        void shouldReturnCorrectSymbol() {
            assertThat(LogicalOperator.AND.getSymbol()).isEqualTo("AND");
            assertThat(LogicalOperator.OR.getSymbol()).isEqualTo("OR");
        }

        @Test
        @DisplayName("应能通过名称获取枚举值")
        void shouldGetValueByName() {
            assertThat(LogicalOperator.valueOf("AND")).isEqualTo(LogicalOperator.AND);
            assertThat(LogicalOperator.valueOf("OR")).isEqualTo(LogicalOperator.OR);
        }
    }

    // ==================== Criterion 测试 ====================

    @Nested
    @DisplayName("Criterion 测试")
    class CriterionTest {

        @Test
        @DisplayName("应正确创建条件项 - 完整构造")
        void shouldCreateCriterionWithFullConstructor() {
            Criterion criterion = new Criterion("name", Operator.EQ, "test", LogicalOperator.AND);

            assertThat(criterion.field()).isEqualTo("name");
            assertThat(criterion.operator()).isEqualTo(Operator.EQ);
            assertThat(criterion.value()).isEqualTo("test");
            assertThat(criterion.nextOperator()).isEqualTo(LogicalOperator.AND);
        }

        @Test
        @DisplayName("应正确创建条件项 - 兼容构造")
        void shouldCreateCriterionWithCompatibleConstructor() {
            Criterion criterion = new Criterion("age", Operator.GT, 18);

            assertThat(criterion.field()).isEqualTo("age");
            assertThat(criterion.operator()).isEqualTo(Operator.GT);
            assertThat(criterion.value()).isEqualTo(18);
            assertThat(criterion.nextOperator()).isNull();
        }

        @Test
        @DisplayName("of 静态方法应正确创建条件项")
        void shouldCreateCriterionWithOfMethod() {
            Criterion criterion = Criterion.of("status", Operator.EQ, "active");

            assertThat(criterion.field()).isEqualTo("status");
            assertThat(criterion.operator()).isEqualTo(Operator.EQ);
            assertThat(criterion.value()).isEqualTo("active");
            assertThat(criterion.nextOperator()).isNull();
        }

        @Test
        @DisplayName("应支持 null 值")
        void shouldSupportNullValue() {
            Criterion criterion = Criterion.of("deleted_at", Operator.IS_NULL, null);

            assertThat(criterion.value()).isNull();
        }

        @Test
        @DisplayName("isUnary 应正确判断一元操作符")
        void shouldCheckUnaryOperator() {
            assertThat(Criterion.of("col", Operator.IS_NULL, null).isUnary()).isTrue();
            assertThat(Criterion.of("col", Operator.IS_NOT_NULL, null).isUnary()).isTrue();
            assertThat(Criterion.of("col", Operator.EQ, "value").isUnary()).isFalse();
            assertThat(Criterion.of("col", Operator.IN, List.of(1, 2, 3)).isUnary()).isFalse();
        }

        @Test
        @DisplayName("isRange 应正确判断范围操作符")
        void shouldCheckRangeOperator() {
            assertThat(Criterion.of("age", Operator.BETWEEN, new Object[]{1, 100}).isRange()).isTrue();
            assertThat(Criterion.of("age", Operator.NOT_BETWEEN, new Object[]{1, 100}).isRange()).isTrue();
            assertThat(Criterion.of("age", Operator.EQ, 18).isRange()).isFalse();
            assertThat(Criterion.of("age", Operator.GT, 18).isRange()).isFalse();
        }

        @Test
        @DisplayName("isCollection 应正确判断集合操作符")
        void shouldCheckCollectionOperator() {
            assertThat(Criterion.of("id", Operator.IN, List.of(1, 2, 3)).isCollection()).isTrue();
            assertThat(Criterion.of("id", Operator.NOT_IN, List.of(1, 2, 3)).isCollection()).isTrue();
            assertThat(Criterion.of("id", Operator.EQ, 1).isCollection()).isFalse();
            assertThat(Criterion.of("id", Operator.BETWEEN, new Object[]{1, 100}).isCollection()).isFalse();
        }

        @Test
        @DisplayName("应支持各种类型的值")
        void shouldSupportVariousValueTypes() {
            // 字符串
            Criterion stringCriterion = Criterion.of("name", Operator.LIKE, "%test%");
            assertThat(stringCriterion.value()).isEqualTo("%test%");

            // 数字
            Criterion intCriterion = Criterion.of("age", Operator.GE, 18);
            assertThat(intCriterion.value()).isEqualTo(18);

            // 布尔
            Criterion boolCriterion = Criterion.of("active", Operator.EQ, true);
            assertThat(boolCriterion.value()).isEqualTo(true);

            // 集合
            Criterion listCriterion = Criterion.of("status", Operator.IN, Arrays.asList("active", "pending"));
            assertThat(listCriterion.value()).isInstanceOf(List.class);

            // 数组
            Criterion arrayCriterion = Criterion.of("range", Operator.BETWEEN, new Object[]{1, 100});
            assertThat(arrayCriterion.value()).isInstanceOf(Object[].class);
        }

        @Test
        @DisplayName("record 应正确实现 equals 和 hashCode")
        void shouldImplementEqualsAndHashCodeCorrectly() {
            Criterion c1 = Criterion.of("name", Operator.EQ, "test");
            Criterion c2 = Criterion.of("name", Operator.EQ, "test");
            Criterion c3 = Criterion.of("name", Operator.EQ, "other");

            assertThat(c1).isEqualTo(c2);
            assertThat(c1).isNotEqualTo(c3);
            assertThat(c1.hashCode()).isEqualTo(c2.hashCode());
        }

        @Test
        @DisplayName("record 应正确实现 toString")
        void shouldImplementToStringCorrectly() {
            Criterion criterion = Criterion.of("name", Operator.EQ, "test");

            assertThat(criterion.toString()).contains("name");
            assertThat(criterion.toString()).contains("EQ");
            assertThat(criterion.toString()).contains("test");
        }
    }

    // ==================== Condition 接口测试 ====================

    @Nested
    @DisplayName("Condition 接口测试")
    class ConditionInterfaceTest {

        @Test
        @DisplayName("empty 静态方法应返回空条件")
        void shouldReturnEmptyCondition() {
            Condition empty = Condition.empty();

            assertThat(empty.isEmpty()).isTrue();
            assertThat(empty.getCriteria()).isEmpty();
            assertThat(empty.getOperator()).isEqualTo(LogicalOperator.AND);
        }
    }

    // ==================== ConditionImpl 测试 ====================

    @Nested
    @DisplayName("ConditionImpl 测试")
    class ConditionImplTest {

        @Test
        @DisplayName("应正确创建条件")
        void shouldCreateCondition() {
            Criterion c1 = Criterion.of("name", Operator.EQ, "test");
            Criterion c2 = Criterion.of("status", Operator.EQ, "active");

            Condition condition = new ConditionImpl(LogicalOperator.AND, List.of(c1, c2));

            assertThat(condition.isEmpty()).isFalse();
            assertThat(condition.getCriteria()).hasSize(2);
            assertThat(condition.getOperator()).isEqualTo(LogicalOperator.AND);
        }

        @Test
        @DisplayName("应正确创建 OR 条件")
        void shouldCreateOrCondition() {
            Criterion c1 = Criterion.of("role", Operator.EQ, "admin");
            Criterion c2 = Criterion.of("role", Operator.EQ, "user");

            Condition condition = new ConditionImpl(LogicalOperator.OR, List.of(c1, c2));

            assertThat(condition.getOperator()).isEqualTo(LogicalOperator.OR);
        }

        @Test
        @DisplayName("空条件列表应返回 isEmpty=true")
        void shouldReturnTrueForEmptyCriteria() {
            Condition condition = new ConditionImpl(LogicalOperator.AND, Collections.emptyList());

            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("add 应返回新条件对象（不可变）")
        void shouldReturnNewConditionWhenAdding() {
            Criterion c1 = Criterion.of("name", Operator.EQ, "test");
            Condition original = new ConditionImpl(LogicalOperator.AND, List.of(c1));

            Criterion c2 = Criterion.of("status", Operator.EQ, "active");
            Condition updated = original.add(c2);

            // 原对象不变
            assertThat(original.getCriteria()).hasSize(1);
            // 新对象包含两个条件
            assertThat(updated.getCriteria()).hasSize(2);
            assertThat(updated.getCriteria()).containsExactly(c1, c2);
        }

        @Test
        @DisplayName("and 应正确合并条件")
        void shouldCombineWithAnd() {
            Criterion c1 = Criterion.of("name", Operator.EQ, "test");
            Criterion c2 = Criterion.of("status", Operator.EQ, "active");

            Condition cond1 = new ConditionImpl(LogicalOperator.AND, List.of(c1));
            Condition cond2 = new ConditionImpl(LogicalOperator.AND, List.of(c2));

            Condition combined = cond1.and(cond2);

            assertThat(combined.getOperator()).isEqualTo(LogicalOperator.AND);
            assertThat(combined.getCriteria()).hasSize(2);
            assertThat(combined.getCriteria()).containsExactly(c1, c2);
        }

        @Test
        @DisplayName("or 应正确合并条件")
        void shouldCombineWithOr() {
            Criterion c1 = Criterion.of("role", Operator.EQ, "admin");
            Criterion c2 = Criterion.of("role", Operator.EQ, "user");

            Condition cond1 = new ConditionImpl(LogicalOperator.OR, List.of(c1));
            Condition cond2 = new ConditionImpl(LogicalOperator.OR, List.of(c2));

            Condition combined = cond1.or(cond2);

            assertThat(combined.getOperator()).isEqualTo(LogicalOperator.OR);
            assertThat(combined.getCriteria()).hasSize(2);
        }

        @Test
        @DisplayName("and 合并时空条件应返回另一条件")
        void shouldReturnOtherWhenAndWithEmpty() {
            Criterion c1 = Criterion.of("name", Operator.EQ, "test");
            Condition cond1 = new ConditionImpl(LogicalOperator.AND, List.of(c1));
            Condition empty = Condition.empty();

            // 空 + 非空 = 非空
            Condition result1 = empty.and(cond1);
            assertThat(result1).isEqualTo(cond1);

            // 非空 + 空 = 非空
            Condition result2 = cond1.and(empty);
            assertThat(result2).isEqualTo(cond1);
        }

        @Test
        @DisplayName("or 合并时空条件应返回另一条件")
        void shouldReturnOtherWhenOrWithEmpty() {
            Criterion c1 = Criterion.of("name", Operator.EQ, "test");
            Condition cond1 = new ConditionImpl(LogicalOperator.OR, List.of(c1));
            Condition empty = Condition.empty();

            // 空 + 非空 = 非空
            Condition result1 = empty.or(cond1);
            assertThat(result1).isEqualTo(cond1);

            // 非空 + 空 = 非空
            Condition result2 = cond1.or(empty);
            assertThat(result2).isEqualTo(cond1);
        }

        @Test
        @DisplayName("空条件 and 空条件应返回空条件")
        void shouldReturnEmptyWhenAndTwoEmpty() {
            Condition result = Condition.empty().and(Condition.empty());
            assertThat(result.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("空条件 or 空条件应返回空条件")
        void shouldReturnEmptyWhenOrTwoEmpty() {
            Condition result = Condition.empty().or(Condition.empty());
            assertThat(result.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("getCriteria 应返回不可变列表")
        void shouldReturnUnmodifiableList() {
            Criterion c1 = Criterion.of("name", Operator.EQ, "test");
            Condition condition = new ConditionImpl(LogicalOperator.AND, List.of(c1));

            List<Criterion> criteria = condition.getCriteria();

            assertThatThrownBy(() -> criteria.add(Criterion.of("other", Operator.EQ, "value")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("toString 应包含操作符和条件")
        void shouldIncludeOperatorAndCriteriaInToString() {
            Criterion c1 = Criterion.of("name", Operator.EQ, "test");
            Condition condition = new ConditionImpl(LogicalOperator.AND, List.of(c1));

            String str = condition.toString();

            assertThat(str).contains("AND");
            assertThat(str).contains("name");
            assertThat(str).contains("EQ");
        }

        @Test
        @DisplayName("EMPTY 常量应为空条件")
        void shouldHaveEmptyConstant() {
            assertThat(ConditionImpl.EMPTY.isEmpty()).isTrue();
            assertThat(ConditionImpl.EMPTY.getOperator()).isEqualTo(LogicalOperator.AND);
        }
    }

    // ==================== Sort 测试 ====================

    @Nested
    @DisplayName("Sort 测试")
    class SortTest {

        @Test
        @DisplayName("应正确创建排序")
        void shouldCreateSort() {
            Sort.Order order = new Sort.Order("name", Sort.Direction.ASC);
            Sort sort = new Sort(List.of(order));

            assertThat(sort.isSorted()).isTrue();
            assertThat(sort.isUnsorted()).isFalse();
            assertThat(sort.getOrders()).hasSize(1);
        }

        @Test
        @DisplayName("unsorted 应返回空排序")
        void shouldReturnUnsortedSort() {
            Sort sort = Sort.unsorted();

            assertThat(sort.isUnsorted()).isTrue();
            assertThat(sort.isSorted()).isFalse();
            assertThat(sort.getOrders()).isEmpty();
        }

        @Test
        @DisplayName("UNSORTED 常量应为空排序")
        void shouldHaveUnsortedConstant() {
            assertThat(Sort.UNSORTED.isUnsorted()).isTrue();
            assertThat(Sort.UNSORTED.isSorted()).isFalse();
        }

        @Test
        @DisplayName("asc 应创建升序排序")
        void shouldCreateAscendingSort() {
            Sort sort = Sort.asc("name", "created_at");

            assertThat(sort.isSorted()).isTrue();
            assertThat(sort.getOrders()).hasSize(2);
            assertThat(sort.getOrders().get(0).getProperty()).isEqualTo("name");
            assertThat(sort.getOrders().get(0).isAscending()).isTrue();
            assertThat(sort.getOrders().get(1).getProperty()).isEqualTo("created_at");
            assertThat(sort.getOrders().get(1).isAscending()).isTrue();
        }

        @Test
        @DisplayName("desc 应创建降序排序")
        void shouldCreateDescendingSort() {
            Sort sort = Sort.desc("priority", "updated_at");

            assertThat(sort.isSorted()).isTrue();
            assertThat(sort.getOrders()).hasSize(2);
            assertThat(sort.getOrders().get(0).isDescending()).isTrue();
            assertThat(sort.getOrders().get(1).isDescending()).isTrue();
        }

        @Test
        @DisplayName("by(Direction, String...) 应创建指定方向排序")
        void shouldCreateSortWithDirection() {
            Sort sort = Sort.by(Sort.Direction.DESC, "score", "time");

            assertThat(sort.getOrders()).hasSize(2);
            assertThat(sort.getOrders().get(0).getDirection()).isEqualTo(Sort.Direction.DESC);
            assertThat(sort.getOrders().get(1).getDirection()).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("by(Order...) 应创建排序")
        void shouldCreateSortWithOrders() {
            Sort.Order o1 = Sort.Order.asc("name");
            Sort.Order o2 = Sort.Order.desc("created_at");

            Sort sort = Sort.by(o1, o2);

            assertThat(sort.getOrders()).hasSize(2);
            assertThat(sort.getOrders().get(0).isAscending()).isTrue();
            assertThat(sort.getOrders().get(1).isDescending()).isTrue();
        }

        @Test
        @DisplayName("and 应正确合并排序")
        void shouldCombineSorts() {
            Sort sort1 = Sort.asc("name");
            Sort sort2 = Sort.desc("created_at");

            Sort combined = sort1.and(sort2);

            assertThat(combined.getOrders()).hasSize(2);
            assertThat(combined.getOrders().get(0).getProperty()).isEqualTo("name");
            assertThat(combined.getOrders().get(1).getProperty()).isEqualTo("created_at");
        }

        @Test
        @DisplayName("and 合并时空排序应返回原排序")
        void shouldReturnOriginalWhenAndWithUnsorted() {
            Sort sorted = Sort.asc("name");
            Sort unsorted = Sort.unsorted();

            // 已排序 + 空排序 = 已排序
            assertThat(sorted.and(unsorted)).isEqualTo(sorted);
            // 空排序 + 已排序 = 已排序
            assertThat(unsorted.and(sorted)).isEqualTo(sorted);
        }

        @Test
        @DisplayName("两个空排序合并应返回空排序")
        void shouldReturnUnsortedWhenCombineTwoUnsorted() {
            Sort result = Sort.unsorted().and(Sort.unsorted());
            assertThat(result.isUnsorted()).isTrue();
        }

        @Test
        @DisplayName("getOrders 应返回不可变列表")
        void shouldReturnUnmodifiableOrders() {
            Sort sort = Sort.asc("name");

            assertThatThrownBy(() -> sort.getOrders().add(Sort.Order.asc("other")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("toString 应正确格式化")
        void shouldFormatToString() {
            Sort sort = Sort.asc("name", "age");

            String str = sort.toString();

            assertThat(str).contains("name ASC");
            assertThat(str).contains("age ASC");
        }

        @Test
        @DisplayName("空排序 toString 应返回 UNSORTED")
        void shouldReturnUnsortedInToString() {
            assertThat(Sort.unsorted().toString()).isEqualTo("UNSORTED");
        }

        // ==================== Sort.Order 测试 ====================

        @Nested
        @DisplayName("Sort.Order 测试")
        class OrderTest {

            @Test
            @DisplayName("应正确创建排序项")
            void shouldCreateOrder() {
                Sort.Order order = new Sort.Order("name", Sort.Direction.ASC);

                assertThat(order.getProperty()).isEqualTo("name");
                assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
                assertThat(order.isAscending()).isTrue();
                assertThat(order.isDescending()).isFalse();
                assertThat(order.isIgnoreCase()).isFalse();
            }

            @Test
            @DisplayName("应支持忽略大小写")
            void shouldSupportIgnoreCase() {
                Sort.Order order = new Sort.Order("name", Sort.Direction.ASC, true);

                assertThat(order.isIgnoreCase()).isTrue();
            }

            @Test
            @DisplayName("asc 静态方法应创建升序排序项")
            void shouldCreateAscOrder() {
                Sort.Order order = Sort.Order.asc("name");

                assertThat(order.getProperty()).isEqualTo("name");
                assertThat(order.isAscending()).isTrue();
            }

            @Test
            @DisplayName("desc 静态方法应创建降序排序项")
            void shouldCreateDescOrder() {
                Sort.Order order = Sort.Order.desc("created_at");

                assertThat(order.getProperty()).isEqualTo("created_at");
                assertThat(order.isDescending()).isTrue();
            }

            @Test
            @DisplayName("ignoreCase 应创建忽略大小写的排序项")
            void shouldCreateIgnoreCaseOrder() {
                Sort.Order original = Sort.Order.asc("name");
                Sort.Order ignoreCase = original.ignoreCase();

                assertThat(original.isIgnoreCase()).isFalse();
                assertThat(ignoreCase.isIgnoreCase()).isTrue();
                assertThat(ignoreCase.getProperty()).isEqualTo("name");
                assertThat(ignoreCase.getDirection()).isEqualTo(Sort.Direction.ASC);
            }

            @Test
            @DisplayName("toString 应正确格式化")
            void shouldFormatToString() {
                Sort.Order order1 = Sort.Order.asc("name");
                assertThat(order1.toString()).isEqualTo("name ASC");

                Sort.Order order2 = new Sort.Order("name", Sort.Direction.ASC, true);
                assertThat(order2.toString()).isEqualTo("name ASC IGNORE CASE");
            }
        }

        // ==================== Sort.Direction 测试 ====================

        @Nested
        @DisplayName("Sort.Direction 测试")
        class DirectionTest {

            @Test
            @DisplayName("应包含所有方向")
            void shouldContainAllDirections() {
                assertThat(Sort.Direction.values()).containsExactly(
                        Sort.Direction.ASC, Sort.Direction.DESC
                );
            }

            @Test
            @DisplayName("应返回正确的符号")
            void shouldReturnCorrectSymbol() {
                assertThat(Sort.Direction.ASC.getSymbol()).isEqualTo("ASC");
                assertThat(Sort.Direction.DESC.getSymbol()).isEqualTo("DESC");
            }

            @Test
            @DisplayName("应能通过名称获取枚举值")
            void shouldGetValueByName() {
                assertThat(Sort.Direction.valueOf("ASC")).isEqualTo(Sort.Direction.ASC);
                assertThat(Sort.Direction.valueOf("DESC")).isEqualTo(Sort.Direction.DESC);
            }
        }
    }

    // ==================== Page 测试 ====================

    @Nested
    @DisplayName("Page 测试")
    class PageTest {

        @Test
        @DisplayName("应正确创建分页")
        void shouldCreatePage() {
            List<String> content = List.of("a", "b", "c");
            Page<String> page = new Page<>(content, 100, 2, 10);

            assertThat(page.getContent()).isEqualTo(content);
            assertThat(page.getTotal()).isEqualTo(100);
            assertThat(page.getPage()).isEqualTo(2);
            assertThat(page.getSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("应正确计算总页数")
        void shouldCalculateTotalPages() {
            // 正好整除
            Page<String> page1 = new Page<>(List.of("a"), 100, 1, 10);
            assertThat(page1.getTotalPages()).isEqualTo(10);

            // 有余数
            Page<String> page2 = new Page<>(List.of("a"), 95, 1, 10);
            assertThat(page2.getTotalPages()).isEqualTo(10);

            // 0 条记录（total=0, size=10 -> (0 + 10 - 1) / 10 = 0）
            Page<String> page3 = new Page<>(Collections.emptyList(), 0, 1, 10);
            assertThat(page3.getTotalPages()).isEqualTo(0);

            // 1 条记录
            Page<String> page4 = new Page<>(List.of("a"), 1, 1, 10);
            assertThat(page4.getTotalPages()).isEqualTo(1);
        }

        @Test
        @DisplayName("应正确计算偏移量")
        void shouldCalculateOffset() {
            Page<String> page1 = new Page<>(List.of("a"), 100, 1, 10);
            assertThat(page1.getOffset()).isEqualTo(0);

            Page<String> page2 = new Page<>(List.of("a"), 100, 2, 10);
            assertThat(page2.getOffset()).isEqualTo(10);

            Page<String> page3 = new Page<>(List.of("a"), 100, 5, 20);
            assertThat(page3.getOffset()).isEqualTo(80);
        }

        @Test
        @DisplayName("应正确判断页面位置")
        void shouldCheckPagePosition() {
            // 第一页
            Page<String> firstPage = new Page<>(List.of("a"), 100, 1, 10);
            assertThat(firstPage.isFirst()).isTrue();
            assertThat(firstPage.isLast()).isFalse();
            assertThat(firstPage.hasPrevious()).isFalse();
            assertThat(firstPage.hasNext()).isTrue();

            // 中间页
            Page<String> middlePage = new Page<>(List.of("a"), 100, 5, 10);
            assertThat(middlePage.isFirst()).isFalse();
            assertThat(middlePage.isLast()).isFalse();
            assertThat(middlePage.hasPrevious()).isTrue();
            assertThat(middlePage.hasNext()).isTrue();

            // 最后一页
            Page<String> lastPage = new Page<>(List.of("a"), 100, 10, 10);
            assertThat(lastPage.isFirst()).isFalse();
            assertThat(lastPage.isLast()).isTrue();
            assertThat(lastPage.hasPrevious()).isTrue();
            assertThat(lastPage.hasNext()).isFalse();

            // 单页
            Page<String> singlePage = new Page<>(List.of("a"), 5, 1, 10);
            assertThat(singlePage.isFirst()).isTrue();
            assertThat(singlePage.isLast()).isTrue();
            assertThat(singlePage.hasPrevious()).isFalse();
            assertThat(singlePage.hasNext()).isFalse();
        }

        @Test
        @DisplayName("应正确判断内容")
        void shouldCheckContent() {
            // 有内容
            Page<String> withContent = new Page<>(List.of("a", "b"), 2, 1, 10);
            assertThat(withContent.hasContent()).isTrue();
            assertThat(withContent.getNumberOfElements()).isEqualTo(2);

            // 无内容
            Page<String> emptyContent = new Page<>(Collections.emptyList(), 0, 1, 10);
            assertThat(emptyContent.hasContent()).isFalse();
            assertThat(emptyContent.getNumberOfElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("应正确处理负数参数")
        void shouldHandleNegativeParameters() {
            // 负 total 应变为 0
            Page<String> page1 = new Page<>(List.of("a"), -10, 1, 10);
            assertThat(page1.getTotal()).isEqualTo(0);

            // 负 page 应变为 1
            Page<String> page2 = new Page<>(List.of("a"), 10, -5, 10);
            assertThat(page2.getPage()).isEqualTo(1);

            // 负 size 应变为 1
            Page<String> page3 = new Page<>(List.of("a"), 10, 1, -10);
            assertThat(page3.getSize()).isEqualTo(1);
        }

        @Test
        @DisplayName("应正确处理 0 参数")
        void shouldHandleZeroParameters() {
            // 0 total
            Page<String> page1 = new Page<>(Collections.emptyList(), 0, 1, 10);
            assertThat(page1.getTotal()).isEqualTo(0);

            // 0 page 应变为 1
            Page<String> page2 = new Page<>(List.of("a"), 10, 0, 10);
            assertThat(page2.getPage()).isEqualTo(1);

            // 0 size 应变为 1
            Page<String> page3 = new Page<>(List.of("a"), 10, 1, 0);
            assertThat(page3.getSize()).isEqualTo(1);
        }

        @Test
        @DisplayName("getContent 应返回不可变列表")
        void shouldReturnUnmodifiableContent() {
            Page<String> page = new Page<>(List.of("a", "b"), 10, 1, 10);

            assertThatThrownBy(() -> page.getContent().add("c"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("map 应正确转换数据类型")
        void shouldMapContentType() {
            List<String> content = List.of("1", "2", "3");
            Page<String> stringPage = new Page<>(content, 100, 2, 10);

            Page<Integer> intPage = stringPage.map(Integer::parseInt);

            assertThat(intPage.getContent()).containsExactly(1, 2, 3);
            assertThat(intPage.getTotal()).isEqualTo(100);
            assertThat(intPage.getPage()).isEqualTo(2);
            assertThat(intPage.getSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("map 应保留分页信息")
        void shouldPreservePageInfoWhenMapping() {
            Page<String> original = new Page<>(List.of("a", "b"), 50, 3, 20);

            Page<Integer> mapped = original.map(String::length);

            assertThat(mapped.getTotal()).isEqualTo(original.getTotal());
            assertThat(mapped.getPage()).isEqualTo(original.getPage());
            assertThat(mapped.getSize()).isEqualTo(original.getSize());
        }

        @Test
        @DisplayName("empty() 应创建空分页")
        void shouldCreateEmptyPage() {
            Page<String> empty = Page.empty();

            assertThat(empty.hasContent()).isFalse();
            assertThat(empty.getTotal()).isEqualTo(0);
            assertThat(empty.getPage()).isEqualTo(1);
            assertThat(empty.getSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("empty(page, size) 应创建指定分页参数的空分页")
        void shouldCreateEmptyPageWithParameters() {
            Page<String> empty = Page.empty(2, 20);

            assertThat(empty.hasContent()).isFalse();
            assertThat(empty.getTotal()).isEqualTo(0);
            assertThat(empty.getPage()).isEqualTo(2);
            assertThat(empty.getSize()).isEqualTo(20);
        }

        @Test
        @DisplayName("of 应创建分页")
        void shouldCreatePageWithOf() {
            List<Integer> content = List.of(1, 2, 3);
            Page<Integer> page = Page.of(content, 100, 2, 10);

            assertThat(page.getContent()).isEqualTo(content);
            assertThat(page.getTotal()).isEqualTo(100);
            assertThat(page.getPage()).isEqualTo(2);
            assertThat(page.getSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("singlePage 应创建单页分页")
        void shouldCreateSinglePage() {
            List<Integer> content = List.of(1, 2, 3, 4, 5);
            Page<Integer> page = Page.singlePage(content);

            assertThat(page.getContent()).isEqualTo(content);
            assertThat(page.getTotal()).isEqualTo(5);
            assertThat(page.getPage()).isEqualTo(1);
            assertThat(page.getSize()).isEqualTo(5);
            assertThat(page.isFirst()).isTrue();
            assertThat(page.isLast()).isTrue();
        }

        @Test
        @DisplayName("singlePage 空列表应正确处理")
        void shouldHandleEmptySinglePage() {
            Page<String> page = Page.singlePage(Collections.emptyList());

            assertThat(page.hasContent()).isFalse();
            assertThat(page.getTotal()).isEqualTo(0);
            // singlePage([]) 传入 size=0，但构造函数会强制 size=Math.max(1, 0)=1
            assertThat(page.getSize()).isEqualTo(1);
            // total=0, size=1 -> (0 + 1 - 1) / 1 = 0
            assertThat(page.getTotalPages()).isEqualTo(0);
        }

        @Test
        @DisplayName("toString 应包含所有信息")
        void shouldIncludeAllInfoInToString() {
            Page<String> page = new Page<>(List.of("a", "b"), 100, 2, 10);

            String str = page.toString();

            assertThat(str).contains("content=[a, b]");
            assertThat(str).contains("total=100");
            assertThat(str).contains("page=2");
            assertThat(str).contains("size=10");
        }

        @Test
        @DisplayName("构造函数应拒绝 null content")
        void shouldRejectNullContent() {
            assertThatThrownBy(() -> new Page<>(null, 100, 1, 10))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("map 应拒绝 null mapper")
        void shouldRejectNullMapper() {
            Page<String> page = Page.of(List.of("a"), 1, 1, 10);

            assertThatThrownBy(() -> page.map(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("应正确处理大数据量分页")
        void shouldHandleLargeDataPagination() {
            long largeTotal = 1_000_000_000L;
            Page<String> page = new Page<>(List.of("item"), largeTotal, 100_000, 100);

            assertThat(page.getTotal()).isEqualTo(largeTotal);
            assertThat(page.getOffset()).isEqualTo(9_999_900L);
            assertThat(page.getTotalPages()).isEqualTo(10_000_000);
            assertThat(page.hasNext()).isTrue();
        }

        @Test
        @DisplayName("应正确处理边界页码")
        void shouldHandleBoundaryPageNumbers() {
            // 最大页码
            Page<String> lastPage = new Page<>(List.of("a"), 100, 10, 10);
            assertThat(lastPage.isLast()).isTrue();
            assertThat(lastPage.hasNext()).isFalse();

            // 超出范围的页码（逻辑上不应该出现，但需要正确处理）
            Page<String> overPage = new Page<>(List.of("a"), 100, 20, 10);
            assertThat(overPage.isLast()).isTrue(); // 页码 >= 总页数
            assertThat(overPage.hasNext()).isFalse();
        }
    }
}
