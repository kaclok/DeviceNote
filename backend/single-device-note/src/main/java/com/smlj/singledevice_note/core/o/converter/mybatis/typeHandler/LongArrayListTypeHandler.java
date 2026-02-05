package com.smlj.singledevice_note.core.o.converter.mybatis.typeHandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;

public class LongArrayListTypeHandler extends BaseTypeHandler<ArrayList<Long>> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ArrayList<Long> parameter, JdbcType jdbcType) throws SQLException {
        Connection conn = ps.getConnection();
        Array array = conn.createArrayOf("int8", parameter.toArray());
        ps.setArray(i, array);
    }

    @Override
    public ArrayList<Long> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toList(rs.getArray(columnName));
    }

    @Override
    public ArrayList<Long> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toList(rs.getArray(columnIndex));
    }

    @Override
    public ArrayList<Long> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toList(cs.getArray(columnIndex));
    }

    private ArrayList<Long> toList(Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return new ArrayList<>();
        }
        Long[] arr = (Long[]) sqlArray.getArray();
        return new ArrayList<>(Arrays.asList(arr));
    }
}
