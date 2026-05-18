package io.github.afgprojects.framework.security.permission.adapter;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.security.permission.entity.SecCasbinRule;
import lombok.RequiredArgsConstructor;
import org.casbin.jcasbin.model.Assertion;
import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.persist.Adapter;
import org.casbin.jcasbin.persist.BatchAdapter;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 DataManager 的 Casbin 适配器
 *
 * <p>使用 DataManager 操作 Casbin 策略数据。
 */
@RequiredArgsConstructor
public class JdbcCasbinAdapter implements BatchAdapter {

    private final DataManager dataManager;

    @Override
    public void loadPolicy(@NonNull Model model) {
        List<SecCasbinRule> rules = dataManager.findAll(SecCasbinRule.class);
        for (SecCasbinRule rule : rules) {
            String line = rule.getPtype() + ", " + String.join(", ", rule.toPolicy());
            AdapterHelper.loadPolicyLine(line, model);
        }
    }

    @Override
    public void savePolicy(@NonNull Model model) {
        // 清空现有策略
        dataManager.entity(SecCasbinRule.class).query().list()
            .forEach(r -> dataManager.deleteById(SecCasbinRule.class, r.getId()));

        // 保存新策略
        List<SecCasbinRule> rules = new ArrayList<>();

        // 保存 p 策略
        if (model.model.containsKey("p")) {
            for (Map.Entry<String, Assertion> entry : model.model.get("p").entrySet()) {
                String ptype = entry.getKey();
                Assertion assertion = entry.getValue();
                for (List<String> policy : assertion.policy) {
                    SecCasbinRule rule = createRule(ptype, policy);
                    rules.add(rule);
                }
            }
        }

        // 保存 g 策略
        if (model.model.containsKey("g")) {
            for (Map.Entry<String, Assertion> entry : model.model.get("g").entrySet()) {
                String ptype = entry.getKey();
                Assertion assertion = entry.getValue();
                for (List<String> policy : assertion.policy) {
                    SecCasbinRule rule = createRule(ptype, policy);
                    rules.add(rule);
                }
            }
        }

        dataManager.saveAll(SecCasbinRule.class, rules);
    }

    private SecCasbinRule createRule(String ptype, List<String> policy) {
        SecCasbinRule rule = new SecCasbinRule();
        rule.setPtype(ptype);
        if (!policy.isEmpty()) rule.setV0(policy.get(0));
        if (policy.size() > 1) rule.setV1(policy.get(1));
        if (policy.size() > 2) rule.setV2(policy.get(2));
        if (policy.size() > 3) rule.setV3(policy.get(3));
        return rule;
    }

    @Override
    public void addPolicy(@NonNull String sec, @NonNull String ptype, @NonNull List<String> rule) {
        SecCasbinRule casbinRule = new SecCasbinRule();
        casbinRule.setPtype(ptype);
        if (!rule.isEmpty()) casbinRule.setV0(rule.get(0));
        if (rule.size() > 1) casbinRule.setV1(rule.get(1));
        if (rule.size() > 2) casbinRule.setV2(rule.get(2));
        if (rule.size() > 3) casbinRule.setV3(rule.get(3));

        dataManager.save(SecCasbinRule.class, casbinRule);
    }

    @Override
    public void removePolicy(@NonNull String sec, @NonNull String ptype, @NonNull List<String> rule) {
        List<SecCasbinRule> rules = dataManager.findAll(SecCasbinRule.class);
        for (SecCasbinRule r : rules) {
            if (matchesRule(r, ptype, rule)) {
                dataManager.deleteById(SecCasbinRule.class, r.getId());
            }
        }
    }

    @Override
    public void addPolicies(@NonNull String sec, @NonNull String ptype, @NonNull List<List<String>> rules) {
        for (List<String> rule : rules) {
            addPolicy(sec, ptype, rule);
        }
    }

    @Override
    public void removePolicies(@NonNull String sec, @NonNull String ptype, @NonNull List<List<String>> rules) {
        for (List<String> rule : rules) {
            removePolicy(sec, ptype, rule);
        }
    }

    @Override
    public void removeFilteredPolicy(@NonNull String sec, @NonNull String ptype, int fieldIndex, @NonNull String... fieldValues) {
        List<SecCasbinRule> rules = dataManager.findAll(SecCasbinRule.class);
        for (SecCasbinRule r : rules) {
            if (matchesFilteredRule(r, ptype, fieldIndex, fieldValues)) {
                dataManager.deleteById(SecCasbinRule.class, r.getId());
            }
        }
    }

    private boolean matchesRule(SecCasbinRule rule, String ptype, List<String> policy) {
        if (!ptype.equals(rule.getPtype())) return false;
        String[] rulePolicy = rule.toPolicy();
        if (policy.size() != rulePolicy.length) return false;
        for (int i = 0; i < policy.size(); i++) {
            if (!policy.get(i).equals(rulePolicy[i])) return false;
        }
        return true;
    }

    private boolean matchesFilteredRule(SecCasbinRule rule, String ptype, int fieldIndex, String... fieldValues) {
        if (!ptype.equals(rule.getPtype())) return false;
        String[] rulePolicy = rule.toPolicy();
        for (int i = 0; i < fieldValues.length; i++) {
            int idx = fieldIndex + i;
            if (idx >= rulePolicy.length) return false;
            if (!fieldValues[i].equals(rulePolicy[idx])) return false;
        }
        return true;
    }

    /**
     * 辅助类：加载策略行
     */
    private static class AdapterHelper {
        static void loadPolicyLine(String line, Model model) {
            if (line.isEmpty() || line.startsWith("#")) return;
            String[] tokens = line.split(",\\s*");
            if (tokens.length < 2) return;
            String sec = tokens[0].equals("p") ? "p" : "g";
            String ptype = tokens[0];
            List<String> policy = new ArrayList<>();
            for (int i = 1; i < tokens.length; i++) {
                policy.add(tokens[i]);
            }
            if (model.model.containsKey(sec) && model.model.get(sec).containsKey(ptype)) {
                model.model.get(sec).get(ptype).policy.add(policy);
            }
        }
    }
}
