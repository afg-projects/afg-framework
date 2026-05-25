package io.github.afgprojects.framework.data.core.mapper.handlers;

import io.github.afgprojects.framework.data.core.mapper.TypeHandler;
import java.sql.Blob;
import java.sql.Clob;

public class BlobTypeHandler implements TypeHandler<byte[]> {
    @Override public Class<byte[]> getType() { return byte[].class; }
    @Override public int priority() { return 10; }

    @Override
    public byte[] convert(Object value, Class<byte[]> targetType) {
        try {
            if (value instanceof Blob blob) return blob.getBytes(1, (int) blob.length());
            if (value instanceof Clob clob) return clob.getSubString(1, (int) clob.length()).getBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert LOB to byte[]", e);
        }
        return null;
    }
}