package com.itheima.pinda.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.j2cache.annotation.Cache;
import com.itheima.j2cache.annotation.CacheEvictor;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.entity.AddressBook;
import com.itheima.pinda.service.IAddressBookService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import net.oschina.j2cache.CacheChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 地址簿
 */
@Log4j2
@RestController
@RequestMapping("addressBook")
public class AddressBookController {
    @Autowired
    private IAddressBookService addressBookService;

    @Autowired
    private CacheChannel cacheChannel;

    private String region = "addressBook";

    /**
     * 新增地址簿
     *
     * @param entity
     * @return
     */
    @PostMapping("")
    @Transactional(rollbackFor = Exception.class)
    public Result save(@RequestBody AddressBook entity) {
        if (entity == null || StringUtils.isBlank(entity.getUserId())) {
            return Result.error("用户ID不能为空");
        }
        // 归属只能是自己：否则任意调用方可以给他人塞地址，再用别人的 addressBookId 下单
        String callerId = currentUserId();
        if (StringUtils.isBlank(callerId)) {
            return Result.error(401, "缺少身份信息，拒绝写入地址簿");
        }
        if (!callerId.equals(entity.getUserId())) {
            log.warn("地址簿越权写入拦截: 声称归属={}, 调用方={}", entity.getUserId(), callerId);
            return Result.error(403, "只能给自己的账号新增地址");
        }
        if (isDefault(entity)) {
            addressBookService.lambdaUpdate().set(AddressBook::getIsDefault, 0).eq(AddressBook::getUserId, entity.getUserId()).update();
        }

        boolean result = addressBookService.save(entity);
        if (result && entity.getId() != null) {
            //载入缓存
            cacheChannel.set(region, entity.getId(), entity);
            return Result.ok();
        }
        return Result.error();
    }

    /**
     * 查询地址簿详情
     *
     * <p>这里原来挂着 {@code @Cache(region,key="ab",params="id")}：缓存键只含地址 id，
     * 不含调用方身份，意味着 A 查过一次之后 B 用同一个 id 就能直接命中缓存拿到 A 的
     * 姓名/手机/详址——归属校验也会被缓存旁路掉。按主键查库的代价本来就极低，
     * 所以去掉这层缓存，改为每次走归属校验。</p>
     *
     * @param id 地址 id
     * @return 地址详情（仅本人）
     */
    @GetMapping("detail/{id}")
    public Result detail(@PathVariable(name = "id") String id) {
        Result ownerCheck = checkOwner(id);
        if (ownerCheck != null) {
            return ownerCheck;
        }
        AddressBook addressBook = addressBookService.getById(id);
        if (addressBook != null) {
            return Result.ok().put("data", addressBook);
        }
        return Result.error("地址不存在");
    }

    /**
     * 分页查询
     *
     * @param page
     * @param pageSize
     * @param userId
     * @return
     */
    @GetMapping("page")
    public PageResponse<AddressBook> page(Integer page, Integer pageSize, String userId, String keyword) {
        Page<AddressBook> iPage = new Page(page, pageSize);
        Page<AddressBook> pageResult = addressBookService.lambdaQuery()
                .eq(StringUtils.isNotEmpty(userId), AddressBook::getUserId, userId)
                .and(StringUtils.isNotEmpty(keyword), wrapper ->
                        wrapper.like(AddressBook::getName, keyword).or()
                                .like(AddressBook::getPhoneNumber, keyword).or()
                                .like(AddressBook::getCompanyName, keyword))
                .page(iPage);

        return PageResponse.<AddressBook>builder()
                .items(pageResult.getRecords())
                .page(page)
                .pagesize(pageSize)
                .pages(pageResult.getPages())
                .counts(pageResult.getTotal())
                .build();
    }

    /**
     * 修改
     *
     * @param id
     * @param entity
     * @return
     */
    @PutMapping("/{id}")
    @CacheEvictor(value = {@Cache(region = "addressBook",key = "ab",params = "1.id")})
    @Transactional(rollbackFor = Exception.class)
    public Result update(@PathVariable(name = "id") String id, @RequestBody AddressBook entity) {
        if (entity == null) {
            return Result.error("请求数据不能为空");
        }
        Result ownerCheck = checkOwner(id);
        if (ownerCheck != null) {
            return ownerCheck;
        }
        entity.setId(id);
        // 请求体里的 userId 一律丢弃：updateById 会把 body 的 userId 写进行，
        // 等于谁拿到 id 都能把这条地址改成自己的（或改成别人的）
        String ownerUserId = addressBookService.getById(id).getUserId();
        entity.setUserId(ownerUserId);
        if (isDefault(entity)) {
            addressBookService.lambdaUpdate().set(AddressBook::getIsDefault, 0).eq(AddressBook::getUserId, ownerUserId).update();
        }
        boolean result = addressBookService.updateById(entity);
        if (result) {
            return Result.ok();
        }
        return Result.error();
    }

    /**
     * isDefault 是 Integer，客户端不传就是 null；原来的 {@code 1 == entity.getIsDefault()}
     * 会在拆箱时抛 NPE，直接 500（新增/修改地址不带这个字段就会踩）。
     */
    private boolean isDefault(AddressBook entity) {
        return Integer.valueOf(1).equals(entity.getIsDefault());
    }

    /**
     * 删除
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @CacheEvictor({@Cache(region = "addressBook",key = "ab",params = "id")})
    public Result del(@PathVariable(name = "id") String id) {
        Result ownerCheck = checkOwner(id);
        if (ownerCheck != null) {
            return ownerCheck;
        }
        boolean result = addressBookService.removeById(id);
        if (result) {
            return Result.ok();
        }
        return Result.error();
    }

    /**
     * 行级归属校验：地址簿里的 name/phone/详址属于个人敏感信息，
     * 只允许本人（或本人所在登录会话）读写自己的行。
     *
     * <p>身份只认网关注入、并由 Feign 透传的 userid 头（见 RequestHeaderInterceptor），
     * 不接受任何查询参数或请求体里的 userId —— 否则 ?userId=别人 就能枚举别人的地址。</p>
     *
     * @return 校验通过返回 null；否则返回要直接回给调用方的错误响应
     */
    private Result checkOwner(String id) {
        String callerId = currentUserId();
        if (StringUtils.isBlank(callerId)) {
            return Result.error(401, "缺少身份信息，拒绝访问地址簿");
        }
        AddressBook row = addressBookService.getById(id);
        if (row == null) {
            return Result.error("地址不存在");
        }
        if (!callerId.equals(row.getUserId())) {
            log.warn("地址簿越权访问拦截: id 归属={}, 调用方={}", row.getUserId(), callerId);
            return Result.error(403, "无权访问该地址");
        }
        return null;
    }

    /**
     * 取当前调用方身份。pd-user 没有注册 ContextHandlerInterceptor，
     * 直接读头最可靠（与 MenuController 同一套做法）。
     */
    private String currentUserId() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            return attributes.getRequest().getHeader("userid");
        } catch (Exception e) {
            return null;
        }
    }
}
