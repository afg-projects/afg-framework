package io.github.afgprojects.framework.data.core.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ResultMapper<R> {
    R map(ResultSet rs, int rowNum) throws SQLException;
}
