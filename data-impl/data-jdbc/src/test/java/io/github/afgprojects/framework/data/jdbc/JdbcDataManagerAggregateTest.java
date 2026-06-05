package io.github.afgprojects.framework.data.jdbc;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.query.AggregateResult;
import io.github.afgprojects.framework.data.jdbc.entity.TestOrder;
import io.github.afgprojects.framework.data.jdbc.entity.TestOrderItem;
import io.github.afgprojects.framework.data.jdbc.entity.TestUser;
import io.github.afgprojects.framework.data.jdbc.test.BaseDataTest;

/**
 * JdbcDataManager 聚合查询集成测试
 * <p>
 * 使用 PostgreSQL Testcontainers + Liquibase 迁移。
 * 测试后自动回滚（@Transactional）。
 * </p>
 */
class JdbcDataManagerAggregateTest extends BaseDataTest {

    @Nested
    @DisplayName("COUNT 聚合")
    class CountAggregate {

        @Test
        @DisplayName("should count all records when aggregate with count")
        void shouldCountAllRecords_whenAggregateWithCount() {
            // 准备：创建多个用户
            for (int i = 0; i < 5; i++) {
                dataManager.save(TestUser.class, TestUser.create("user-" + i, "user" + i + "@test.com"));
            }

            // 执行：COUNT 聚合
            AggregateResult result = dataManager.entity(TestUser.class)
                .query()
                .aggregate()
                .count("id", "totalCount")
                .single();

            // 验证
            assertThat(result.getLong("totalCount")).isGreaterThanOrEqualTo(5L);
        }

