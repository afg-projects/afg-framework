package io.github.afgprojects.framework.core.datasource.lb;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NonNull;

/**
 * 轮询策略
 *
 * <p>按顺序依次选择数据源
 *
 * @since 1.0.0
 */
public class RoundRobinStrategy implements LoadBalanceStrategy {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    @NonNull
    public String select(@NonNull List<String> candidates) {
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("Candidates list cannot be empty");
        }
        if (candidates.size() == 1) {
            return candidates.getFirst();
        }
        int index = Math.abs(counter.getAndIncrement() % candidates.size());
        return candidates.get(index);
    }

    @Override
    @NonNull
    public String getName() {
        return "ROUND_ROBIN";
    }
}
