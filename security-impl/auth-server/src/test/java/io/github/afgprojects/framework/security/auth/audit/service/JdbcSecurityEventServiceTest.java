package io.github.afgprojects.framework.security.auth.audit.service;

import io.github.afgprojects.framework.data.jdbc.JdbcDataManager;
import io.github.afgprojects.framework.security.auth.audit.model.SecurityEvent;
import io.github.afgprojects.framework.security.auth.audit.model.SecurityEventType;
import io.github.afgprojects.framework.security.core.audit.SecurityEventService.SecurityEventInfo;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JdbcSecurityEventService 测试。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@DisplayName("JdbcSecurityEventService 测试")
class JdbcSecurityEventServiceTest {

    private JdbcDataSource dataSource;
    private JdbcDataManager dataManager;
    private JdbcSecurityEventService eventService;

    @BeforeEach
    void setUp() {
        // 创建 H2 数据源
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:testdb_security_event;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        // 创建 DataManager
        dataManager = new JdbcDataManager(dataSource);

        // 创建表
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS auth_security_event");
            stmt.execute("""
                CREATE TABLE auth_security_event (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    event_type VARCHAR(50) NOT NULL,
                    user_id VARCHAR(64),
                    tenant_id VARCHAR(64),
                    ip VARCHAR(64),
                    details TEXT,
                    created_at TIMESTAMP NOT NULL
                )
                """);
            stmt.execute("CREATE INDEX idx_event_type ON auth_security_event (event_type)");
            stmt.execute("CREATE INDEX idx_created_at ON auth_security_event (created_at)");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 创建服务
        eventService = new JdbcSecurityEventService(dataManager);
    }

