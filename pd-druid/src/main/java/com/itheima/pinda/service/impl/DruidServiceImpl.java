package com.itheima.pinda.service.impl;

import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.mapper.MessageMapper;
import com.itheima.pinda.service.DruidService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service("druidService")
public class DruidServiceImpl implements DruidService {

    @Autowired
    private MessageMapper messageMapper;

    // ==================== ORDER BY 白名单，防止排序字段注入 ====================
    private static final Map<String, String> ALLOWED_ORDER_FIELDS;

    static {
        Map<String, String> fields = new HashMap<>();
        fields.put("currentTime", "currentTime");
        fields.put("__time", "__time");
        fields.put("name", "name");
        fields.put("phone", "phone");
        fields.put("licensePlate", "licensePlate");
        fields.put("businessId", "businessId");
        ALLOWED_ORDER_FIELDS = fields;
    }

    // ==================== 查询接口实现 ====================

    @Override
    public Result queryAllTruckLast(Map<String, Object> params) {
        params.put("type", "truck");
        String sqlTemplate = "SELECT CONCAT(businessId,'#','" + params.get("type") + "','#',MAX(currentTime)) as id " +
                             "FROM tms_order_location WHERE 1 = 1";
        WhereResult whereResult = whereSql(params);
        sqlTemplate += whereResult.sql;
        sqlTemplate += " GROUP BY businessId";

        List<Map> idMaps = messageMapper.list(sqlTemplate, whereResult.values);

        if (CollectionUtils.isEmpty(idMaps)) {
            return Result.ok().put("data", new ArrayList<>());
        }

        List<String> ids = idMaps.stream()
                .map(m -> (String) m.get("id"))
                .collect(Collectors.toList());
        String inClause = ids.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));
        String querySqlTemplate = "SELECT currentTime, name, phone, licensePlate, businessId, lat, lng " +
                                  "FROM tms_order_location WHERE id IN (" + inClause + ")";

        List<Object> inParams = new ArrayList<>(ids);
        List<Map> messageEntities = messageMapper.list(querySqlTemplate, inParams);

        return Result.ok().put("data", messageEntities);
    }

    @Override
    public Result queryOneTruck(Map<String, Object> params) {
        params.put("type", "truck");
        params.put("order", "__time ASC");
        params.put("limit", 99);

        // ---- 第一条查询：获取基础信息（不需要 LIMIT）----
        String baseSqlTemplate = "SELECT name, phone, licensePlate, businessId, __time " +
                                 "FROM tms_order_location WHERE 1 = 1";
        WhereResult whereResult = whereSql(params);
        OrderResult orderResult = orderSql(params);
        baseSqlTemplate += whereResult.sql + orderResult.sql;

        Map baseMap = messageMapper.listFirst(baseSqlTemplate, whereResult.values);
        if (CollectionUtils.isEmpty(baseMap)) {
            return Result.ok();
        }

        // ---- 第二条查询：获取经纬度（带 LIMIT）----
        String latLngSqlTemplate = "SELECT lat, lng, __time " +
                                   "FROM tms_order_location WHERE 1 = 1";
        WhereResult whereResult2 = whereSql(params);
        OrderResult orderResult2 = orderSql(params);
        // 单独处理 limit，避免多次调用问题
        LimitResult limitResult = limitSql(params);
        latLngSqlTemplate += whereResult2.sql + orderResult2.sql + limitResult.sql;

        // 合并 WHERE 参数 + LIMIT 参数
        List<Object> allParams = new ArrayList<>(whereResult2.values);
        allParams.addAll(limitResult.values);

        List<Map> messageEntities = messageMapper.list(latLngSqlTemplate, allParams);

        baseMap.put("polyLinePath", messageEntities);
        return Result.ok().put("data", baseMap);
    }

    @Override
    public Result queryAll(List<Map<String, Object>> paramsList) {
        List<Map> baseMapList = new ArrayList<>();
        List<Map> linePointList = new ArrayList<>();

        for (Map<String, Object> param : paramsList) {
            param.put("order", "__time ASC");

            // ---- 第一条查询：获取基础信息（不带 LIMIT）----
            String baseSqlTemplate = "SELECT name, phone, licensePlate, businessId, __time " +
                                     "FROM tms_order_location WHERE 1 = 1";
            WhereResult whereResult = whereSql(param);
            OrderResult orderResult = orderSql(param);
            baseSqlTemplate += whereResult.sql + orderResult.sql;

            Map baseMap = messageMapper.listFirst(baseSqlTemplate, whereResult.values);
            if (CollectionUtils.isEmpty(baseMap)) {
                continue;
            }

            // ---- 第二条查询：获取经纬度（带 LIMIT）----
            String latLngSqlTemplate = "SELECT lat, lng, __time " +
                                       "FROM tms_order_location WHERE 1 = 1";
            WhereResult whereResult2 = whereSql(param);
            OrderResult orderResult2 = orderSql(param);
            LimitResult limitResult = limitSql(param);
            latLngSqlTemplate += whereResult2.sql + orderResult2.sql + limitResult.sql;

            List<Object> allParams = new ArrayList<>(whereResult2.values);
            allParams.addAll(limitResult.values);

            List<Map> messageEntities = messageMapper.list(latLngSqlTemplate, allParams);
            linePointList.addAll(messageEntities);
            baseMap.put("list", messageEntities);
            baseMapList.add(baseMap);
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("polyLinePath", linePointList);
        resultMap.put("pointsArr", new ArrayList<>());
        return Result.ok().put("data", resultMap);
    }

    // ==================== SQL 构建方法（参数化，防止 SQL 注入） ====================

    /**
     * 构建参数化 WHERE 子句
     *
     * @return WhereResult 包含 SQL 模板（? 占位符）和按顺序排列的参数值列表
     */
    private WhereResult whereSql(Map<String, Object> params) {
        // 使用副本，避免修改调用方的 map
        Map<String, Object> copyParams = new LinkedHashMap<>(params);
        StringBuilder sb = new StringBuilder();
        List<Object> values = new ArrayList<>();

        for (Map.Entry<String, Object> entry : copyParams.entrySet()) {
            String key = entry.getKey();
            if ("order".equals(key) || "limit".equals(key)) {
                continue;
            }
            String value = entry.getValue() != null ? entry.getValue().toString().trim() : null;
            if (StringUtils.isBlank(value)) {
                continue;
            }

            if (key.contains("_")) {
                String[] keyArray = key.split("_", 2);
                String type = keyArray[0];
                String nkey = keyArray[1];

                sb.append(" AND ").append(nkey);

                switch (type) {
                    case "like":
                        sb.append(" LIKE ?");
                        values.add("%" + value + "%");
                        continue;
                    case "gt":
                        sb.append(" > ?");
                        break;
                    case "ge":
                        sb.append(" >= ?");
                        break;
                    case "lt":
                        sb.append(" < ?");
                        break;
                    case "le":
                        sb.append(" <= ?");
                        break;
                    case "ne":
                        sb.append(" <> ?");
                        break;
                    default:
                        sb.append(" = ?");
                        break;
                }
                values.add(value);
            } else {
                sb.append(" AND ").append(key).append(" = ?");
                values.add(value);
            }
        }

        log.info("SQL WHERE 构建完成: params={}, sqlTemplate={}, values={}", params, sb, values);
        return new WhereResult(sb.toString(), values);
    }

    /**
     * 构建参数化 ORDER BY 子句，使用白名单校验
     */
    private OrderResult orderSql(Map<String, Object> params) {
        if (params.containsKey("order") && params.get("order") != null) {
            String orderValue = params.get("order").toString().trim();
            // 解析 "fieldName ASC|DESC"
            String[] parts = orderValue.split("\\s+", 2);
            String field = parts[0];
            String direction = parts.length > 1 ? parts[1].toUpperCase() : "ASC";

            // 校验字段名白名单
            if (!ALLOWED_ORDER_FIELDS.containsKey(field)) {
                log.warn("SQL ORDER BY 字段不合法，已拒绝: field={}", field);
                return new OrderResult("", new ArrayList<>());
            }

            // 校验排序方向
            if (!"ASC".equals(direction) && !"DESC".equals(direction)) {
                log.warn("SQL ORDER BY 方向不合法，已拒绝: direction={}", direction);
                return new OrderResult("", new ArrayList<>());
            }

            String sql = " ORDER BY " + field + " " + direction;
            log.info("SQL ORDER 构建完成: params={}, result={}", params, sql);
            return new OrderResult(sql, new ArrayList<>());
        }
        return new OrderResult("", new ArrayList<>());
    }

    /**
     * 构建参数化 LIMIT 子句，不修改调用方的 map
     */
    private LimitResult limitSql(Map<String, Object> params) {
        if (params.containsKey("limit") && params.get("limit") != null) {
            // 不对调用方 map 做 remove，仅用于读取
            Object limitValue = params.get("limit");
            // limit 只接受数字格式，直接校验
            int limit;
            if (limitValue instanceof Number) {
                limit = ((Number) limitValue).intValue();
            } else {
                try {
                    limit = Integer.parseInt(limitValue.toString().trim());
                } catch (NumberFormatException e) {
                    log.warn("SQL LIMIT 值不合法，已拒绝: limit={}", limitValue);
                    return new LimitResult("", new ArrayList<>());
                }
            }
            if (limit < 0) {
                log.warn("SQL LIMIT 值不能为负数，已拒绝: limit={}", limit);
                return new LimitResult("", new ArrayList<>());
            }

            String sql = " LIMIT ?";
            List<Object> values = new ArrayList<>();
            values.add(limit);
            log.info("SQL LIMIT 构建完成: params={}, result={}", params, sql);
            return new LimitResult(sql, values);
        }
        return new LimitResult("", new ArrayList<>());
    }

    // ==================== 内部数据结构：SQL 模板 + 参数值 ====================

    /** WHERE 子句结果：SQL 模板 + 参数值列表 */
    private static class WhereResult {
        final String sql;
        final List<Object> values;

        WhereResult(String sql, List<Object> values) {
            this.sql = sql;
            this.values = values;
        }
    }

    /** ORDER BY 子句结果（无需参数值，已做白名单校验） */
    private static class OrderResult {
        final String sql;
        final List<Object> values;

        OrderResult(String sql, List<Object> values) {
            this.sql = sql;
            this.values = values;
        }
    }

    /** LIMIT 子句结果：SQL 模板（? 占位符）+ 参数值列表 */
    private static class LimitResult {
        final String sql;
        final List<Object> values;

        LimitResult(String sql, List<Object> values) {
            this.sql = sql;
            this.values = values;
        }
    }
}
