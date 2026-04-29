package io.github.afgprojects.framework.data.core.relation;

import io.github.afgprojects.framework.data.core.EntityProxy;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class EntityProxyRelationTest {

    @Test
    void shouldHaveWithAssociationMethod() throws NoSuchMethodException {
        Method method = EntityProxy.class.getMethod("withAssociation", String.class);
        assertThat(method).isNotNull();
        assertThat(method.getReturnType()).isEqualTo(EntityProxy.class);
    }

    @Test
    void shouldHaveWithAssociationsMethod() throws NoSuchMethodException {
        Method method = EntityProxy.class.getMethod("withAssociations", String[].class);
        assertThat(method).isNotNull();
        assertThat(method.getReturnType()).isEqualTo(EntityProxy.class);
    }

    @Test
    void shouldHaveFetchMethod() throws NoSuchMethodException {
        Method method = EntityProxy.class.getMethod("fetch", Object.class, String.class);
        assertThat(method).isNotNull();
    }

    @Test
    void shouldHaveFetchAllMethod() throws NoSuchMethodException {
        Method method = EntityProxy.class.getMethod("fetchAll", Iterable.class, String.class);
        assertThat(method).isNotNull();
        assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    void shouldHaveClearAssociationsMethod() throws NoSuchMethodException {
        Method method = EntityProxy.class.getMethod("clearAssociations");
        assertThat(method).isNotNull();
        assertThat(method.getReturnType()).isEqualTo(EntityProxy.class);
    }
}
