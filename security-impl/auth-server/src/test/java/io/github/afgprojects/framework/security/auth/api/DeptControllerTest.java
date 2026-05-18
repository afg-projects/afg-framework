package io.github.afgprojects.framework.security.auth.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;
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

import io.github.afgprojects.framework.security.datascope.entity.SecDept;
import io.github.afgprojects.framework.security.datascope.service.JdbcDeptService;

/**
 * DeptController 集成测试
 */
@ExtendWith(MockitoExtension.class)
class DeptControllerTest {

    @Mock
    private JdbcDeptService deptService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DeptController controller = new DeptController(deptService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    @DisplayName("部门 CRUD 测试")
    class DeptCrudTests {

        @Test
        @DisplayName("创建部门")
        void shouldCreateDept() throws Exception {
            SecDept dept = new SecDept();
            dept.setId(1L);
            dept.setDeptName("研发部");
            dept.setTenantId("tenant-001");

            when(deptService.create(any())).thenReturn(dept);

            mockMvc.perform(post("/depts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "deptName": "研发部",
                            "tenantId": "tenant-001"
                        }
                        """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.deptName").value("研发部"));
        }

        @Test
        @DisplayName("获取部门详情")
        void shouldGetDeptById() throws Exception {
            SecDept dept = new SecDept();
            dept.setId(1L);
            dept.setDeptName("研发部");

            when(deptService.findById(1L)).thenReturn(Optional.of(dept));

            mockMvc.perform(get("/depts/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deptName").value("研发部"));
        }

        @Test
        @DisplayName("获取部门列表")
        void shouldListDepts() throws Exception {
            SecDept d1 = new SecDept();
            d1.setId(1L);
            d1.setDeptName("研发部");

            SecDept d2 = new SecDept();
            d2.setId(2L);
            d2.setDeptName("产品部");

            when(deptService.findAll("tenant-001")).thenReturn(List.of(d1, d2));

            mockMvc.perform(get("/depts")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("获取部门树")
        void shouldGetDeptTree() throws Exception {
            SecDept root = new SecDept();
            root.setId(1L);
            root.setDeptName("总公司");

            when(deptService.getDeptTree("tenant-001")).thenReturn(List.of(root));

            mockMvc.perform(get("/depts/tree")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("获取子部门 ID")
        void shouldGetChildIds() throws Exception {
            when(deptService.getChildDeptIds(1L, "tenant-001")).thenReturn(Set.of(2L, 3L));

            mockMvc.perform(get("/depts/1/children")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("删除部门")
        void shouldDeleteDept() throws Exception {
            doNothing().when(deptService).delete(1L);

            mockMvc.perform(delete("/depts/1"))
                    .andExpect(status().isOk());

            verify(deptService).delete(1L);
        }
    }

    @Nested
    @DisplayName("用户部门关联测试")
    class UserDeptTests {

        @Test
        @DisplayName("获取用户部门列表")
        void shouldGetUserDepts() throws Exception {
            SecDept dept = new SecDept();
            dept.setId(1L);
            dept.setDeptName("研发部");

            when(deptService.getUserDepts("user-001", "tenant-001")).thenReturn(List.of(dept));

            mockMvc.perform(get("/depts/user/user-001")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("获取用户主部门")
        void shouldGetPrimaryDept() throws Exception {
            SecDept dept = new SecDept();
            dept.setId(1L);
            dept.setDeptName("研发部");

            when(deptService.getPrimaryDept("user-001", "tenant-001")).thenReturn(Optional.of(dept));

            mockMvc.perform(get("/depts/user/user-001/primary")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deptName").value("研发部"));
        }

        @Test
        @DisplayName("设置用户部门")
        void shouldSetUserDept() throws Exception {
            doNothing().when(deptService).setUserDept("user-001", 1L, "tenant-001", true);

            mockMvc.perform(post("/depts/user/user-001")
                    .param("deptId", "1")
                    .param("tenantId", "tenant-001")
                    .param("isPrimary", "true"))
                    .andExpect(status().isOk());

            verify(deptService).setUserDept("user-001", 1L, "tenant-001", true);
        }
    }
}