package io.github.afgprojects.framework.ai.core.chat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 多 ChatClient 注册接口
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface ChatClientRegistry {

    void register(@NonNull String name, @NonNull AfgChatClient client);

    void remove(@NonNull String name);

    @NonNull
    Optional<AfgChatClient> get(@NonNull String name);

    @NonNull
    AfgChatClient getDefault();

    void setDefault(@NonNull String name);

    @NonNull
    List<String> listNames();
}