package io.github.afgprojects.framework.core.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * AuditLogAspect 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AuditLogAspectTest {

    @Mock
    private AuditLogStorage storage;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private AuditLogProperties properties;
    private AuditLogAspect aspect;

    @BeforeEach
    void setUp() {
        properties = new AuditLogProperties();
        aspect = new AuditLogAspect(storage, properties);
    }

    @Test
    void should_recordAuditLog_when_methodSucceeds() throws Throwable {
        // Given
        Audited annotation = createAudited("创建用户", "用户管理", new String[]{});
        setupJoinPoint("createUser", new String[]{"request"}, new Object[]{"test-data"});

        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = aspect.auditAround(joinPoint, annotation);

        // Then
        assertThat(result).isEqualTo("result");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(storage).save(captor.capture());

        AuditLog auditLog = captor.getValue();
        assertThat(auditLog.operation()).isEqualTo("创建用户");
        assertThat(auditLog.module()).isEqualTo("用户管理");
        assertThat(auditLog.result()).isEqualTo(AuditLog.Result.SUCCESS);
        assertThat(auditLog.methodName()).isEqualTo("createUser");
        assertThat(auditLog.errorMessage()).isNull();
    }

    @Test
    void should_recordFailureLog_when_methodThrowsException() throws Throwable {
        // Given
        Audited annotation = createAudited("删除用户", "用户管理", new String[]{});
        setupJoinPoint("deleteUser", new String[]{"id"}, new Object[]{"user-123"});

        when(joinPoint.proceed()).thenThrow(new RuntimeException("用户不存在"));

        // When & Then
        assertThatThrownBy(() -> aspect.auditAround(joinPoint, annotation))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("用户不存在");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(storage).save(captor.capture());

        AuditLog auditLog = captor.getValue();
        assertThat(auditLog.result()).isEqualTo(AuditLog.Result.FAILURE);
        assertThat(auditLog.errorMessage()).isEqualTo("用户不存在");
    }

    @Test
    void should_maskSensitiveFields_when_configured() throws Throwable {
        // Given
        Audited annotation = createAudited("更新密码", "用户管理", new String[]{"password", "oldPassword"});
        setupJoinPoint("updatePassword", new String[]{"userId", "password", "oldPassword"}, new Object[]{"user-1", "newSecret", "oldSecret"});

        when(joinPoint.proceed()).thenReturn(null);

        // When
        aspect.auditAround(joinPoint, annotation);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(storage).save(captor.capture());

        AuditLog auditLog = captor.getValue();
        assertThat(auditLog.args()).contains("password=***");
        assertThat(auditLog.args()).contains("oldPassword=***");
        assertThat(auditLog.args()).contains("userId=\"user-1\"");
    }

    @Test
    void should_useMethodNameAsOperation_when_notSpecified() throws Throwable {
        // Given
        Audited annotation = createAudited("", "", new String[]{});
        setupJoinPoint("createUser", new String[]{}, new Object[]{});

        when(joinPoint.proceed()).thenReturn("result");

        // When
        aspect.auditAround(joinPoint, annotation);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(storage).save(captor.capture());

        AuditLog auditLog = captor.getValue();
        assertThat(auditLog.operation()).isEqualTo("createUser");
        assertThat(auditLog.module()).isEqualTo("TestService");
    }

    @Test
    void should_notRecordArgs_when_recordArgsIsFalse() throws Throwable {
        // Given
        Audited annotation = new Audited() {
            @Override
            public String operation() {
                return "test";
            }

            @Override
            public String module() {
                return "test";
            }

            @Override
            public String[] sensitiveFields() {
                return new String[0];
            }

            @Override
            public boolean recordArgs() {
                return false;
            }

            @Override
            public boolean recordResult() {
                return true;
            }

            @Override
            public String target() {
                return "";
            }

            @Override
            public Class<Audited> annotationType() {
                return Audited.class;
            }
        };
        setupJoinPoint("testMethod", new String[]{"param"}, new Object[]{"value"});

        when(joinPoint.proceed()).thenReturn("result");

        // When
        aspect.auditAround(joinPoint, annotation);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(storage).save(captor.capture());

        AuditLog auditLog = captor.getValue();
        assertThat(auditLog.args()).isNull();
    }

    @Test
    void should_extractTarget_when_targetExpressionProvided() throws Throwable {
        // Given
        Audited annotation = new Audited() {
            @Override
            public String operation() {
                return "更新用户";
            }

            @Override
            public String module() {
                return "用户管理";
            }

            @Override
            public String[] sensitiveFields() {
                return new String[0];
            }

            @Override
            public boolean recordArgs() {
                return true;
            }

            @Override
            public boolean recordResult() {
                return true;
            }

            @Override
            public String target() {
                return "#userId";
            }

            @Override
            public Class<Audited> annotationType() {
                return Audited.class;
            }
        };
        setupJoinPoint("updateUser", new String[]{"userId", "name"}, new Object[]{"user-123", "张三"});

        when(joinPoint.proceed()).thenReturn("result");

        // When
        aspect.auditAround(joinPoint, annotation);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(storage).save(captor.capture());

        AuditLog auditLog = captor.getValue();
        assertThat(auditLog.target()).isEqualTo("user-123");
    }

    private void setupJoinPoint(String methodName, String[] paramNames, Object[] args) {
        lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
        lenient().when(methodSignature.getName()).thenReturn(methodName);
        lenient().when(methodSignature.getDeclaringType()).thenReturn(TestService.class);
        lenient().when(methodSignature.getParameterNames()).thenReturn(paramNames);
        lenient().when(joinPoint.getArgs()).thenReturn(args);
    }

    private Audited createAudited(String operation, String module, String[] sensitiveFields) {
        return new Audited() {
            @Override
            public String operation() {
                return operation;
            }

            @Override
            public String module() {
                return module;
            }

            @Override
            public String[] sensitiveFields() {
                return sensitiveFields;
            }

            @Override
            public boolean recordArgs() {
                return true;
            }

            @Override
            public boolean recordResult() {
                return true;
            }

            @Override
            public String target() {
                return "";
            }

            @Override
            public Class<Audited> annotationType() {
                return Audited.class;
            }
        };
    }

    // 用于方法名解析的测试服务类
    static class TestService {}
}
