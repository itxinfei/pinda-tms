package com.xinfei.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinfei.DTO.AddressBookDTO;
import com.xinfei.common.utils.PageResponse;
import com.xinfei.common.utils.Result;
import com.xinfei.controller.support.IdConverter;
import com.xinfei.converter.AddressBookConverter;
import com.xinfei.entity.AddressBook;
import com.xinfei.service.IAddressBookService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员地址簿内部端点：仅供客户 BFF 通过 Feign 调用。
 *
 * <p>与旧 {@code /addressBook/**} 的区别：会员归属键改为显式 memberId 入参（会员雪花 ID
 * 由客户 BFF 从认证账号解析得到），不再读 userid 请求头——认证账号 ID 与会员 ID 已不是
 * 同一个键。路径 {@code /addressBook/internal/**} 预留统一 Same-Token 入站加固；
 * 网关无 pd-user 路由，外部不可达。</p>
 *
 * <p>安全语义与旧端点完全一致：page 按 memberId 过滤；save/update 丢弃请求体 userId
 * 强制回填归属；isDefault 先清零同会员旧默认；detail/update/del 做行级归属校验。</p>
 */
@Log4j2
@RestController
@RequestMapping("addressBook/internal")
public class AddressBookInternalController {

    @Autowired
    private IAddressBookService addressBookService;

    @Autowired
    private AddressBookConverter addressBookConverter;

    /**
     * 新增地址簿
     */
    @PostMapping("")
    @Transactional(rollbackFor = Exception.class)
    public Result save(@RequestParam("memberId") String memberId, @RequestBody AddressBookDTO dto) {
        if (dto == null) {
            return Result.error("请求数据不能为空");
        }
        if (IdConverter.toLong(memberId) == null) {
            return Result.error(401, "缺少会员身份，拒绝写入地址簿");
        }
        // 归属只能是自己：请求体里的 userId 一律丢弃，强制回填调用方会员，
        // 否则任意调用方可以给他人塞地址，再用别人的 addressBookId 下单
        dto.setUserId(memberId);
        AddressBook entity = addressBookConverter.toEntity(dto);
        if (isDefault(entity)) {
            addressBookService.lambdaUpdate().set(AddressBook::getIsDefault, 0)
                    .eq(AddressBook::getMemberId, entity.getMemberId()).update();
        }
        boolean result = addressBookService.save(entity);
        if (result) {
            return Result.ok();
        }
        return Result.error();
    }

    /**
     * 查询地址簿详情：不属于该会员/不存在一律返回 null，不泄露数据；调用方判空处理。
     */
    @GetMapping("detail/{id}")
    public AddressBookDTO detail(@PathVariable(name = "id") String id,
                                 @RequestParam("memberId") String memberId) {
        Result ownerCheck = checkOwner(id, memberId);
        if (ownerCheck != null) {
            return null;
        }
        return addressBookConverter.toDto(addressBookService.getById(IdConverter.toLong(id)));
    }

    /**
     * 分页查询某会员的地址簿
     */
    @GetMapping("page")
    public PageResponse<AddressBookDTO> page(@RequestParam("page") Integer page,
                                             @RequestParam("pageSize") Integer pageSize,
                                             @RequestParam("memberId") String memberId,
                                             @RequestParam(value = "keyword", required = false) String keyword) {
        Page<AddressBook> iPage = new Page<>(page, pageSize);
        Page<AddressBook> pageResult = addressBookService.lambdaQuery()
                .eq(StringUtils.isNotEmpty(memberId), AddressBook::getMemberId, IdConverter.toLong(memberId))
                .and(StringUtils.isNotEmpty(keyword), wrapper ->
                        wrapper.like(AddressBook::getContactName, keyword).or()
                                .like(AddressBook::getPhoneNumber, keyword).or()
                                .like(AddressBook::getCompanyName, keyword))
                .page(iPage);

        return PageResponse.<AddressBookDTO>builder()
                .items(addressBookConverter.toDtoList(pageResult.getRecords()))
                .page(page)
                .pagesize(pageSize)
                .pages(pageResult.getPages())
                .counts(pageResult.getTotal())
                .build();
    }

    /**
     * 修改
     */
    @PutMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result update(@PathVariable(name = "id") String id,
                         @RequestParam("memberId") String memberId,
                         @RequestBody AddressBookDTO dto) {
        if (dto == null) {
            return Result.error("请求数据不能为空");
        }
        Result ownerCheck = checkOwner(id, memberId);
        if (ownerCheck != null) {
            return ownerCheck;
        }
        dto.setId(id);
        // 请求体里的 userId 一律丢弃：updateById 会把 body 的 userId 写进行，
        // 等于谁拿到 id 都能把这条地址改成自己的（或改成别人的）
        AddressBook existing = addressBookService.getById(IdConverter.toLong(id));
        dto.setUserId(IdConverter.toStr(existing.getMemberId()));
        AddressBook entity = addressBookConverter.toEntity(dto);
        if (isDefault(entity)) {
            addressBookService.lambdaUpdate().set(AddressBook::getIsDefault, 0)
                    .eq(AddressBook::getMemberId, existing.getMemberId()).update();
        }
        boolean result = addressBookService.updateById(entity);
        if (result) {
            return Result.ok();
        }
        return Result.error();
    }

    /**
     * isDefault 是 Integer，客户端不传就是 null；直接拆箱比较会抛 NPE。
     */
    private boolean isDefault(AddressBook entity) {
        return Integer.valueOf(1).equals(entity.getIsDefault());
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    public Result del(@PathVariable(name = "id") String id, @RequestParam("memberId") String memberId) {
        Result ownerCheck = checkOwner(id, memberId);
        if (ownerCheck != null) {
            return ownerCheck;
        }
        boolean result = addressBookService.removeById(IdConverter.toLong(id));
        if (result) {
            return Result.ok();
        }
        return Result.error();
    }

    /**
     * 行级归属校验：地址簿里的 contactName/phoneNumber/详址属于个人敏感信息，
     * 只允许归属会员本人读写自己的行。
     *
     * @return 校验通过返回 null；否则返回要直接回给调用方的错误响应
     */
    private Result checkOwner(String id, String memberId) {
        Long callerId = IdConverter.toLong(memberId);
        if (callerId == null) {
            return Result.error(401, "缺少会员身份，拒绝访问地址簿");
        }
        AddressBook row = addressBookService.getById(IdConverter.toLong(id));
        if (row == null) {
            return Result.error("地址不存在");
        }
        if (!callerId.equals(row.getMemberId())) {
            log.warn("地址簿越权访问拦截: 会员归属={}, 调用方={}", row.getMemberId(), callerId);
            return Result.error(403, "无权访问该地址");
        }
        return null;
    }
}
