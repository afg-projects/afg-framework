package io.github.afgprojects.framework.ai.core.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public interface ModelInfo {

    @NonNull
    String name();

    @NonNull
    ModelType type();

    @Nullable
    String provider();

    @Nullable
    String displayName();

    @Nullable
    Integer contextWindow();

    @Nullable
    Integer dimensions();

    @Nullable
    Integer maxOutputTokens();

    @Nullable
    Double inputPricePer1kTokens();

    @Nullable
    Double outputPricePer1kTokens();

    boolean available();

    @NonNull
    Map<String, Object> capabilities();
}