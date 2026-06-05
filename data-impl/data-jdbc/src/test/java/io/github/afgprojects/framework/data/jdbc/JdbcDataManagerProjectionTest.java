package io.github.afgprojects.framework.data.jdbc;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.mapper.Projection;
import io.github.afgprojects.framework.data.core.query.Page;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.jdbc.entity.TestOrder;
import io.github.afgprojects.framework.data.jdbc.entity.TestOrderItem;
import io.github.afgprojects.framework.data.jdbc.entity.TestUser;
import io.github.afgprojects.framework.data.jdbc.test.BaseDataTest;

/**
 * JdbcDataManager DTO 投影集成测试
 * <p>
 * 使用 PostgreSQL Testcontainers + Liquibase 迁移。
 * 测试后自动回滚（@Transactional）。
 * </p>
 */
class JdbcDataManagerProjectionTest extends BaseDataTest {

    // ==================== Record 投影定义 ====================

    /**
     * 用户摘要 Record 投影
     */
    record UserSummary(Long id, String username, String email) {}

    /**
     * 用户状态 Record 投影（部分字段）
     */
    record UserStatus(Long id, Integer status) {}

    /**
     * 订单项摘要 Record 投影
     */
    record OrderItemSummary(String productName, BigDecimal price, Integer quantity) {}

    // ==================== POJO 投影定义 ====================

    /**
     * 用户详情 POJO 投影
     */
    static class UserDetail {
        private Long id;
        private String username;
        private String email;
        private Integer status;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }

    @Nested
    @DisplayName("Record 投影")
    class RecordProjection {

        @Test
        @DisplayName("should project to record when query with project")
        void shouldProjectToRecord_whenQueryWithProject() {
            // 准备
            dataManager.save(TestUser.class, TestUser.create("proj-user-1", "proj1@test.com"));
            dataManager.save(TestUser.class, TestUser.create("proj-user-2", "proj2@test.com"));

            // 执行：投影到 Record
            List<UserSummary> summaries = dataManager.entity(TestUser.class)
                .query()
                .project(UserSummary.class)
                .list();

            // 验证
            assertThat(summaries).isNotEmpty();
            assertThat(summaries).allSatisfy(s -> {
                assertThat(s.id()).isNotNull();
                assertThat(s.username()).isNotNull();
                assertThat(s.email()).isNotNull();
            });
        }

        @Test
        @DisplayName("should project with condition when project with where")
        void shouldProjectWithCondition_whenProjectWithWhere() {
            // 准备
            dataManager.save(TestUser.class, TestUser.create("cond-user-1", "cu1@test.com", 1));
            dataManager.save(TestUser.class, TestUser.create("cond-user-2", "cu2@test.com", 0));

            // 执行：带条件投影
            List<UserSummary> activeSummaries = dataManager.entity(TestUser.class)
                .query()
                .project(UserSummary.class)
                .where(Conditions.builder(TestUser.class).eq(TestUser::getStatus, 1).build())
                .list();

            // 验证：只有活跃用户
            assertThat(activeSummaries).isNotEmpty();
            assertThat(activeSummaries).allSatisfy(s ->
                assertThat(s.username()).isEqualTo("cond-user-1")
            );
        }

        @Test
        @DisplayName("should project single result when project with one")
        void shouldProjectSingleResult_whenProjectWithOne() {
            // 准备
            dataManager.save(TestUser.class, TestUser.create("single-user", "single@test.com"));

            // 执行
            Optional<UserSummary> result = dataManager.entity(TestUser.class)
                .query()
                .project(UserSummary.class)
                .where(Conditions.builder(TestUser.class).eq(TestUser::getUsername, "single-user").build())
                .one();

            // 验证
            assertThat(result).isPresent();
            assertThat(result.get().username()).isEqualTo("single-user");
        }
    }

    @Nested
    @DisplayName("POJO 投影")
    class PojoProjection {

        @Test
        @DisplayName("should project to POJO when query with project")
        void shouldProjectToPojo_whenQueryWithProject() {
            // 准备
            dataManager.save(TestUser.class, TestUser.create("pojo-user", "pojo@test.com", 1));

            // 执行：投影到 POJO
            List<UserDetail> details = dataManager.entity(TestUser.class)
                .query()
                .project(UserDetail.class)
                .list();

            // 验证
            assertThat(details).isNotEmpty();
            assertThat(details).anySatisfy(d -> {
                assertThat(d.getUsername()).isEqualTo("pojo-user");
                assertThat(d.getEmail()).isEqualTo("pojo@test.com");
                assertThat(d.getStatus()).isEqualTo(1);
            });
        }

