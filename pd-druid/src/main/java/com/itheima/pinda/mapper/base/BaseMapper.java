package com.itheima.pinda.mapper.base;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class BaseMapper {

    @Autowired
    private DataSource dataSource;

    // ==================== 原有方法：修复 JDBC 资源泄漏（try-with-resources） ====================

    public List<Map> list(String sql) {
        List<Map> resultList = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData rsmd = rs.getMetaData();
            while (rs.next()) {
                Map map = new HashMap();
                for (int i = 0; i < rsmd.getColumnCount(); i++) {
                    String columnName = rsmd.getColumnName(i + 1);
                    map.put(columnName, rs.getObject(columnName));
                }
                resultList.add(map);
            }
        } catch (Exception e) {
            log.error("查询执行失败: sql={}", sql, e);
        }
        return resultList.isEmpty() ? null : resultList;
    }

    public Map listFirst(String sql) {
        try (Connection connection = dataSource.getConnection();
             Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData rsmd = rs.getMetaData();
            if (rs.next()) {
                Map map = new HashMap();
                for (int i = 0; i < rsmd.getColumnCount(); i++) {
                    String columnName = rsmd.getColumnName(i + 1);
                    map.put(columnName, rs.getObject(columnName));
                }
                return map;
            }
        } catch (Exception e) {
            log.error("查询执行失败: sql={}", sql, e);
        }
        return null;
    }

    // ==================== 新增：参数化查询方法，防止 SQL 注入 ====================

    /**
     * 参数化查询（PreparedStatement）
     *
     * @param sql    SQL 模板，使用 ? 作为占位符
     * @param params 按顺序排列的参数值列表
     * @return 查询结果列表
     */
    public List<Map<String, Object>> list(String sql, List<Object> params) {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement pst = connection.prepareStatement(sql)) {
            if (params != null) {
                int index = 1;
                for (Object value : params) {
                    pst.setObject(index++, value);
                }
            }
            try (ResultSet rs = pst.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnLabel(i), rs.getObject(i));
                    }
                    result.add(row);
                }
            }
        } catch (Exception e) {
            log.error("查询执行失败: sql={}", sql, e);
        }
        return result;
    }

    /**
     * 参数化查询，返回第一条记录
     */
    public Map<String, Object> listFirst(String sql, List<Object> params) {
        List<Map<String, Object>> list = list(sql, params);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 参数化更新（INSERT/UPDATE/DELETE）
     */
    public int update(String sql, List<Object> params) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement pst = connection.prepareStatement(sql)) {
            if (params != null) {
                int index = 1;
                for (Object value : params) {
                    pst.setObject(index++, value);
                }
            }
            return pst.executeUpdate();
        } catch (Exception e) {
            log.error("更新执行失败: sql={}", sql, e);
            return 0;
        }
    }
}
