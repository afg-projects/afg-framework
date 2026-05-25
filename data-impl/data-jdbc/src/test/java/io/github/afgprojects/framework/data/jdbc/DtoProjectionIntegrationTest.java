package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.mapper.MappingField;
import io.github.afgprojects.framework.data.core.mapper.Projection;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Page;
import io.github.afgprojects.framework.data.core.query.ProjectedQuery;
import io.github.afgprojects.framework.data.core.query.Sort;
import io.github.afgprojects.framework.data.jdbc.mapper.DtoMapper;
import io.github.afgprojects.framework.data.jdbc.mapper.ResultMapperAdapter;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DTO 投影集成测试
 * <p>
 * 验证 DTO 投影的完整功能，包括：
 * - 同名字段自动映射
 * - @MappingField 注解映射
 * - 编程式 Projection 映射
 * - Record 和 POJO 支持
 * - 分页、条件查询、排序
 * - DtoMapper 直接使用
 * </p>
 */
@DisplayName("DTO 投影集成测试")
class DtoProjectionIntegrationTest {

    private JdbcDataManager dataManager;
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        dataSource = createDataSource();
        dataManager = new JdbcDataManager(dataSource);
    }

    @AfterEach
    void tearDown() {
        dropAllTables();
    }

    @Nested
    @DisplayName("DtoMapper 直接映射")
    class DtoMapperDirectTests {

        @BeforeEach
        void setUp() {
            createProductTable();
            insertTestProducts();
        }

        @Test
        @DisplayName("应该映射 ResultSet 到 Java Record")
        void shouldMapResultSetToRecord() {
            DtoMapper<ProductRecord> mapper = new DtoMapper<>(ProductRecord.class,
                    io.github.afgprojects.framework.data.core.mapper.TypeHandlerRegistry.defaultRegistry());

            List<ProductRecord> results = dataManager.getJdbcClient()
                    .sql("SELECT * FROM dto_test_product ORDER BY name")
                    .query(ResultMapperAdapter.adapt(mapper))
                    .list();

            assertThat(results).hasSize(2);
            assertThat(results.get(0).name()).isEqualTo("Gadget");
            assertThat(results.get(0).price()).isEqualByComparingTo("19.99");
            assertThat(results.get(1).name()).isEqualTo("Widget");
            assertThat(results.get(1).price()).isEqualByComparingTo("9.99");
        }

        @Test
        @DisplayName("应该映射 ResultSet 到 POJO")
        void shouldMapResultSetToPojo() {
            DtoMapper<ProductPojo> mapper = new DtoMapper<>(ProductPojo.class,
                    io.github.afgprojects.framework.data.core.mapper.TypeHandlerRegistry.defaultRegistry());

            List<ProductPojo> results = dataManager.getJdbcClient()
                    .sql("SELECT * FROM dto_test_product ORDER BY name")
                    .query(ResultMapperAdapter.adapt(mapper))
                    .list();

            assertThat(results).hasSize(2);
            assertThat(results.get(0).getName()).isEqualTo("Gadget");
            assertThat(results.get(0).getPrice()).isEqualByComparingTo("19.99");
        }

        @Test
        @DisplayName("应该支持 @MappingField 注解映射")
        void shouldMapWithAnnotation() {
            DtoMapper<ProductAnnotatedDto> mapper = new DtoMapper<>(ProductAnnotatedDto.class,
                    io.github.afgprojects.framework.data.core.mapper.TypeHandlerRegistry.defaultRegistry());

            List<ProductAnnotatedDto> results = dataManager.getJdbcClient()
                    .sql("SELECT * FROM dto_test_product ORDER BY name")
                    .query(ResultMapperAdapter.adapt(mapper))
                    .list();

            assertThat(results).hasSize(2);
            assertThat(results.get(0).getDisplayName()).isEqualTo("Gadget");
            assertThat(results.get(1).getDisplayName()).isEqualTo("Widget");
        }

        @Test
        @DisplayName("应该支持 snake_case 到 camelCase 自动映射")
        void shouldMapSnakeCaseToCamelCase() {
            DtoMapper<ProductDetailPojo> mapper = new DtoMapper<>(ProductDetailPojo.class,
                    io.github.afgprojects.framework.data.core.mapper.TypeHandlerRegistry.defaultRegistry());

            List<ProductDetailPojo> results = dataManager.getJdbcClient()
                    .sql("SELECT * FROM dto_test_product ORDER BY name")
                    .query(ResultMapperAdapter.adapt(mapper))
                    .list();

            assertThat(results).hasSize(2);
            assertThat(results.get(0).getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("应该映射带条件查询的结果")
        void shouldMapWithConditionQuery() {
            DtoMapper<ProductRecord> mapper = new DtoMapper<>(ProductRecord.class,
                    io.github.afgprojects.framework.data.core.mapper.TypeHandlerRegistry.defaultRegistry());

            List<ProductRecord> results = dataManager.getJdbcClient()
                    .sql("SELECT * FROM dto_test_product WHERE price > ?")
                    .param(10.0)
                    .query(ResultMapperAdapter.adapt(mapper))
                    .list();

            assertThat(results).hasSize(1);
            assertThat(results.get(0).name()).isEqualTo("Gadget");
        }
    }

    @Nested
    @DisplayName("ProjectedQuery 投影")
    class ProjectedQueryTests {

        @BeforeEach
        void setUp() {
            createProductTable();
            insertTestProducts();
        }

        @Test
        @DisplayName("应该通过 project() 投影到 Record DTO")
        void shouldProjectToRecordDto() {
            List<ProductRecord> results = dataManager.entity(Product.class)
                    .query()
                    .project(ProductRecord.class)
                    .list();

            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("应该通过 project() 投影到 POJO DTO")
        void shouldProjectToPojoDto() {
            List<ProductPojo> results = dataManager.entity(Product.class)
                    .query()
                    .project(ProductPojo.class)
                    .list();

            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("应该支持编程式 Projection 映射")
        void shouldSupportProgrammaticProjection() {
            Projection<Product, ProductExportDto> projection = Projection.of(
                    Product.class, ProductExportDto.class,
                    product -> {
                        ProductExportDto dto = new ProductExportDto();
                        dto.setLabel(product.getName() + " ($" + product.getPrice() + ")");
                        dto.setStockCode(product.getStock());
                        return dto;
                    }
            );

            List<ProductExportDto> results = dataManager.entity(Product.class)
                    .query()
                    .project(projection)
                    .list();

            assertThat(results).hasSize(2);
            assertThat(results.stream().map(ProductExportDto::getLabel).toList())
                    .containsExactlyInAnyOrder("Widget ($9.99)", "Gadget ($19.99)");
        }

        @Test
        @DisplayName("应该支持条件查询 + 投影")
        void shouldProjectWithCondition() {
            List<ProductRecord> results = dataManager.entity(Product.class)
                    .query()
                    .where(Conditions.builder(Product.class).gt(Product::getPrice, 10.0).build())
                    .project(ProductRecord.class)
                    .list();

            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("应该支持排序 + 投影")
        void shouldProjectWithSort() {
            List<ProductRecord> results = dataManager.entity(Product.class)
                    .query()
                    .orderBy(Sort.asc("name"))
                    .project(ProductRecord.class)
                    .list();

            assertThat(results).hasSize(2);
            assertThat(results.get(0).name()).isEqualTo("Gadget");
        }

        @Test
        @DisplayName("应该支持 limit + offset 投影")
        void shouldProjectWithLimitOffset() {
            List<ProductRecord> results = dataManager.entity(Product.class)
                    .query()
                    .orderBy(Sort.asc("name"))
                    .project(ProductRecord.class)
                    .limit(1)
                    .list();

            assertThat(results).hasSize(1);
            assertThat(results.get(0).name()).isEqualTo("Gadget");
        }

        @Test
        @DisplayName("one() 应该返回唯一结果")
        void shouldReturnOneResult() {
            Optional<ProductRecord> result = dataManager.entity(Product.class)
                    .query()
                    .where(Conditions.builder(Product.class).eq(Product::getName, "Widget").build())
                    .project(ProductRecord.class)
                    .one();

            assertThat(result).isPresent();
            assertThat(result.get().name()).isEqualTo("Widget");
        }

        @Test
        @DisplayName("first() 应该返回第一个结果")
        void shouldReturnFirstResult() {
            Optional<ProductRecord> result = dataManager.entity(Product.class)
                    .query()
                    .orderBy(Sort.asc("name"))
                    .project(ProductRecord.class)
                    .first();

            assertThat(result).isPresent();
            assertThat(result.get().name()).isEqualTo("Gadget");
        }

        @Test
        @DisplayName("count() 应该返回匹配行数")
        void shouldReturnCount() {
            long count = dataManager.entity(Product.class)
                    .query()
                    .where(Conditions.builder(Product.class).gt(Product::getPrice, 10.0).build())
                    .project(ProductRecord.class)
                    .count();

            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("应该支持分页投影")
        void shouldSupportPageProjection() {
            Page<ProductRecord> page = dataManager.entity(Product.class)
                    .query()
                    .project(ProductRecord.class)
                    .page(PageRequest.of(1, 1));

            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getTotal()).isEqualTo(2);
        }

        @Test
        @DisplayName("应该支持分页投影（有 Projection）")
        void shouldSupportPageProjectionWithProgrammaticProjection() {
            Projection<Product, ProductExportDto> projection = Projection.of(
                    Product.class, ProductExportDto.class,
                    product -> {
                        ProductExportDto dto = new ProductExportDto();
                        dto.setLabel(product.getName());
                        return dto;
                    }
            );

            Page<ProductExportDto> page = dataManager.entity(Product.class)
                    .query()
                    .project(projection)
                    .page(PageRequest.of(1, 1));

            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getTotal()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("类型转换")
    class TypeConversionTests {

        @BeforeEach
        void setUp() {
            createProductTable();
            insertTestProducts();
        }

        @Test
        @DisplayName("应该正确处理 Integer → Long 类型转换")
        void shouldConvertIntegerToLong() {
            DtoMapper<ProductIdDto> mapper = new DtoMapper<>(ProductIdDto.class,
                    io.github.afgprojects.framework.data.core.mapper.TypeHandlerRegistry.defaultRegistry());

            List<ProductIdDto> results = dataManager.getJdbcClient()
                    .sql("SELECT id, name FROM dto_test_product")
                    .query(ResultMapperAdapter.adapt(mapper))
                    .list();

            assertThat(results).hasSize(2);
            assertThat(results.get(0).getId()).isInstanceOf(Long.class);
        }

        @Test
        @DisplayName("应该正确处理 BigDecimal 价格映射")
        void shouldMapBigDecimalPrice() {
            DtoMapper<ProductRecord> mapper = new DtoMapper<>(ProductRecord.class,
                    io.github.afgprojects.framework.data.core.mapper.TypeHandlerRegistry.defaultRegistry());

            List<ProductRecord> results = dataManager.getJdbcClient()
                    .sql("SELECT name, price FROM dto_test_product ORDER BY name")
                    .query(ResultMapperAdapter.adapt(mapper))
                    .list();

            assertThat(results.get(0).price()).isEqualByComparingTo("19.99");
            assertThat(results.get(1).price()).isEqualByComparingTo("9.99");
        }
    }

    // ==================== 辅助方法 ====================

    private DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:dto_proj_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private void createProductTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE dto_test_product (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(200) NOT NULL,
                    price DECIMAL(15,2),
                    stock INTEGER,
                    created_at TIMESTAMP
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create product table", e);
        }
    }

    private void insertTestProducts() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO dto_test_product (name, price, stock, created_at) VALUES (?, ?, ?, ?)")) {
            pstmt.setString(1, "Widget");
            pstmt.setBigDecimal(2, new BigDecimal("9.99"));
            pstmt.setInt(3, 100);
            pstmt.setTimestamp(4, java.sql.Timestamp.valueOf("2024-01-15 10:30:00"));
            pstmt.executeUpdate();

            pstmt.setString(1, "Gadget");
            pstmt.setBigDecimal(2, new BigDecimal("19.99"));
            pstmt.setInt(3, 50);
            pstmt.setTimestamp(4, java.sql.Timestamp.valueOf("2024-02-20 14:00:00"));
            pstmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to insert test products", e);
        }
    }

    private void dropAllTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS dto_test_product");
        } catch (Exception ignored) {
        }
    }

    // ==================== 实体类 ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @jakarta.persistence.Table(name = "dto_test_product")
    static class Product {
        private Long id;
        private String name;
        private BigDecimal price;
        private Integer stock;
        private LocalDateTime createdAt;
    }

    // ==================== DTO 类 ====================

    record ProductRecord(String name, BigDecimal price) {}

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class ProductPojo {
        private String name;
        private BigDecimal price;
    }

    @Data
    @NoArgsConstructor
    static class ProductAnnotatedDto {
        @MappingField(column = "name")
        private String displayName;

        private BigDecimal price;
    }

    @Data
    @NoArgsConstructor
    static class ProductDetailPojo {
        private String name;
        private BigDecimal price;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    static class ProductExportDto {
        private String label;
        private Integer stockCode;
    }

    @Data
    @NoArgsConstructor
    static class ProductIdDto {
        private Long id;
        private String name;
    }
}