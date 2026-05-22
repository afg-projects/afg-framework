package io.github.afgprojects.framework.security.auth.casbin.model;

import java.util.Arrays;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Casbin 策略规则
 *
 * <p>表示 Casbin 的一条策略规则，支持策略（p）和角色继承（g）两种类型。
 *
 * <p>RBAC with domains 模型：
 * <ul>
 *   <li>策略（p）：sub, dom, obj, act</li>
 *   <li>角色（g）：sub, dom, role</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class CasbinRule {

    /**
     * 规则类型：p（策略）或 g（角色继承）
     */
    private String ptype;

    /**
     * 字段值：sub（主体）
     */
    @Nullable
    private String v0;

    /**
     * 字段值：dom（域/租户）
     */
    @Nullable
    private String v1;

    /**
     * 字段值：obj（对象/资源）或 role（角色）
     */
    @Nullable
    private String v2;

    /**
     * 字段值：act（动作）
     */
    @Nullable
    private String v3;

    /**
     * 默认构造函数
     */
    public CasbinRule() {
    }

    /**
     * 构造函数
     *
     * @param ptype 规则类型
     * @param v0    字段0
     * @param v1    字段1
     * @param v2    字段2
     * @param v3    字段3
     */
    public CasbinRule(String ptype, @Nullable String v0, @Nullable String v1,
                      @Nullable String v2, @Nullable String v3) {
        this.ptype = ptype;
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
    }

    /**
     * 创建策略规则
     *
     * @param sub 主体（用户）
     * @param dom 域（租户）
     * @param obj 对象（资源）
     * @param act 动作
     * @return 策略规则
     */
    public static CasbinRule createPolicy(String sub, String dom, String obj, String act) {
        return new CasbinRule("p", sub, dom, obj, act);
    }

    /**
     * 创建角色继承规则
     *
     * @param sub  主体（用户）
     * @param dom  域（租户）
     * @param role 角色
     * @return 角色继承规则
     */
    public static CasbinRule createRole(String sub, String dom, String role) {
        return new CasbinRule("g", sub, dom, role, null);
    }

    /**
     * 转换为 Casbin 策略数组
     *
     * @return 策略数组
     */
    public String[] toPolicy() {
        if ("p".equals(ptype)) {
            return new String[]{v0, v1, v2, v3};
        } else if ("g".equals(ptype)) {
            return new String[]{v0, v1, v2};
        }
        return new String[0];
    }

    // Getters and Setters

    public String getPtype() {
        return ptype;
    }

    public void setPtype(String ptype) {
        this.ptype = ptype;
    }

    @Nullable
    public String getV0() {
        return v0;
    }

    public void setV0(@Nullable String v0) {
        this.v0 = v0;
    }

    @Nullable
    public String getV1() {
        return v1;
    }

    public void setV1(@Nullable String v1) {
        this.v1 = v1;
    }

    @Nullable
    public String getV2() {
        return v2;
    }

    public void setV2(@Nullable String v2) {
        this.v2 = v2;
    }

    @Nullable
    public String getV3() {
        return v3;
    }

    public void setV3(@Nullable String v3) {
        this.v3 = v3;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CasbinRule that = (CasbinRule) o;
        return Objects.equals(ptype, that.ptype) &&
                Objects.equals(v0, that.v0) &&
                Objects.equals(v1, that.v1) &&
                Objects.equals(v2, that.v2) &&
                Objects.equals(v3, that.v3);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ptype, v0, v1, v2, v3);
    }

    @Override
    public String toString() {
        if ("p".equals(ptype)) {
            return String.format("%s, %s, %s, %s, %s", ptype, v0, v1, v2, v3);
        } else if ("g".equals(ptype)) {
            return String.format("%s, %s, %s, %s", ptype, v0, v1, v2);
        }
        return ptype;
    }
}
