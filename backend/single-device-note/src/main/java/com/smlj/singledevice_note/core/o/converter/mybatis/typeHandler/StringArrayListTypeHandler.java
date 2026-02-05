package com.smlj.singledevice_note.core.o.converter.mybatis.typeHandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;

public class StringArrayListTypeHandler extends BaseTypeHandler<ArrayList<String>> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ArrayList<String> parameter, JdbcType jdbcType) throws SQLException {
        Connection conn = ps.getConnection();
        Array array = conn.createArrayOf("varchar", parameter.toArray());
        ps.setArray(i, array);
    }

    @Override
    public ArrayList<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toList(rs.getArray(columnName));
    }

    @Override
    public ArrayList<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toList(rs.getArray(columnIndex));
    }

    @Override
    public ArrayList<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toList(cs.getArray(columnIndex));
    }

    private ArrayList<String> toList(Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return new ArrayList<>();
        }
        String[] arr = (String[]) sqlArray.getArray();
        return new ArrayList<>(Arrays.asList(arr));
    }
}
