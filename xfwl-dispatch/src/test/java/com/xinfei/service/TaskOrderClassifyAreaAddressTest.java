package com.xinfei.service;

import com.xinfei.DTO.AreaDTO;
import com.xinfei.feign.AreaFeign;
import com.xinfei.service.impl.TaskOrderClassifyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 调度订单分类：地址区划拼接的健壮性单测。
 *
 * <p>背景：区划 Feign 抖动/返回空时，原实现 {@code areaMap.get(id).getName()} 直接抛 NPE，
 * 日志只有 "Map.get(Object) is null"，无法定位缺失层级。现实现要求：任何区划数据异常都必须
 * 转成带层级的业务异常（RuntimeException + 明确文案），由上层单订单 try-catch 隔离跳过。</p>
 *
 * <p>被测方法为 private，通过反射组合调用（getAreaMap 批量取区划 + buildFullAddress 拼接，
 * 6bd0ef9d 区划批量优化后的链路形态）；AreaFeign 用 Mockito mock 后反射注入
 * （生产类是 {@code @Autowired} 字段注入，风格与 {@link SmsNotificationServiceTest} 一致）。</p>
 */
public class TaskOrderClassifyAreaAddressTest {

    private static final Long PROVINCE = 420000L;
    private static final Long CITY = 420100L;
    private static final Long COUNTY = 420102L;

    private TaskOrderClassifyServiceImpl service;
    private AreaFeign areaFeign;

    @BeforeEach
    public void setUp() throws Exception {
        service = new TaskOrderClassifyServiceImpl();
        areaFeign = mock(AreaFeign.class);
        Field field = TaskOrderClassifyServiceImpl.class.getDeclaredField("areaFeign");
        field.setAccessible(true);
        field.set(service, areaFeign);
    }

    /**
     * 反射组合调用生产链路（6bd0ef9d 区划批量优化后的新形态）：
     * getAreaMap(ids) 批量取区划映射 → buildFullAddress(map, role, 省, 市, 区, 详细地址)。
     */
    private String invoke(String role, String province, String city, String county, String detail)
            throws Exception {
        Set<Long> areaIds = Set.of(PROVINCE, CITY, COUNTY);
        Method getMap = TaskOrderClassifyServiceImpl.class.getDeclaredMethod("getAreaMap", Set.class);
        getMap.setAccessible(true);
        Method build = TaskOrderClassifyServiceImpl.class.getDeclaredMethod(
                "buildFullAddress", Map.class, String.class, String.class, String.class, String.class, String.class);
        build.setAccessible(true);
        try {
            @SuppressWarnings("unchecked")
            Map<Long, AreaDTO> areaMap = (Map<Long, AreaDTO>) getMap.invoke(service, areaIds);
            return (String) build.invoke(service, areaMap, role, province, city, county, detail);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw e;
        }
    }

    private AreaDTO area(Long id, String name) {
        AreaDTO dto = new AreaDTO();
        dto.setId(id);
        dto.setName(name);
        return dto;
    }

    @Test
    @DisplayName("正常路径：省+市+区+详细地址 拼接")
    public void testHappyPath() throws Exception {
        when(areaFeign.findAll(isNull(), any())).thenReturn(List.of(
                area(PROVINCE, "湖北省"), area(CITY, "武汉市"), area(COUNTY, "江岸区")));
        String addr = invoke("收件人", "420000", "420100", "420102", "解放大道1号");
        assertEquals("湖北省武汉市江岸区解放大道1号", addr);
    }