        @Test
        @DisplayName("should count with group by when aggregate with groupBy")
        void shouldCountWithGroupBy_whenAggregateWithGroupBy() {
            // 准备：创建不同状态的用户
            dataManager.save(TestUser.class, TestUser.create("active-1", "a1@test.com", 1));
            dataManager.save(TestUser.class, TestUser.create("active-2", "a2@test.com", 1));
            dataManager.save(TestUser.class, TestUser.create("inactive-1", "i1@test.com", 0));

            // 执行：按状态分组计数
            List<AggregateResult> results = dataManager.entity(TestUser.class)
                .query()
                .aggregate()
                .groupBy("status")
                .count("id", "userCount")
                .list();

            // 验证
            assertThat(results).hasSize(2);
            AggregateResult activeResult = results.stream()
                .filter(r -> Integer.valueOf(1).equals(r.getInteger("status")))
                .findFirst()
                .orElseThrow();
            assertThat(activeResult.getLong("userCount")).isGreaterThanOrEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("SUM 聚合")
    class SumAggregate {

        @Test
        @DisplayName("should sum numeric field when aggregate with sum")
        void shouldSumNumericField_whenAggregateWithSum() {
            // 准备：创建订单项
            TestOrder order = dataManager.save(TestOrder.class, TestOrder.create("ORD-SUM", null));
            dataManager.save(TestOrderItem.class,
                TestOrderItem.create(order.getId(), "P1", new BigDecimal("100.00"), 2));
            dataManager.save(TestOrderItem.class,
                TestOrderItem.create(order.getId(), "P2", new BigDecimal("200.00"), 3));

            // 执行：计算总金额（price * quantity 的总和需要原始 SQL，这里只测试 SUM）
            AggregateResult result = dataManager.entity(TestOrderItem.class)
                .query()
                .aggregate()
                .sum("quantity", "totalQuantity")
                .single();

            // 验证
            assertThat(result.getBigDecimal("totalQuantity")).isGreaterThanOrEqualTo(new BigDecimal("5"));
        }
    }

    @Nested
    @DisplayName("AVG 聚合")
    class AvgAggregate {

        @Test
        @DisplayName("should calculate average when aggregate with avg")
        void shouldCalculateAverage_whenAggregateWithAvg() {
            // 准备：创建订单项
            TestOrder order = dataManager.save(TestOrder.class, TestOrder.create("ORD-AVG", null));
            dataManager.save(TestOrderItem.class,
                TestOrderItem.create(order.getId(), "P1", new BigDecimal("100.00"), 1));
            dataManager.save(TestOrderItem.class,
                TestOrderItem.create(order.getId(), "P2", new BigDecimal("200.00"), 1));
            dataManager.save(TestOrderItem.class,
                TestOrderItem.create(order.getId(), "P3", new BigDecimal("300.00"), 1));

            // 执行：计算平均价格
            AggregateResult result = dataManager.entity(TestOrderItem.class)
                .query()
                .aggregate()
                .avg("price", "avgPrice")
                .single();

            // 验证：平均价格应该是 200.00
            BigDecimal avgPrice = result.getBigDecimal("avgPrice");
            assertThat(avgPrice).isNotNull();
            assertThat(avgPrice).isBetween(new BigDecimal("199.99"), new BigDecimal("200.01"));
        }
    }

    @Nested
    @DisplayName("MAX/MIN 聚合")
    class MaxMinAggregate {

        @Test
        @DisplayName("should find max and min values when aggregate with max and min")
        void shouldFindMaxAndMinValues_whenAggregateWithMaxAndMin() {
            // 准备：创建订单项
            TestOrder order = dataManager.save(TestOrder.class, TestOrder.create("ORD-MAXMIN", null));
            dataManager.save(TestOrderItem.class,
                TestOrderItem.create(order.getId(), "P1", new BigDecimal("50.00"), 1));
            dataManager.save(TestOrderItem.class,
                TestOrderItem.create(order.getId(), "P2", new BigDecimal("150.00"), 1));
            dataManager.save(TestOrderItem.class,
                TestOrderItem.create(order.getId(), "P3", new BigDecimal("250.00"), 1));

            // 执行：计算最大和最小价格
            AggregateResult result = dataManager.entity(TestOrderItem.class)
                .query()
                .aggregate()
                .max("price", "maxPrice")
                .min("price", "minPrice")
                .single();

            // 验证
            assertThat(result.getBigDecimal("maxPrice")).isEqualByComparingTo("250.00");
            assertThat(result.getBigDecimal("minPrice")).isEqualByComparingTo("50.00");
        }
    }

    @Nested
    @DisplayName("聚合 + WHERE 条件")
    class AggregateWithCondition {

        @Test
        @DisplayName("should filter before aggregate when aggregate with where")
        void shouldFilterBeforeAggregate_whenAggregateWithWhere() {
            // 准备：创建不同状态的用户
            dataManager.save(TestUser.class, TestUser.create("cond-active-1", "ca1@test.com", 1));
            dataManager.save(TestUser.class, TestUser.create("cond-active-2", "ca2@test.com", 1));
            dataManager.save(TestUser.class, TestUser.create("cond-inactive", "ci@test.com", 0));

            // 执行：只统计活跃用户
            AggregateResult result = dataManager.entity(TestUser.class)
                .query()
                .aggregate()
                .where(Conditions.builder(TestUser.class).eq(TestUser::getStatus, 1).build())
                .count("id", "activeCount")
                .single();

            // 验证
            assertThat(result.getLong("activeCount")).isGreaterThanOrEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("COUNT DISTINCT 聚合")
    class CountDistinctAggregate {

        @Test
        @DisplayName("should count distinct values when aggregate with countDistinct")
        void shouldCountDistinctValues_whenAggregateWithCountDistinct() {
            // 准备：创建有重复邮箱域名的用户
            dataManager.save(TestUser.class, TestUser.create("u1", "same@test.com"));
            dataManager.save(TestUser.class, TestUser.create("u2", "same@test.com"));
            dataManager.save(TestUser.class, TestUser.create("u3", "other@test.com"));

            // 执行：统计不同邮箱的数量
            AggregateResult result = dataManager.entity(TestUser.class)
                .query()
                .aggregate()
                .countDistinct("email", "uniqueEmails")
                .single();

            // 验证：至少有 2 个不同的邮箱
            assertThat(result.getLong("uniqueEmails")).isGreaterThanOrEqualTo(2L);
        }
    }
}
