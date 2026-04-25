package com.boot.study.config.mybatis;

import com.boot.study.enums.BaseDbEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 通用的 {@link BaseDbEnum} TypeHandler：
 * - 写入时：将枚举的 {@code getCode()} 写入数据库（可以是 String / Integer / Byte 等）
 * - 读取时：根据数据库中的值，反查对应的枚举常量
 *
 * 全局配置为默认的枚举处理器后，可让所有实现了 BaseDbEnum 的枚举按自定义 code 存储。
 */
public class BaseDbEnumTypeHandler<E extends Enum<E> & BaseDbEnum<?>> extends BaseTypeHandler<E> {

    private final Class<E> type;
    private final E[] enums;

    public BaseDbEnumTypeHandler(Class<E> type) {
        if (type == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        this.type = type;
        this.enums = type.getEnumConstants();
        if (this.enums == null) {
            throw new IllegalArgumentException(type.getSimpleName() + " does not represent an enum type.");
        }
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        Object code = parameter.getCode();
        if (jdbcType == null) {
            ps.setObject(i, code);
        } else {
            ps.setObject(i, code, jdbcType.TYPE_CODE);
        }
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        return toEnum(value);
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Object value = rs.getObject(columnIndex);
        return toEnum(value);
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Object value = cs.getObject(columnIndex);
        return toEnum(value);
    }

    /**
     * 根据数据库中的值，匹配对应的枚举常量（按 BaseDbEnum.getCode() 比较）
     */
    private E toEnum(Object dbValue) {
        if (dbValue == null) {
            return null;
        }
        for (E e : enums) {
            Object code = e.getCode();
            if (code == null) {
                continue;
            }
            // 尝试宽松匹配：数值 & 字符串都转成字符串比较，避免 Integer / Long / Byte 类型不一致的问题
            if (String.valueOf(code).equals(String.valueOf(dbValue))) {
                return e;
            }
        }
        // 找不到匹配的就返回 null，避免报错，让上层自己处理
        return null;
    }
}