    @AfterEach
    void tearDown() {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS auth_security_event");
        } catch (Exception e) {
            // ignore
        }
    }

    @Nested
    @DisplayName("recordEvent 方法测试")
    class RecordEventTests {

        @Test
        @DisplayName("应该成功记录安全事件")
        void shouldRecordSecurityEvent() {
            // given
            SecurityEvent event = SecurityEvent.builder()
                    .eventType(SecurityEventType.LOGIN_FAILURE_EXCESSIVE)
                    .userId("user-001")
                    .tenantId("tenant-001")
                    .ip("192.168.1.100")
                    .details("连续5次登录失败")
                    .createdAt(Instant.now())
                    .build();

            // when
            eventService.recordEvent(event);

            // then
            long count = dataManager.getJdbcClient()
                    .sql("SELECT COUNT(*) FROM auth_security_event WHERE user_id = ?")
                    .param("user-001")
                    .query(Long.class)
                    .single();
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("应该记录所有安全事件字段")
        void shouldRecordAllEventFields() {
            // given
            Instant now = Instant.now();
            SecurityEvent event = SecurityEvent.builder()
                    .eventType(SecurityEventType.SUSPICIOUS_IP)
                    .userId("user-002")
                    .tenantId("tenant-002")
                    .ip("10.0.0.1")
                    .details("IP在黑名单中")
                    .createdAt(now)
                    .build();

            // when
            eventService.recordEvent(event);

            // then
            var result = dataManager.getJdbcClient()
                    .sql("SELECT * FROM auth_security_event WHERE user_id = ?")
                    .param("user-002")
                    .query((rs, rowNum) -> {
                        java.util.Map<String, Object> map = new java.util.HashMap<>();
                        java.sql.ResultSetMetaData metaData = rs.getMetaData();
                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
                            map.put(metaData.getColumnName(i).toLowerCase(), rs.getObject(i));
                        }
                        return map;
                    })
                    .optional()
                    .orElse(null);

            assertThat(result).isNotNull();
            assertThat(result.get("event_type")).isEqualTo("SUSPICIOUS_IP");
            assertThat(result.get("user_id")).isEqualTo("user-002");
            assertThat(result.get("tenant_id")).isEqualTo("tenant-002");
            assertThat(result.get("ip")).isEqualTo("10.0.0.1");
            assertThat(result.get("details")).isEqualTo("IP在黑名单中");
        }

        @Test
        @DisplayName("应该支持可选字段为 null")
        void shouldSupportNullOptionalFields() {
            // given
            SecurityEvent event = SecurityEvent.builder()
                    .eventType(SecurityEventType.LOGIN_FROM_NEW_DEVICE)
                    .userId(null)
                    .tenantId(null)
                    .ip("192.168.1.100")
                    .details(null)
                    .createdAt(Instant.now())
                    .build();

            // when
            eventService.recordEvent(event);

            // then
            var result = dataManager.getJdbcClient()
                    .sql("SELECT * FROM auth_security_event WHERE ip = ?")
                    .param("192.168.1.100")
                    .query((rs, rowNum) -> {
                        java.util.Map<String, Object> map = new java.util.HashMap<>();
                        java.sql.ResultSetMetaData metaData = rs.getMetaData();
                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
                            map.put(metaData.getColumnName(i).toLowerCase(), rs.getObject(i));
                        }
                        return map;
                    })
                    .optional()
                    .orElse(null);

            assertThat(result).isNotNull();
            assertThat(result.get("user_id")).isNull();
            assertThat(result.get("tenant_id")).isNull();
            assertThat(result.get("details")).isNull();
        }

        @Test
        @DisplayName("应该支持不同类型的安全事件")
        void shouldSupportDifferentEventTypes() {
            // given
            SecurityEventType[] types = SecurityEventType.values();

            // when
            for (SecurityEventType type : types) {
                SecurityEvent event = SecurityEvent.builder()
                        .eventType(type)
                        .ip("192.168.1.100")
                        .createdAt(Instant.now())
                        .build();
                eventService.recordEvent(event);
            }

            // then
            long count = dataManager.getJdbcClient()
                    .sql("SELECT COUNT(*) FROM auth_security_event")
                    .query(Long.class)
                    .single();
            assertThat(count).isEqualTo(types.length);
        }
    }

    @Nested
    @DisplayName("getRecentEvents 方法测试")
    class GetRecentEventsTests {

        @Test
        @DisplayName("应该获取指定时间范围内的安全事件")
        void shouldGetRecentEvents() {
            // given - 记录多个事件
            Instant now = Instant.now();
            for (int i = 0; i < 5; i++) {
                SecurityEvent event = SecurityEvent.builder()
                        .eventType(SecurityEventType.LOGIN_FAILURE_EXCESSIVE)
                        .userId("user-" + i)
                        .ip("192.168.1." + i)
                        .createdAt(now.minusSeconds(i * 60)) // 每分钟一个事件
                        .build();
                eventService.recordEvent(event);
            }

            // when - 获取最近3分钟的事件
            List<SecurityEventInfo> events = eventService.getRecentEvents(Duration.ofMinutes(3));

            // then
            assertThat(events).hasSize(3);
        }

        @Test
        @DisplayName("应该按时间倒序返回事件")
        void shouldReturnEventsInDescendingOrder() {
            // given
            Instant now = Instant.now();
            for (int i = 0; i < 3; i++) {
                SecurityEvent event = SecurityEvent.builder()
                        .eventType(SecurityEventType.LOGIN_FAILURE_EXCESSIVE)
                        .userId("user-" + i)
                        .ip("192.168.1." + i)
                        .createdAt(now.minusSeconds(i * 60))
                        .build();
                eventService.recordEvent(event);
            }

            // when
            List<SecurityEventInfo> events = eventService.getRecentEvents(Duration.ofMinutes(5));

            // then - 最新的事件在前
            assertThat(events).hasSize(3);
            assertThat(events.get(0).getUserId()).isEqualTo("user-0");
            assertThat(events.get(2).getUserId()).isEqualTo("user-2");
        }

        @Test
        @DisplayName("当没有事件时应该返回空列表")
        void shouldReturnEmptyListWhenNoEvents() {
            // when
            List<SecurityEventInfo> events = eventService.getRecentEvents(Duration.ofMinutes(10));

            // then
            assertThat(events).isEmpty();
        }
    }

    @Nested
    @DisplayName("getEventsByType 方法测试")
    class GetEventsByTypeTests {

        @Test
        @DisplayName("应该按类型获取安全事件")
        void shouldGetEventsByType() {
            // given - 记录不同类型的事件
            Instant now = Instant.now();
            for (int i = 0; i < 3; i++) {
                SecurityEvent event1 = SecurityEvent.builder()
                        .eventType(SecurityEventType.LOGIN_FAILURE_EXCESSIVE)
                        .userId("user-" + i)
                        .ip("192.168.1." + i)
                        .createdAt(now.minusSeconds(i * 60))
                        .build();
                SecurityEvent event2 = SecurityEvent.builder()
                        .eventType(SecurityEventType.SUSPICIOUS_IP)
                        .userId("user-" + i)
                        .ip("10.0.0." + i)
                        .createdAt(now.minusSeconds(i * 60))
                        .build();
                eventService.recordEvent(event1);
                eventService.recordEvent(event2);
            }

            // when
            List<SecurityEventInfo> events = eventService.getEventsByType(
                    "LOGIN_FAILURE_EXCESSIVE", Duration.ofMinutes(10));

            // then
            assertThat(events).hasSize(3);
            assertThat(events).allMatch(e -> "LOGIN_FAILURE_EXCESSIVE".equals(e.getEventType()));
        }

        @Test
        @DisplayName("应该同时按类型和时间范围过滤")
        void shouldFilterByTypeAndTimeRange() {
            // given
            Instant now = Instant.now();
            SecurityEvent oldEvent = SecurityEvent.builder()
                    .eventType(SecurityEventType.LOGIN_FAILURE_EXCESSIVE)
                    .userId("user-old")
                    .ip("192.168.1.100")
                    .createdAt(now.minusSeconds(300)) // 5分钟前
                    .build();
            SecurityEvent newEvent = SecurityEvent.builder()
                    .eventType(SecurityEventType.LOGIN_FAILURE_EXCESSIVE)
                    .userId("user-new")
                    .ip("192.168.1.101")
                    .createdAt(now.minusSeconds(60)) // 1分钟前
                    .build();
            eventService.recordEvent(oldEvent);
            eventService.recordEvent(newEvent);

            // when - 获取最近2分钟的事件
            List<SecurityEventInfo> events = eventService.getEventsByType(
                    "LOGIN_FAILURE_EXCESSIVE", Duration.ofMinutes(2));

            // then
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getUserId()).isEqualTo("user-new");
        }

        @Test
        @DisplayName("当指定类型没有事件时应该返回空列表")
        void shouldReturnEmptyListWhenNoEventsOfType() {
            // given
            SecurityEvent event = SecurityEvent.builder()
                    .eventType(SecurityEventType.LOGIN_FAILURE_EXCESSIVE)
                    .ip("192.168.1.100")
                    .createdAt(Instant.now())
                    .build();
            eventService.recordEvent(event);

            // when
            List<SecurityEventInfo> events = eventService.getEventsByType(
                    "SUSPICIOUS_IP", Duration.ofMinutes(10));

            // then
            assertThat(events).isEmpty();
        }
    }

    @Nested
    @DisplayName("多租户场景测试")
    class MultiTenantTests {

        @Test
        @DisplayName("应该区分不同租户的安全事件")
        void shouldDistinguishDifferentTenants() {
            // given
            SecurityEvent event1 = SecurityEvent.builder()
                    .eventType(SecurityEventType.LOGIN_FAILURE_EXCESSIVE)
                    .userId("user-001")
                    .tenantId("tenant-001")
                    .ip("192.168.1.100")
                    .createdAt(Instant.now())
                    .build();
            SecurityEvent event2 = SecurityEvent.builder()
                    .eventType(SecurityEventType.LOGIN_FAILURE_EXCESSIVE)
                    .userId("user-001")
                    .tenantId("tenant-002")
                    .ip("192.168.1.100")
                    .createdAt(Instant.now())
                    .build();
            eventService.recordEvent(event1);
            eventService.recordEvent(event2);

            // when
            List<SecurityEventInfo> events = eventService.getRecentEvents(Duration.ofMinutes(1));

            // then
            assertThat(events).hasSize(2);
            assertThat(events.stream().map(SecurityEventInfo::getTenantId))
                    .containsExactlyInAnyOrder("tenant-001", "tenant-002");
        }
    }

    @Nested
    @DisplayName("完整流程测试")
    class FullWorkflowTests {

        @Test
        @DisplayName("应该支持完整的安全事件记录和查询流程")
        void shouldSupportFullEventWorkflow() {
            // given - 记录多种类型的安全事件
            Instant now = Instant.now();

            // 登录失败过多
            eventService.recordEvent(SecurityEvent.builder()
                    .eventType(SecurityEventType.LOGIN_FAILURE_EXCESSIVE)
                    .userId("user-001")
                    .tenantId("tenant-001")
                    .ip("192.168.1.100")
                    .details("连续5次登录失败")
                    .createdAt(now)
                    .build());

            // 新设备登录
            eventService.recordEvent(SecurityEvent.builder()
                    .eventType(SecurityEventType.LOGIN_FROM_NEW_DEVICE)
                    .userId("user-001")
                    .tenantId("tenant-001")
                    .ip("192.168.1.100")
                    .details("设备: iPhone 15")
                    .createdAt(now)
                    .build());

            // 可疑IP
            eventService.recordEvent(SecurityEvent.builder()
                    .eventType(SecurityEventType.SUSPICIOUS_IP)
                    .userId("user-002")
                    .tenantId("tenant-001")
                    .ip("10.0.0.1")
                    .details("IP在黑名单中")
                    .createdAt(now)
                    .build());

            // when - 查询最近的事件
            List<SecurityEventInfo> recentEvents = eventService.getRecentEvents(Duration.ofMinutes(1));
            List<SecurityEventInfo> failureEvents = eventService.getEventsByType(
                    "LOGIN_FAILURE_EXCESSIVE", Duration.ofMinutes(1));

            // then
            assertThat(recentEvents).hasSize(3);
            assertThat(failureEvents).hasSize(1);
            assertThat(failureEvents.get(0).getUserId()).isEqualTo("user-001");
        }
    }
}
