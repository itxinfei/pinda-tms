package com.itheima.pinda.feign;

import com.itheima.pinda.DTO.AddressBookDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

// 不要在接口上加类级 @RequestMapping；路径前缀下沉到方法，避免被 MVC 注册与本地 Controller 冲突。
@FeignClient(name = "pd-user")
public interface AddressBookFeign {

    /**
     * 分页查询
     *
     * @param page
     * @param pageSize
     * @param userId
     * @return
     */
    @GetMapping("/addressBook/page")
    PageResponse<AddressBookDTO> page(@RequestParam("page") Integer page,@RequestParam("pageSize") Integer pageSize, @RequestParam("userId")String userId,@RequestParam("keyword") String keyword);

    /**
     * 新增
     *
     * @param entity
     * @return
     */
    @PostMapping("/addressBook")
    Result save(@RequestBody AddressBookDTO entity);

    /**
     * 修改
     *
     * @param id
     * @param entity
     * @return
     */
    @PutMapping("/addressBook/{id}")
    Result update(@PathVariable(name = "id") String id, @RequestBody AddressBookDTO entity);

    /**
     * 删除
     *
     * @param id
     * @return
     */
    @DeleteMapping("/addressBook/{id}")
    Result del(@PathVariable(name = "id") String id);

    /**
     * 详情（鉴权失败/不存在返回 null）
     *
     * @param id
     * @return
     */
    @GetMapping("/addressBook/detail/{id}")
    AddressBookDTO detail(@PathVariable(name = "id") String id);
}
