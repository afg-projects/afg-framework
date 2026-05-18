package io.github.afgprojects.framework.security.permission.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * Casbin 策略规则实体
 *
 * <p>支持 RBAC with domains 模型：
 * <ul>
 *   <li>策略（p）：ptype=p, v0=sub, v1=dom, v2=obj, v3=act</li>
 *   <li>角色（g）：ptype=g, v0=sub, v1=dom, v2=role</li>
 * </ul>
 */
@Getter
@Setter
@AfEntity
@Table(name = "sec_casbin_rule")
public class SecCasbinRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ptype", length = 10)
    private String ptype;

    @Column(name = "v0", length = 100)
    @Nullable
    private String v0;

    @Column(name = "v1", length = 100)
    @Nullable
    private String v1;

    @Column(name = "v2", length = 100)
    @Nullable
    private String v2;

    @Column(name = "v3", length = 100)
    @Nullable
    private String v3;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    /**
     * 创建策略规则
     */
    public static SecCasbinRule createPolicy(String sub, String dom, String obj, String act) {
        SecCasbinRule rule = new SecCasbinRule();
        rule.setPtype("p");
        rule.setV0(sub);
        rule.setV1(dom);
        rule.setV2(obj);
        rule.setV3(act);
        rule.setTenantId(dom);
        return rule;
    }

    /**
     * 创建角色继承规则
     */
    public static SecCasbinRule createRole(String sub, String dom, String role) {
        SecCasbinRule rule = new SecCasbinRule();
        rule.setPtype("g");
        rule.setV0(sub);
        rule.setV1(dom);
        rule.setV2(role);
        rule.setTenantId(dom);
        return rule;
    }

    /**
     * 转换为 Casbin 策略数组
     */
    public String[] toPolicy() {
        if ("p".equals(ptype)) {
            return new String[]{v0, v1, v2, v3};
        } else if ("g".equals(ptype)) {
            return new String[]{v0, v1, v2};
        }
        return new String[0];
    }
}
