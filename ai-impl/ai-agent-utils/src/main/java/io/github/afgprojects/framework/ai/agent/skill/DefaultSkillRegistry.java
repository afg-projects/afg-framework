package io.github.afgprojects.framework.ai.agent.skill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认 Skill 注册表实现
 *
 * <p>支持从 YAML 文件加载 Skill 定义。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultSkillRegistry implements SkillRegistry {

    private static final Logger log = LoggerFactory.getLogger(DefaultSkillRegistry.class);

    private final Map<String, SkillDefinition> skills = new ConcurrentHashMap<>();
    private final ObjectMapper yamlMapper;

    public DefaultSkillRegistry() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    @Override
    public void register(@NonNull SkillDefinition definition) {
        String name = definition.name();
        if (skills.containsKey(name)) {
            log.warn("Skill already registered: {}, replacing", name);
        }
        skills.put(name, definition);
        log.info("Registered skill: {}", name);
    }

    @Override
    public boolean unregister(@NonNull String name) {
        boolean removed = skills.remove(name) != null;
        if (removed) {
            log.info("Unregistered skill: {}", name);
        }
        return removed;
    }

    @Override
    @NonNull
    public Optional<SkillDefinition> get(@NonNull String name) {
        return Optional.ofNullable(skills.get(name));
    }

    @Override
    public boolean exists(@NonNull String name) {
        return skills.containsKey(name);
    }

    @Override
    @NonNull
    public List<SkillDefinition> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(skills.values()));
    }

    @Override
    public int size() {
        return skills.size();
    }

    @Override
    public void clear() {
        skills.clear();
        log.info("Cleared all skills");
    }

    /**
     * 从 YAML 文件加载 Skill
     *
     * @param path YAML 文件路径
     * @return Skill 定义
     * @throws IOException 加载失败
     */
    @NonNull
    public SkillDefinition loadFromYaml(@NonNull Path path) throws IOException {
        log.debug("Loading skill from YAML: {}", path);

        String content = Files.readString(path);
        SkillDefinition definition = parseYaml(content);

        register(definition);
        return definition;
    }

    /**
     * 从输入流加载 Skill
     *
     * @param inputStream 输入流
     * @return Skill 定义
     * @throws IOException 加载失败
     */
    @NonNull
    public SkillDefinition loadFromYaml(@NonNull InputStream inputStream) throws IOException {
        log.debug("Loading skill from input stream");

        SkillDefinition definition = yamlMapper.readValue(inputStream, SkillDefinition.class);
        register(definition);
        return definition;
    }

    /**
     * 解析 YAML 内容
     *
     * @param yamlContent YAML 内容
     * @return Skill 定义
     * @throws IOException 解析失败
     */
    @NonNull
    public SkillDefinition parseYaml(@NonNull String yamlContent) throws IOException {
        return yamlMapper.readValue(yamlContent, SkillDefinition.class);
    }

    /**
     * 从目录批量加载 Skills
     *
     * @param directory 目录路径
     * @return 加载的 Skill 数量
     */
    public int loadFromDirectory(@NonNull Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            log.warn("Not a directory: {}", directory);
            return 0;
        }

        int count = 0;
        try (var stream = Files.list(directory)) {
            for (Path file : stream.toList()) {
                if (file.toString().endsWith(".yaml") || file.toString().endsWith(".yml")) {
                    try {
                        loadFromYaml(file);
                        count++;
                    } catch (Exception e) {
                        log.error("Failed to load skill from: {}", file, e);
                    }
                }
            }
        }

        log.info("Loaded {} skills from directory: {}", count, directory);
        return count;
    }
}