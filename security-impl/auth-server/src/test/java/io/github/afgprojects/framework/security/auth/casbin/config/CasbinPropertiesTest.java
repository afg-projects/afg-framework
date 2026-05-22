package io.github.afgprojects.framework.security.auth.casbin.config;

import io.github.afgprojects.framework.security.auth.autoconfigure.AuthSecurityProperties;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * AuthSecurityProperties.CasbinConfig 测试
 */
class CasbinPropertiesTest {

    @Test
    void should_haveDefaultValues() {
        AuthSecurityProperties.CasbinConfig properties = new AuthSecurityProperties.CasbinConfig();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getModelType()).isEqualTo("rbac-domain");
        assertThat(properties.getPolicyAdapterType()).isEqualTo("memory");
        assertThat(properties.isAutoSave()).isTrue();
        assertThat(properties.isAutoBuildRoleLinks()).isTrue();
    }

    @Test
    void should_setAndGetValues() {
        AuthSecurityProperties.CasbinConfig properties = new AuthSecurityProperties.CasbinConfig();

        properties.setEnabled(false);
        properties.setModelType("acl");
        properties.setPolicyAdapterType("jdbc");
        properties.setAutoSave(false);
        properties.setAutoBuildRoleLinks(false);
        properties.setModelText("[request_definition]\nr = sub, obj, act");

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getModelType()).isEqualTo("acl");
        assertThat(properties.getPolicyAdapterType()).isEqualTo("jdbc");
        assertThat(properties.isAutoSave()).isFalse();
        assertThat(properties.isAutoBuildRoleLinks()).isFalse();
        assertThat(properties.getModelText()).contains("[request_definition]");
    }

    @Test
    void should_haveConfigurationPropertiesAnnotation() {
        // CasbinConfig 是 AuthSecurityProperties 的嵌套类，没有独立的 @ConfigurationProperties 注解
        // 配置前缀由父类 AuthSecurityProperties 的 @ConfigurationProperties(prefix = "afg.security.auth-server") 定义
        // CasbinConfig 的配置路径是 afg.security.auth-server.casbin
        org.springframework.boot.context.properties.ConfigurationProperties annotation =
                AnnotationUtils.findAnnotation(AuthSecurityProperties.class,
                        org.springframework.boot.context.properties.ConfigurationProperties.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.prefix()).isEqualTo("afg.security.auth-server");
    }

    @Test
    void should_returnDefaultModel_when_modelTextIsNull() {
        AuthSecurityProperties.CasbinConfig properties = new AuthSecurityProperties.CasbinConfig();
        properties.setModelText(null);

        String modelText = properties.getDefaultModelText();

        assertThat(modelText).contains("[request_definition]");
        assertThat(modelText).contains("r = sub, dom, obj, act");
        assertThat(modelText).contains("[policy_definition]");
        assertThat(modelText).contains("p = sub, dom, obj, act");
        assertThat(modelText).contains("[role_definition]");
        assertThat(modelText).contains("g = _, _, _");
        assertThat(modelText).contains("[policy_effect]");
        assertThat(modelText).contains("e = some(where (p.eft == allow))");
        assertThat(modelText).contains("[matchers]");
        assertThat(modelText).contains("g(r.sub, r.dom, p.sub) && r.dom == p.dom && keyMatch2(r.obj, p.obj) && r.act == p.act");
    }
}