    @Test
    @DisplayName("区划 Feign 返回 null → 业务异常「区划服务无返回」，不再是 NPE")
    public void testNullAreaMap() {
        when(areaFeign.findAll(isNull(), any())).thenReturn(null);
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> invoke("收件人", "420000", "420100", "420102", "解放大道1号"));
        assertTrue(e.getMessage().contains("区划服务无返回"), "实际文案: " + e.getMessage());
        assertFalse(e instanceof NullPointerException);
    }

    @Test
    @DisplayName("区划缺失某一级（返回空列表）→ 业务异常「区划数据缺失」，不再是 NPE")
    public void testEmptyAreaMap() {
        when(areaFeign.findAll(isNull(), any())).thenReturn(List.of());
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> invoke("发件人", "420000", "420100", "420102", "解放大道1号"));
        assertTrue(e.getMessage().contains("区划数据缺失"), "实际文案: " + e.getMessage());
        assertFalse(e instanceof NullPointerException);
    }

    @Test
    @DisplayName("只返回部分层级（缺区级）→ 异常文案能定位到区与具体 id")
    public void testMissingCountyLevel() {
        when(areaFeign.findAll(isNull(), any())).thenReturn(List.of(
                area(PROVINCE, "湖北省"), area(CITY, "武汉市")));
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> invoke("收件人", "420000", "420100", "420102", "解放大道1号"));
        assertTrue(e.getMessage().contains("收件人"), "实际文案: " + e.getMessage());
        assertTrue(e.getMessage().contains("420102"), "实际文案: " + e.getMessage());
    }

    @Test
    @DisplayName("区划名称为空白 → 同样按缺失处理，不拼出 nullnullnull 地址")
    public void testBlankAreaName() {
        when(areaFeign.findAll(isNull(), any())).thenReturn(List.of(
                area(PROVINCE, "湖北省"), area(CITY, "武汉市"), area(COUNTY, "  ")));
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> invoke("收件人", "420000", "420100", "420102", "解放大道1号"));
        assertTrue(e.getMessage().contains("区划数据缺失"), "实际文案: " + e.getMessage());
    }

    @Test
    @DisplayName("区划id 空白/非数字 → 明确业务异常，不再抛 NumberFormatException")
    public void testIllegalAreaId() {
        when(areaFeign.findAll(any(), any())).thenReturn(List.of(area(PROVINCE, "湖北省")));
        RuntimeException blank = assertThrows(RuntimeException.class,
                () -> invoke("收件人", "", "420100", "420102", "x"));
        assertTrue(blank.getMessage().contains("区划id为空"), "实际文案: " + blank.getMessage());

        RuntimeException bad = assertThrows(RuntimeException.class,
                () -> invoke("收件人", "420000", "abc", "420102", "x"));
        assertTrue(bad.getMessage().contains("区划id非法"), "实际文案: " + bad.getMessage());
    }

    @Test
    @DisplayName("区划 Feign 抛异常（服务不可达）→ 转成「区划查询失败」业务异常")
    public void testFeignFailure() {
        when(areaFeign.findAll(any(), any())).thenThrow(new RuntimeException("Connection refused"));
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> invoke("收件人", "420000", "420100", "420102", "解放大道1号"));
        assertTrue(e.getMessage().contains("区划查询失败"), "实际文案: " + e.getMessage());
        assertFalse(e instanceof NullPointerException);
    }

    @Test
    @DisplayName("详细地址为空白时不拼 null，正常返回区划全称")
    public void testBlankDetail() throws Exception {
        when(areaFeign.findAll(isNull(), any())).thenReturn(List.of(
                area(PROVINCE, "湖北省"), area(CITY, "武汉市"), area(COUNTY, "江岸区")));
        String addr = invoke("收件人", "420000", "420100", "420102", null);
        assertEquals("湖北省武汉市江岸区", addr);
        assertFalse(addr.contains("null"));
    }

    @Test
    @DisplayName("父级入参透传：区划查询走 ids 维度（parentId 为 null）")
    public void testFindAllCalledWithNullParent() throws Exception {
        when(areaFeign.findAll(isNull(), any())).thenReturn(List.of(
                area(PROVINCE, "湖北省"), area(CITY, "武汉市"), area(COUNTY, "江岸区")));
        invoke("收件人", "420000", "420100", "420102", "解放大道1号");
        verify(areaFeign).findAll(isNull(), any());
    }
}