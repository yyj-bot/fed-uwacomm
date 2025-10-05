package com.feduwacomm.config;

import com.feduwacomm.enums.DataStatus;
import com.feduwacomm.enums.DataType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 基于code的枚举类型处理器
 * 支持DataType和DataStatus枚举
 */
@MappedTypes({DataType.class, DataStatus.class})
@MappedJdbcTypes(JdbcType.VARCHAR)
public class EnumCodeTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {

    private final Class<E> type;

    public EnumCodeTypeHandler(Class<E> type) {
        if (type == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        this.type = type;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType)
            throws SQLException {
        // 根据枚举类型调用相应的getCode方法
        String code = getEnumCode(parameter);
        ps.setString(i, code);
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String code = rs.getString(columnName);
        return code == null ? null : getEnumFromCode(code);
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String code = rs.getString(columnIndex);
        return code == null ? null : getEnumFromCode(code);
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String code = cs.getString(columnIndex);
        return code == null ? null : getEnumFromCode(code);
    }

    /**
     * 获取枚举的code值
     */
    private String getEnumCode(E enumValue) {
        if (enumValue instanceof DataType) {
            return ((DataType) enumValue).getCode();
        } else if (enumValue instanceof DataStatus) {
            return ((DataStatus) enumValue).getCode();
        }
        // 默认使用枚举的name()
        return enumValue.name();
    }

    /**
     * 根据code获取枚举值
     */
    @SuppressWarnings("unchecked")
    private E getEnumFromCode(String code) {
        if (type == DataType.class) {
            return (E) DataType.fromCode(code);
        } else if (type == DataStatus.class) {
            return (E) DataStatus.fromCode(code);
        }

        // 默认使用枚举的valueOf方法
        try {
            return Enum.valueOf(type, code);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cannot convert " + code + " to " + type.getSimpleName());
        }
    }
}