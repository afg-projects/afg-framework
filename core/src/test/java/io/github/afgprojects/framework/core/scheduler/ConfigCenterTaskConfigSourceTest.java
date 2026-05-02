package io.github.afgprojects.framework.core.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.afgprojects.framework.core.api.scheduler.DynamicTaskConfigSource;
import io.github.afgprojects.framework.core.api.scheduler.TaskDefinition;
import io.github.afgprojects.framework.core.config.AfgConfigRegistry;
import io.github.afgprojects.framework.core.config.ConfigChangeEvent;

/**
 * ConfigCenterTaskConfigSource 测试
 */
@DisplayName("ConfigCenterTaskConfigSource 测试")
@ExtendWith(MockitoExtension.class)
class ConfigCenterTaskConfigSourceTest {

    @Mock
    private AfgConfigRegistry configRegistry;

    private ConfigCenterTaskConfigSource configSource;

    @BeforeEach
    void setUp() {
        doNothing().when(configRegistry).addListener(anyString(), any());
        configSource = new ConfigCenterTaskConfigSource(configRegistry);
    }

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("应该正确初始化")
        void shouldInitialize() {
            assertThat(configSource).isNotNull();
            assertThat(configSource.getName()).isEqualTo("config-center");
        }

        @Test
        @DisplayName("应该支持自定义配置前缀")
        void shouldSupportCustomPrefix() {
            ConfigCenterTaskConfigSource customSource = new ConfigCenterTaskConfigSource(configRegistry, "custom.prefix");
            assertThat(customSource).isNotNull();
        }
    }

    @Nested
    @DisplayName("loadAll 测试")
    class LoadAllTests {

        @Test
        @DisplayName("应该加载所有任务定义")
        void shouldLoadAllTaskDefinitions() {
            Map<String, Object> allConfigs = new HashMap<>();
            Map<String, Object> taskConfig = new HashMap<>();
            taskConfig.put("cron", "0 * * * * *");
            taskConfig.put("enabled", true);
            allConfigs.put("afg.tasks.task-1", taskConfig);

            when(configRegistry.getAllConfigs()).thenReturn(allConfigs);

            List<TaskDefinition> definitions = configSource.loadAll();

            assertThat(definitions).hasSize(1);
            assertThat(definitions.get(0).taskId()).isEqualTo("task-1");
        }
    }

    @Nested
    @DisplayName("load 测试")
    class LoadTests {

        @Test
        @DisplayName("应该加载指定任务定义")
        void shouldLoadTaskDefinition() {
            Map<String, Object> taskConfig = new HashMap<>();
            taskConfig.put("cron", "0 * * * * *");
            taskConfig.put("enabled", true);

            when(configRegistry.getConfig("afg.tasks.task-1")).thenReturn(taskConfig);

            Optional<TaskDefinition> result = configSource.load("task-1");

            assertThat(result).isPresent();
            assertThat(result.get().taskId()).isEqualTo("task-1");
        }

        @Test
        @DisplayName("任务不存在应该返回空")
        void shouldReturnEmptyWhenTaskNotFound() {
            when(configRegistry.getConfig(anyString())).thenReturn(null);

            Optional<TaskDefinition> result = configSource.load("non-existent");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("loadByGroup 测试")
    class LoadByGroupTests {

        @Test
        @DisplayName("应该按组加载任务定义")
        void shouldLoadByGroup() {
            Map<String, Object> allConfigs = new HashMap<>();
            Map<String, Object> taskConfig1 = new HashMap<>();
            taskConfig1.put("group", "group-a");
            taskConfig1.put("cron", "0 * * * * *");
            taskConfig1.put("enabled", true);
            Map<String, Object> taskConfig2 = new HashMap<>();
            taskConfig2.put("group", "group-b");
            taskConfig2.put("cron", "0 0 * * * *");
            taskConfig2.put("enabled", true);

            allConfigs.put("afg.tasks.task-1", taskConfig1);
            allConfigs.put("afg.tasks.task-2", taskConfig2);

            when(configRegistry.getAllConfigs()).thenReturn(allConfigs);

            List<TaskDefinition> result = configSource.loadByGroup("group-a");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).taskId()).isEqualTo("task-1");
        }
    }

    @Nested
    @DisplayName("save 测试")
    class SaveTests {

        @Test
        @DisplayName("应该保存任务定义")
        void shouldSaveTaskDefinition() {
            TaskDefinition definition = TaskDefinition.ofCron("task-1", "0 * * * * *");

            configSource.save(definition);

            verify(configRegistry).register(anyString(), any());
        }
    }

    @Nested
    @DisplayName("delete 测试")
    class DeleteTests {

        @Test
        @DisplayName("应该删除任务定义")
        void shouldDeleteTaskDefinition() {
            configSource.delete("task-1");

            verify(configRegistry).unregister("afg.tasks.task-1");
        }
    }

    @Nested
    @DisplayName("监听器测试")
    class ListenerTests {

        @Test
        @DisplayName("应该添加监听器")
        void shouldAddListener() {
            DynamicTaskConfigSource.ConfigChangeListener listener = mock(DynamicTaskConfigSource.ConfigChangeListener.class);

            configSource.addChangeListener(listener);

            // 验证监听器被添加
            configSource.refresh();
            verify(listener).onConfigChange(DynamicTaskConfigSource.ConfigChangeListener.ConfigChangeEvent.REFRESHED, null);
        }

        @Test
        @DisplayName("应该移除监听器")
        void shouldRemoveListener() {
            DynamicTaskConfigSource.ConfigChangeListener listener = mock(DynamicTaskConfigSource.ConfigChangeListener.class);

            configSource.addChangeListener(listener);
            configSource.removeChangeListener(listener);

            configSource.refresh();
            verify(listener, never()).onConfigChange(any(), any());
        }
    }

    @Nested
    @DisplayName("refresh 测试")
    class RefreshTests {

        @Test
        @DisplayName("应该刷新配置源")
        void shouldRefreshConfigSource() {
            DynamicTaskConfigSource.ConfigChangeListener listener = mock(DynamicTaskConfigSource.ConfigChangeListener.class);
            configSource.addChangeListener(listener);

            configSource.refresh();

            verify(listener).onConfigChange(DynamicTaskConfigSource.ConfigChangeListener.ConfigChangeEvent.REFRESHED, null);
        }
    }
}
