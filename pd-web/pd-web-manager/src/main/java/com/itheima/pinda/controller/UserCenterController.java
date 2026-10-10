package com.itheima.pinda.controller;

import com.itheima.pinda.DTO.UserDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.context.RequestContext;
import com.itheima.pinda.common.exception.PdException;
import com.itheima.pinda.feign.OrgFeign;
import com.itheima.pinda.feign.RoleFeign;
import com.itheima.pinda.feign.UserFeign;
import com.itheima.pinda.util.BeanUtil;
import com.itheima.pinda.vo.base.userCenter.MessageVo;
import com.itheima.pinda.vo.base.userCenter.SysUserVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("userCenter")
@Tag(name = "个人中心")
@Slf4j
public class UserCenterController {
    @Autowired
    private OrgFeign orgFeign;
    @Autowired
    private UserFeign userFeign;
    @Autowired
    private RoleFeign roleFeign;

    /**
     * 获取个人信息
     *
     * @return 用户信息
     */
    @Operation(summary = "获取个人信息")
    @GetMapping("/info")
    public SysUserVo info() {
        // 从 token 上下文获取用户ID（网关透传 userid 头）
        Long userId = RequestContext.getUserId() == null ? null : Long.valueOf(RequestContext.getUserId());
        if (userId == null) {
            throw new PdException("用户未登录");
        }
        UserDTO user = userFeign.get(userId);
        SysUserVo vo = new SysUserVo();
        if (user != null) {
            vo = BeanUtil.parseUser2Vo(user, roleFeign, orgFeign);
        }
        return vo;
    }

    @Operation(summary = "获取通知公告")
    @GetMapping("/message")
    public PageResponse<MessageVo> info(@RequestParam(name = "page") Integer page, @RequestParam(name = "pageSize") Integer pageSize, @RequestParam(value = "messageType", required = false) String messageType) {
        // 说明：消息中心为占位实现（返回示例数据），待对接通知/消息模块后接入真实未读条数与列表
        List<MessageVo> messageVoList = new ArrayList<>();
        MessageVo messageVo = new MessageVo();
        messageVo.setId("1");
        messageVo.setContent("hahahaha");
        messageVo.setTitle("说点什么呢");
        messageVo.setStatus(1);
        messageVo.setMessageType("notice");
        messageVoList.add(messageVo);
        return PageResponse.<MessageVo>builder().pages(1L).counts(2L).page(page).pagesize(pageSize).items(messageVoList).build();
    }

    @Operation(summary = "打开未读消息")
    @PutMapping("/message/{id}")
    public MessageVo read(@PathVariable(value = "id") Long id) {
        // 说明：消息已读状态切换为占位实现，待对接消息模块后接入
        MessageVo messageVo = new MessageVo();
        messageVo.setId("1");
        messageVo.setContent("hahahaha");
        messageVo.setTitle("说点什么呢");
        messageVo.setStatus(0);
        messageVo.setMessageType("notice");
        return messageVo;
    }

}
