package com.itheima.pinda.feign;

import com.itheima.pinda.DTO.AddressBookDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

// 不要在接口上加类级 @RequestMapping；路径前缀下沉到方法，避免被 MVC 注册与本地 Controller 冲突。
// 全部为 pd-user 内部端点：会员雪花 ID（member_info.id）由客户 BFF 从认证账号解析后显式传入，
// pd-user 不再读 userid 请求头——认证账号 ID 与会员 ID 已不是同一个键。
@FeignClient(name = "pd-user")
public interface AddressBookFeign {

    /**
     * 分页查询某会员的地址簿
     *
     * @param memberId 会员雪花 ID（String 形态，防精度丢失）
     */
    @GetMapping("/addressBook/internal/page")
    PageResponse<AddressBookDTO> page(@RequestParam("page") Integer page,
                                      @RequestParam("pageSize") Integer pageSize,
                                      @RequestParam("memberId") String memberId,
                                      @RequestParam(value = "keyword", required = false) String keyword);

    /**
     * 新增（归属强制为 memberId，请求体里的 userId 被下游丢弃）
     */
    @PostMapping("/addressBook/internal")
    Result save(@RequestParam("memberId") String memberId, @RequestBody AddressBookDTO entity);

    /**
     * 修改
     */
    @PutMapping("/addressBook/internal/{id}")
    Result update(@PathVariable(name = "id") String id,
                  @RequestParam("memberId") String memberId,
                  @RequestBody AddressBookDTO entity);

    /**
     * 删除
     */
    @DeleteMapping("/addressBook/internal/{id}")
    Result del(@PathVariable(name = "id") String id, @RequestParam("memberId") String memberId);

    /**
     * 详情（不属于该会员/不存在返回 null）
     */
    @GetMapping("/addressBook/internal/detail/{id}")
    AddressBookDTO detail(@PathVariable(name = "id") String id, @RequestParam("memberId") String memberId);
}
