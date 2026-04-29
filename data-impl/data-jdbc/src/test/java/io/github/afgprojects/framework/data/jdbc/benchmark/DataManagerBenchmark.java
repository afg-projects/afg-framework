package io.github.afgprojects.framework.data.jdbc.benchmark;

import io.github.afgprojects.framework.data.core.EntityProxy;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.jdbc.JdbcDataManager;
import lombok.Data;
import org.h2.jdbcx.JdbcDataSource;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * DataManager CRUD 操作性能基准测试
 * <p>
 * 测试场景：
 * <ul>
 *   <li>单条记录查询（findById）</li>
 *   <li>批量插入（insertAll）</li>
 *   <li>条件查询（findAll with Condition）</li>
 *   <li>分页查询</li>
 * </ul>
 * <p>
 * 注意：此基准测试需要 H2 数据库环境
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class DataManagerBenchmark {

    /**
     * 测试实体类
     */
    @Data
    public static class TestUser {
        private Long id;
        private String name;
        private String email;
        private Integer age;
        private Integer status;
        private String department;
    }

    /**
     * 数据库状态
     */
    @State(Scope.Benchmark)
    public static class DatabaseState {
        JdbcDataSource dataSource;
        JdbcDataManager dataManager;
        EntityProxy<TestUser> userProxy;

        // 已存在的用户ID列表
        List<Long> existingUserIds = new ArrayList<>();

        @Setup(Level.Trial)
        public void setup() {
            // 创建 H2 数据源
            dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:benchmark;DB_CLOSE_DELAY=-1;MODE=MySQL");
            dataSource.setUser("sa");
            dataSource.setPassword("");

            // 创建 DataManager
            dataManager = new JdbcDataManager(dataSource);

            // 创建表
            dataManager.getJdbcClient().sql("""
                CREATE TABLE IF NOT EXISTS test_user (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100),
                    email VARCHAR(200),
                    age INT,
                    status INT DEFAULT 1,
                    department VARCHAR(100)
                )
                """).update();

            // 预插入数据
            userProxy = dataManager.entity(TestUser.class);
            for (int i = 0; i < 1000; i++) {
                TestUser user = new TestUser();
                user.setName("user-" + i);
                user.setEmail("user-" + i + "@example.com");
                user.setAge(20 + (i % 50));
                user.setStatus(1);
                user.setDepartment("dept-" + (i % 10));

                TestUser saved = userProxy.insert(user);
                existingUserIds.add(saved.getId());
            }
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            // 清理数据（H2 内存数据库会在连接关闭时自动清理）
            try {
                dataManager.getJdbcClient().sql("DROP TABLE IF EXISTS test_user").update();
            } catch (Exception e) {
                // 忽略清理错误
            }
        }
    }

    /**
     * 新实体状态
     */
    @State(Scope.Thread)
    public static class NewEntityState {
        TestUser newUser;

        @Setup(Level.Invocation)
        public void setup() {
            newUser = new TestUser();
            newUser.setName("new-user-" + Thread.currentThread().getId());
            newUser.setEmail("new-user@example.com");
            newUser.setAge(25);
            newUser.setStatus(1);
            newUser.setDepartment("new-dept");
        }
    }

    /**
     * 查询条件状态
     */
    @State(Scope.Thread)
    public static class ConditionState {
        Condition simpleCondition;
        Condition complexCondition;

        @Setup(Level.Trial)
        public void setup() {
            simpleCondition = Conditions.eq("status", 1);

            complexCondition = Conditions.builder()
                    .eq("status", 1)
                    .gt("age", 20)
                    .like("name", "user")
                    .build();
        }
    }

    // ==================== 单条记录查询测试 ====================

    /**
     * 测试 findById - 存在的记录
     */
    @Benchmark
    public Optional<TestUser> findByIdExisting(DatabaseState state) {
        Long id = state.existingUserIds.get(0);
        return state.userProxy.findById(id);
    }

    /**
     * 测试 findById - 不存在的记录
     */
    @Benchmark
    public Optional<TestUser> findByIdNotFound(DatabaseState state) {
        return state.userProxy.findById(999999L);
    }

    /**
     * 测试 findAllById - 批量 ID 查询
     */
    @Benchmark
    public List<TestUser> findAllByIdBatch(DatabaseState state) {
        List<Long> ids = state.existingUserIds.subList(0, 10);
        return state.userProxy.findAllById(ids);
    }

    // ==================== 插入测试 ====================

    /**
     * 测试单条插入
     */
    @Benchmark
    public TestUser insertSingle(DatabaseState state, NewEntityState entityState) {
        return state.userProxy.insert(entityState.newUser);
    }

    /**
     * 测试批量插入 - 小批次
     */
    @Benchmark
    public List<TestUser> insertBatchSmall(DatabaseState state) {
        List<TestUser> users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TestUser user = new TestUser();
            user.setName("batch-user-" + i);
            user.setEmail("batch-user-" + i + "@example.com");
            user.setAge(25);
            user.setStatus(1);
            user.setDepartment("batch-dept");
            users.add(user);
        }
        return state.userProxy.insertAll(users);
    }

    // ==================== 更新测试 ====================

    /**
     * 测试单条更新
     */
    @Benchmark
    public TestUser updateSingle(DatabaseState state) {
        Long id = state.existingUserIds.get(0);
        Optional<TestUser> userOpt = state.userProxy.findById(id);
        if (userOpt.isPresent()) {
            TestUser user = userOpt.get();
            user.setAge(user.getAge() + 1);
            return state.userProxy.update(user);
        }
        return null;
    }

    // ==================== 条件查询测试 ====================

    /**
     * 测试简单条件查询
     */
    @Benchmark
    public List<TestUser> findAllSimpleCondition(DatabaseState state, ConditionState condState) {
        return state.userProxy.findAll(condState.simpleCondition);
    }

    /**
     * 测试复杂条件查询
     */
    @Benchmark
    public List<TestUser> findAllComplexCondition(DatabaseState state, ConditionState condState) {
        return state.userProxy.findAll(condState.complexCondition);
    }

    /**
     * 测试 findOne
     */
    @Benchmark
    public Optional<TestUser> findOne(DatabaseState state) {
        Condition condition = Conditions.eq("email", "user-0@example.com");
        return state.userProxy.findOne(condition);
    }

    /**
     * 测试 findFirst
     */
    @Benchmark
    public Optional<TestUser> findFirst(DatabaseState state, ConditionState condState) {
        return state.userProxy.findFirst(condState.simpleCondition);
    }

    // ==================== 统计测试 ====================

    /**
     * 测试 count - 全表
     */
    @Benchmark
    public long countAll(DatabaseState state) {
        return state.userProxy.count();
    }

    /**
     * 测试 count - 条件
     */
    @Benchmark
    public long countWithCondition(DatabaseState state, ConditionState condState) {
        return state.userProxy.count(condState.simpleCondition);
    }

    /**
     * 测试 existsById
     */
    @Benchmark
    public boolean existsById(DatabaseState state) {
        Long id = state.existingUserIds.get(0);
        return state.userProxy.existsById(id);
    }

    // ==================== 删除测试 ====================

    /**
     * 测试 deleteById
     */
    @Benchmark
    public void deleteById(DatabaseState state) {
        // 先插入一条用于删除
        TestUser user = new TestUser();
        user.setName("delete-user");
        user.setEmail("delete@example.com");
        user.setAge(25);
        user.setStatus(1);
        TestUser saved = state.userProxy.insert(user);
        state.userProxy.deleteById(saved.getId());
    }

    // ==================== EntityProxy 获取测试 ====================

    /**
     * 测试获取 EntityProxy
     */
    @Benchmark
    public EntityProxy<TestUser> getEntityProxy(DatabaseState state) {
        return state.dataManager.entity(TestUser.class);
    }

    // ==================== SQL 构建测试 ====================

    /**
     * 测试 query() 创建
     */
    @Benchmark
    public Object createQueryBuilder(DatabaseState state) {
        return state.dataManager.query();
    }

    /**
     * 运行基准测试
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(DataManagerBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}