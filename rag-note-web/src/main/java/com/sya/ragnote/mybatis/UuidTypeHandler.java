package com.sya.ragnote.mybatis;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.util.UUID;

@MappedTypes(UUID.class)
public class UuidTypeHandler extends BaseTypeHandler<UUID> {
    @Override
    public void setNonNullParameter(
            PreparedStatement statement, int index, UUID value, JdbcType jdbcType)
            throws SQLException {
        statement.setObject(index, value);
    }

    @Override
    public UUID getNullableResult(ResultSet rs, String column) throws SQLException {
        return uuid(rs.getObject(column));
    }

    @Override
    public UUID getNullableResult(ResultSet rs, int column) throws SQLException {
        return uuid(rs.getObject(column));
    }

    @Override
    public UUID getNullableResult(CallableStatement cs, int column) throws SQLException {
        return uuid(cs.getObject(column));
    }

    private UUID uuid(Object value) {
        return value == null
                ? null
                : value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
    }
}
