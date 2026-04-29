package io.github.afgprojects.framework.data.core.query;

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
            this.property = Objects.requireNonNull(property, "property must not be null");
            this.direction = Objects.requireNonNull(direction, "direction must not be null");
            this.ignoreCase = ignoreCase;
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
}