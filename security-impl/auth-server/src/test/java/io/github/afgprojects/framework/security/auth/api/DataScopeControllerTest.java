package io.github.afgprojects.framework.security.auth.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import io.github.afgprojects.framework.security.datascope.entity.SecDataScope;
import io.github.afgprojects.framework.security.datascope.service.JdbcDataScopeService;

/**
 * DataScopeController 集成测试
 */
@ExtendWith(MockitoExtension.class)
class DataScopeControllerTest {

    @Mock
    private JdbcDataScopeService dataScopeService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DataScopeController controller = new DataScopeController(dataScopeService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    @DisplayName("数据权限 CRUD 测试")
    class DataScopeCrudTests {

        @Test
        @DisplayName("创建数据权限")
        void shouldCreateDataScope() throws Exception {
            SecDataScope scope = new SecDataScope();
            scope.setId(1L);
            scope.setScopeName("本部门数据");
            scope.setScopeCode("DEPT_SCOPE");
            scope.setScopeType("DEPT");

            when(dataScopeService.create(any())).thenReturn(scope);

            mockMvc.perform(post("/data-scopes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "scopeName": "本部门数据",
                            "scopeCode": "DEPT_SCOPE",
                            "scopeType": "DEPT"
                        }
                        """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.scopeName").value("本部门数据"));
        }

        @Test
        @DisplayName("获取数据权限详情")
        void shouldGetDataScopeById() throws Exception {
            SecDataScope scope = new SecDataScope();
            scope.setId(1L);
            scope.setScopeName("本部门数据");
            scope.setScopeCode("DEPT_SCOPE");
            scope.setScopeType("DEPT");

            when(dataScopeService.findById(1L)).thenReturn(java.util.Optional.of(scope));

            mockMvc.perform(get("/data-scopes/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scopeName").value("本部门数据"));
        }

        @Test
        @DisplayName("获取数据权限列表")
        void shouldListDataScopes() throws Exception {
            SecDataScope s1 = new SecDataScope();
            s1.setId(1L);
            s1.setScopeName("全部数据");
            s1.setScopeCode("ALL_SCOPE");
            s1.setScopeType("ALL");

            SecDataScope s2 = new SecDataScope();
            s2.setId(2L);
            s2.setScopeName("本部门数据");
            s2.setScopeCode("DEPT_SCOPE");
            s2.setScopeType("DEPT");

            when(dataScopeService.findAll("tenant-001")).thenReturn(List.of(s1, s2));

            mockMvc.perform(get("/data-scopes")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("删除数据权限")
        void shouldDeleteDataScope() throws Exception {
            doNothing().when(dataScopeService).delete(1L);

            mockMvc.perform(delete("/data-scopes/1"))
                    .andExpect(status().isOk());

            verify(dataScopeService).delete(1L);
        }
    }

    @Nested
    @DisplayName("用户数据权限测试")
    class UserDataScopeTests {

        @Test
        @DisplayName("获取用户数据权限")
        void shouldGetUserDataScopes() throws Exception {
            SecDataScope scope = new SecDataScope();
            scope.setId(1L);
            scope.setScopeName("本部门数据");
            scope.setScopeCode("DEPT_SCOPE");
            scope.setScopeType("DEPT");

            when(dataScopeService.getUserDataScopes("user-001", "tenant-001")).thenReturn(List.of(scope));

            mockMvc.perform(get("/data-scopes/user/user-001")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("设置用户数据权限")
        void shouldSetUserDataScopes() throws Exception {
            doNothing().when(dataScopeService).setUserDataScopes("user-001", Set.of(1L, 2L), "tenant-001");

            mockMvc.perform(post("/data-scopes/user/user-001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("[1, 2]")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk());

            verify(dataScopeService).setUserDataScopes("user-001", Set.of(1L, 2L), "tenant-001");
        }
    }
}