        @Test
        @DisplayName("should project with pagination when project with page")
        void shouldProjectWithPagination_whenProjectWithPage() {
            // 准备
            for (int i = 0; i < 10; i++) {
                dataManager.save(TestUser.class, TestUser.create("page-user-" + i, "page" + i + "@test.com"));
            }

            // 执行：分页投影
            Page<UserDetail> page = dataManager.entity(TestUser.class)
                .query()
                .project(UserDetail.class)
                .page(PageRequest.of(1, 5));

            // 验证
            assertThat(page.getContent()).hasSize(5);
            assertThat(page.getTotal()).isGreaterThanOrEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("字段选择投影")
    class FieldSelectionProjection {

        @Test
        @DisplayName("should project selected fields when project with select")
        void shouldProjectSelectedFields_whenProjectWithSelect() {
            // 准备
            dataManager.save(TestUser.class, TestUser.create("sel-user", "sel@test.com", 1));

            // 执行：只选择 id 和 status 字段
            List<UserStatus> statuses = dataManager.entity(TestUser.class)
                .query()
                .project(UserStatus.class)
                .select(TestUser::getId, TestUser::getStatus)
                .list();

            // 验证
            assertThat(statuses).isNotEmpty();
            assertThat(statuses).allSatisfy(s -> {
                assertThat(s.id()).isNotNull();
                assertThat(s.status()).isNotNull();
            });
        }
    }

    @Nested
    @DisplayName("Projection 接口投影")
    class ProjectionInterface {

        @Test
        @DisplayName("should project using Projection interface when project with custom mapper")
        void shouldProjectUsingProjectionInterface_whenProjectWithCustomMapper() {
            // 准备
            dataManager.save(TestUser.class, TestUser.create("mapper-user", "mapper@test.com", 1));

            // 定义自定义投影
            Projection<TestUser, UserSummary> customProjection = Projection.of(
                TestUser.class, UserSummary.class,
                user -> new UserSummary(user.getId(), user.getUsername().toUpperCase(), user.getEmail())
            );

            // 执行
            List<UserSummary> summaries = dataManager.entity(TestUser.class)
                .query()
                .project(customProjection)
                .list();

            // 验证：用户名应该被转为大写
            assertThat(summaries).isNotEmpty();
            assertThat(summaries).anySatisfy(s ->
                assertThat(s.username()).isEqualTo("MAPPER-USER")
            );
        }

        @Test
        @DisplayName("should project with condition using Projection interface")
        void shouldProjectWithCondition_usingProjectionInterface() {
            // 准备
            TestOrder order = dataManager.save(TestOrder.class, TestOrder.create("ORD-PROJ", null));
            dataManager.save(TestOrderItem.class,
                TestOrderItem.create(order.getId(), "Item-A", new BigDecimal("99.99"), 3));

            // 定义投影：订单项 → 摘要
            Projection<TestOrderItem, OrderItemSummary> itemProjection = Projection.of(
                TestOrderItem.class, OrderItemSummary.class,
                item -> new OrderItemSummary(item.getProductName(), item.getPrice(), item.getQuantity())
            );

            // 执行
            List<OrderItemSummary> summaries = dataManager.entity(TestOrderItem.class)
                .query()
                .project(itemProjection)
                .where(Conditions.builder(TestOrderItem.class).eq(TestOrderItem::getOrderId, order.getId()).build())
                .list();

            // 验证
            assertThat(summaries).hasSize(1);
            assertThat(summaries.get(0).productName()).isEqualTo("Item-A");
            assertThat(summaries.get(0).price()).isEqualByComparingTo("99.99");
            assertThat(summaries.get(0).quantity()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("投影计数")
    class ProjectionCount {

        @Test
        @DisplayName("should count projected results when project with count")
        void shouldCountProjectedResults_whenProjectWithCount() {
            // 准备
            for (int i = 0; i < 3; i++) {
                dataManager.save(TestUser.class, TestUser.create("count-user-" + i, "count" + i + "@test.com"));
            }

            // 执行
            long count = dataManager.entity(TestUser.class)
                .query()
                .project(UserSummary.class)
                .count();

            // 验证
            assertThat(count).isGreaterThanOrEqualTo(3L);
        }
    }
}
