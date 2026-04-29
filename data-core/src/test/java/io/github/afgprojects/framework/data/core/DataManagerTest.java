package io.github.afgprojects.framework.data.core;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataManagerTest {

    @Test
    void shouldDefineDataManagerInterface() {
        assertThat(DataManager.class).isInterface();
    }

    @Test
    void shouldDefineEntityProxyInterface() {
        assertThat(EntityProxy.class).isInterface();
    }

    @Test
    void shouldBuildConditionWithFluentApi() {
        Condition condition = Conditions.builder()
            .eq("name", "test")
            .like("email", "example")
            .build();

        assertThat(condition.isEmpty()).isFalse();
    }

    @Test
    void shouldBuildDataScope() {
        DataScope scope = DataScope.of("sys_user", "dept_id", DataScopeType.DEPT);
        assertThat(scope.table()).isEqualTo("sys_user");
    }

    @Test
    void shouldBuildPageRequest() {
        PageRequest request = PageRequest.of(1, 10);
        assertThat(request.page()).isEqualTo(1);
        assertThat(request.size()).isEqualTo(10);
    }
}