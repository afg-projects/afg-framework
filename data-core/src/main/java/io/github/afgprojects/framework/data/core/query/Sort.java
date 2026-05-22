package io.github.afgprojects.framework.data.core.query;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.condition.SFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 排序定义
 */
public final class Sort {

    /**
     * 排序方向
     */
    public enum Direction {
        /**
         * 升序
         */
        ASC("ASC"),
        /**
         * 降序
         */
        DESC("DESC");

        private final String symbol;

        Direction(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    /**
     * 空排序
     */
    public static final Sort UNSORTED = new Sort(Collections.emptyList());

    private final List<Order> orders;

    /**
     * 构造排序
     *
     * @param orders 排序项列表
     */
    public Sort(@NonNull List<Order> orders) {
        this.orders = Collections.unmodifiableList(new ArrayList<>(orders));
    }

    /**
     * 获取排序项列表
     *
     * @return 排序项列表
     */
    public @NonNull List<Order> getOrders() {
        return orders;
    }

    /**
     * 是否未排序
     *
     * @return true表示未排序
     */
    public boolean isUnsorted() {
        return orders.isEmpty();
    }

    /**
     * 是否已排序
     *
     * @return true表示已排序
     */
    public boolean isSorted() {
        return !orders.isEmpty();
    }

    // ========== 静态工厂方法 ==========

    /**
     * 创建空排序
     *
     * @return 空排序
     */
    public static @NonNull Sort unsorted() {
        return UNSORTED;
    }

    /**
     * 创建升序排序
     *
     * @param properties 属性名列表
     * @return 排序对象
     */
    public static @NonNull Sort asc(@NonNull String... properties) {
        return by(Direction.ASC, properties);
    }

    /**
     * 创建降序排序
     *
     * @param properties 属性名列表
     * @return 排序对象
     */
    public static @NonNull Sort desc(@NonNull String... properties) {
        return by(Direction.DESC, properties);
    }

    /**
     * 创建排序
     *
     * @param direction  排序方向
     * @param properties 属性名列表
     * @return 排序对象
     */
    public static @NonNull Sort by(@NonNull Direction direction, @NonNull String... properties) {
        Objects.requireNonNull(direction, "direction must not be null");
        Objects.requireNonNull(properties, "properties must not be null");

        List<Order> orders = new ArrayList<>();
        for (String property : properties) {
            orders.add(new Order(property, direction));
        }
        return new Sort(orders);
    }

    /**
     * 创建排序
     *
     * @param orders 排序项
     * @return 排序对象
     */
    public static @NonNull Sort by(@NonNull Order... orders) {
        Objects.requireNonNull(orders, "orders must not be null");
        List<Order> orderList = new ArrayList<>();
        Collections.addAll(orderList, orders);
        return new Sort(orderList);
    }

    // ========== Lambda 风格工厂方法 ==========

    /**
     * 创建类型化排序构建器
     * <p>
     * 使用 Lambda 方式构建排序，类型安全，避免字段名拼写错误。
     * <p>
     * 使用示例：
     * <pre>
     * Sort sort = Sort.builder(User.class)
     *     .asc(User::getCreateTime)
     *     .desc(User::getStatus)
     *     .build();
     * </pre>
     *
     * @param entityClass 实体类
     * @param <T>         实体类型
     * @return 类型化排序构建器
     */
    public static <T> @NonNull TypedSortBuilder<T> builder(@NonNull Class<T> entityClass) {
        return new DefaultTypedSortBuilder<>(entityClass);
    }

    /**
     * 创建升序排序（Lambda 方式）
     *
     * @param getter 字段 getter 方法引用
     * @param <T>    实体类型
     * @param <R>    字段类型
     * @return 排序对象
     */
    public static <T, R> @NonNull Sort asc(@NonNull SFunction<T, R> getter) {
        return by(Order.asc(Conditions.getFieldName(getter)));
    }

    /**
     * 创建降序排序（Lambda 方式）
     *
     * @param getter 字段 getter 方法引用
     * @param <T>    实体类型
     * @param <R>    字段类型
     * @return 排序对象
     */
    public static <T, R> @NonNull Sort desc(@NonNull SFunction<T, R> getter) {
        return by(Order.desc(Conditions.getFieldName(getter)));
    }

    /**
     * 创建排序（Lambda 方式，支持多个字段）
     *
     * @param direction 排序方向
     * @param getters   字段 getter 方法引用
     * @param <T>       实体类型
     * @return 排序对象
     */
    @SuppressWarnings("unchecked")
    public static <T> @NonNull Sort by(@NonNull Direction direction, @NonNull SFunction<T, ?>... getters) {
        Objects.requireNonNull(direction, "direction must not be null");
        Objects.requireNonNull(getters, "getters must not be null");

        List<Order> orders = new ArrayList<>();
        for (SFunction<T, ?> getter : getters) {
            orders.add(new Order(Conditions.getFieldName(getter), direction));
        }
        return new Sort(orders);
    }

    /**
     * 追加排序
     *
     * @param sort 另一个排序
     * @return 组合后的排序
     */
    public @NonNull Sort and(@NonNull Sort sort) {
        if (sort.isUnsorted()) {
            return this;
        }
        if (this.isUnsorted()) {
            return sort;
        }
        List<Order> newOrders = new ArrayList<>(this.orders);
        newOrders.addAll(sort.orders);
        return new Sort(newOrders);
    }

    @Override
    public String toString() {
        if (isUnsorted()) {
            return "UNSORTED";
        }
        return String.join(", ", orders.stream().map(Order::toString).toList());
    }

    /**
     * 排序项
     */
    public static final class Order {

        /**
         * 属性名验证正则：仅允许字母、数字、下划线、点号
         */
        private static final java.util.regex.Pattern PROPERTY_PATTERN =
                java.util.regex.Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_.]*$");

        private final String property;
        private final Direction direction;
        private final boolean ignoreCase;

        /**
         * 构造排序项
         *
         * @param property  属性名
         * @param direction 排序方向
         */
        public Order(@NonNull String property, @NonNull Direction direction) {
            this(property, direction, false);
        }

        /**
         * 构造排序项
         *
         * @param property   属性名
         * @param direction  排序方向
         * @param ignoreCase 是否忽略大小写
         */
        public Order(@NonNull String property, @NonNull Direction direction, boolean ignoreCase) {
            Objects.requireNonNull(property, "property must not be null");
            Objects.requireNonNull(direction, "direction must not be null");
            validateProperty(property);
            this.property = property;
            this.direction = direction;
            this.ignoreCase = ignoreCase;
        }

        /**
         * 验证属性名是否合法
         * <p>
         * 仅允许字母、数字、下划线和点号，防止 SQL 注入
         *
         * @param property 属性名
         * @throws IllegalArgumentException 如果属性名不合法
         */
        private static void validateProperty(@NonNull String property) {
            if (property.isEmpty()) {
                throw new IllegalArgumentException("Property name must not be empty");
            }
            if (!PROPERTY_PATTERN.matcher(property).matches()) {
                throw new IllegalArgumentException(
                        "Invalid property name: '" + property + "'. " +
                        "Property names must start with a letter or underscore, " +
                        "and contain only letters, digits, underscores, and dots.");
            }
        }

        /**
         * 获取属性名
         *
         * @return 属性名
         */
        public @NonNull String getProperty() {
            return property;
        }

        /**
         * 获取排序方向
         *
         * @return 排序方向
         */
        public @NonNull Direction getDirection() {
            return direction;
        }

        /**
         * 是否升序
         *
         * @return true表示升序
         */
        public boolean isAscending() {
            return direction == Direction.ASC;
        }

        /**
         * 是否降序
         *
         * @return true表示降序
         */
        public boolean isDescending() {
            return direction == Direction.DESC;
        }

        /**
         * 是否忽略大小写
         *
         * @return true表示忽略大小写
         */
        public boolean isIgnoreCase() {
            return ignoreCase;
        }

        /**
         * 创建升序排序项
         *
         * @param property 属性名
         * @return 排序项
         */
        public static @NonNull Order asc(@NonNull String property) {
            return new Order(property, Direction.ASC);
        }

        /**
         * 创建降序排序项
         *
         * @param property 属性名
         * @return 排序项
         */
        public static @NonNull Order desc(@NonNull String property) {
            return new Order(property, Direction.DESC);
        }

        /**
         * 创建忽略大小写的排序项
         *
         * @return 新排序项
         */
        public @NonNull Order ignoreCase() {
            return new Order(property, direction, true);
        }

        @Override
        public String toString() {
            String suffix = ignoreCase ? " IGNORE CASE" : "";
            return property + " " + direction.getSymbol() + suffix;
        }
    }

    /**
     * 默认类型化排序构建器实现
     */
    private static final class DefaultTypedSortBuilder<T> implements TypedSortBuilder<T> {
        private final Class<T> entityClass;
        private final List<Order> orders = new ArrayList<>();

        DefaultTypedSortBuilder(Class<T> entityClass) {
            this.entityClass = entityClass;
        }

        @Override
        public <R> TypedSortBuilder<T> asc(SFunction<T, R> getter) {
            orders.add(new Order(Conditions.getFieldName(getter), Direction.ASC));
            return this;
        }

        @Override
        public <R> TypedSortBuilder<T> desc(SFunction<T, R> getter) {
            orders.add(new Order(Conditions.getFieldName(getter), Direction.DESC));
            return this;
        }

        @Override
        public Sort build() {
            if (orders.isEmpty()) {
                return UNSORTED;
            }
            return new Sort(orders);
        }
    }
}