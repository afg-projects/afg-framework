package io.github.afgprojects.framework.data.jdbc.mapper;

import io.github.afgprojects.framework.data.core.mapper.ResultMapper;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ResultMapper 到 Spring RowMapper 的适配器
 *
 * @param <R> 结果类型
 */
public class ResultMapperAdapter<R> implements RowMapper<R> {

    private final ResultMapper<R> delegate;

    public ResultMapperAdapter(ResultMapper<R> delegate) {
        this.delegate = delegate;
    }

    @Override
    public R mapRow(ResultSet rs, int rowNum) throws SQLException {
        return delegate.map(rs, rowNum);
    }

    public static <R> RowMapper<R> adapt(ResultMapper<R> mapper) {
        if (mapper instanceof RowMapper<?> rowMapper) {
            @SuppressWarnings("unchecked")
            RowMapper<R> cast = (RowMapper<R>) rowMapper;
            return cast;
        }
        return new ResultMapperAdapter<>(mapper);
    }

    /**
     * 将 Spring RowMapper 适配为 ResultMapper
     *
     * @param rowMapper Spring RowMapper 实例
     * @return ResultMapper 实例
     */
    public static <R> ResultMapper<R> fromRowMapper(RowMapper<R> rowMapper) {
        if (rowMapper instanceof ResultMapper<?> resultMapper) {
            @SuppressWarnings("unchecked")
            ResultMapper<R> cast = (ResultMapper<R>) resultMapper;
            return cast;
        }
        return (rs, rowNum) -> {
            try {
                return rowMapper.mapRow(rs, rowNum);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to map row", e);
            }
        };
    }
}
