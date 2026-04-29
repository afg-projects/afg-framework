package io.github.afgprojects.framework.core.module;

import java.util.List;
import java.util.Objects;

/**
 * 模块定义
 */
@SuppressWarnings("PMD.UnusedAssignment")
public record ModuleDefinition(String id, String name, List<String> dependencies, AfgModule moduleInstance) {

    // NOPMD - record 紧凑构造器中重新赋值参数是正常行为
    public ModuleDefinition {
        dependencies = dependencies != null ? List.copyOf(dependencies) : List.of();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModuleDefinition that = (ModuleDefinition) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String id;
        private String name;
        private List<String> dependencies;
        private AfgModule moduleInstance;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder dependencies(List<String> dependencies) {
            this.dependencies = dependencies;
            return this;
        }

        public Builder moduleInstance(AfgModule moduleInstance) {
            this.moduleInstance = moduleInstance;
            return this;
        }

        public ModuleDefinition build() {
            return new ModuleDefinition(id, name, dependencies, moduleInstance);
        }
    }
}
