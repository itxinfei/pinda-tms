package com.xinfei.auth.service.v1;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinfei.auth.common.BizException;
import com.xinfei.auth.common.v1.NoticeDetail;
import com.xinfei.auth.common.v1.NoticeForm;
import com.xinfei.auth.common.v1.NoticeItemVO;
import com.xinfei.auth.common.v1.PageResult;
import com.xinfei.auth.entity.SysNotice;
import com.xinfei.auth.entity.SysNoticeRead;
import com.xinfei.auth.mapper.NoticeReadMapper;
import com.xinfei.auth.mapper.SysNoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通知公告（youlai 契约 /api/v1/notices）：草稿/发布/撤回 + 我的通知 + 已读。
 */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final SysNoticeMapper noticeMapper;
    private final NoticeReadMapper readMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int ST_DRAFT = 0;
    private static final int ST_PUBLISHED = 1;
    private static final int ST_REVOKED = 2;

    public PageResult<NoticeItemVO> page(long pageNum, long pageSize, String title,
                                         Integer publishStatus) {
        IPage<SysNotice> res = noticeMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<SysNotice>lambdaQuery()
                        .like(StringUtils.hasText(title), SysNotice::getTitle, title)
                        .eq(publishStatus != null, SysNotice::getStatus, publishStatus)
                        .orderByDesc(SysNotice::getId));
        Set<Long> readIds = readIds(res.getRecords());
        List<NoticeItemVO> list = res.getRecords().stream()
                .map(n -> toItem(n, readIds.contains(n.getId()) ? 1 : 0)).toList();
        return new PageResult<>(list, res.getTotal());
    }

    public NoticeForm form(Long id) {
        SysNotice n = mustGet(id);
        NoticeForm f = new NoticeForm();
        f.setId(String.valueOf(n.getId()));
        f.setTitle(n.getTitle());
        f.setContent(n.getContent());
        f.setType(n.getType());
        f.setLevel(n.getLevel());
        f.setStatus(n.getStatus());
        f.setTargetType(n.getTargetType());
        f.setTargetUsers(parseTargetUsers(n.getTargetUsers()));
        return f;
    }

    @Transactional
    public Long create(NoticeForm form) {
        if (!StringUtils.hasText(form.getTitle())) {
            throw BizException.validFail("通知标题不能为空");
        }
        SysNotice n = new SysNotice();
        n.setTitle(form.getTitle().trim());
        n.setContent(form.getContent());
        n.setType(form.getType() == null ? 1 : form.getType());
        n.setLevel(StringUtils.hasText(form.getLevel()) ? form.getLevel() : "info");
        n.setStatus(ST_DRAFT);
        n.setTargetType(form.getTargetType() == null ? 1 : form.getTargetType());
        n.setTargetUsers(joinTargetUsers(form.getTargetUsers()));
        noticeMapper.insert(n);
        return n.getId();
    }

    @Transactional
    public void update(Long id, NoticeForm form) {
        SysNotice n = mustGet(id);
        if (n.getStatus() != null && n.getStatus() == ST_PUBLISHED) {
            throw BizException.validFail("已发布的通知不可编辑，请先撤回");
        }
        if (StringUtils.hasText(form.getTitle())) {
            n.setTitle(form.getTitle().trim());
        }
        n.setContent(form.getContent());
        if (form.getType() != null) {
            n.setType(form.getType());
        }
        if (StringUtils.hasText(form.getLevel())) {
            n.setLevel(form.getLevel());
        }
        if (form.getTargetType() != null) {
            n.setTargetType(form.getTargetType());
        }
        n.setTargetUsers(joinTargetUsers(form.getTargetUsers()));
        noticeMapper.updateById(n);
    }

    @Transactional
    public void deleteByIds(List<Long> ids) {
        noticeMapper.deleteByIds(ids);
        readMapper.delete(Wrappers.<SysNoticeRead>lambdaQuery().in(SysNoticeRead::getNoticeId, ids));
    }

    @Transactional
    public void publish(Long id) {
        SysNotice n = mustGet(id);
        if (n.getStatus() != null && n.getStatus() == ST_PUBLISHED) {
            throw BizException.validFail("通知已发布");
        }
        n.setStatus(ST_PUBLISHED);
        n.setPublishTime(LocalDateTime.now());
        n.setPublisherId(currentUserId());
        noticeMapper.updateById(n);
    }

    @Transactional
    public void revoke(Long id) {
        SysNotice n = mustGet(id);
        if (n.getStatus() == null || n.getStatus() != ST_PUBLISHED) {
            throw BizException.validFail("仅已发布的通知可撤回");
        }
        n.setStatus(ST_REVOKED);
        n.setRevokeTime(LocalDateTime.now());
        noticeMapper.updateById(n);
    }

    /** 通知详情：查看即标记当前用户已读 */
    @Transactional
    public NoticeDetail detail(Long id) {
        SysNotice n = mustGet(id);
        markRead(id);
        NoticeDetail d = new NoticeDetail();
        d.setId(String.valueOf(n.getId()));
        d.setTitle(n.getTitle());
        d.setContent(n.getContent());
        d.setType(n.getType());
        d.setLevel(n.getLevel());
        d.setPublishStatus(n.getStatus());
        d.setTargetUserIds(n.getTargetUsers());
        d.setPublisherName(n.getPublisherName());
        d.setPublishTime(fmt(n.getPublishTime()));
        return d;
    }

    /** 我的通知（已发布且面向当前用户），按发布时间倒序 */
    public PageResult<NoticeItemVO> myPage(long pageNum, long pageSize, String title, Integer publishStatus) {
        Long me = currentUserId();
        List<SysNotice> published = noticeMapper.selectList(Wrappers.<SysNotice>lambdaQuery()
                .eq(SysNotice::getStatus, ST_PUBLISHED)
                .like(StringUtils.hasText(title), SysNotice::getTitle, title)
                .orderByDesc(SysNotice::getPublishTime).orderByDesc(SysNotice::getId));
        List<SysNotice> visible = published.stream()
                .filter(n -> visibleTo(n, me))
                .collect(Collectors.toList());
        long from = Math.max(0, (pageNum - 1) * pageSize);
        int to = (int) Math.min(visible.size(), from + pageSize);
        List<SysNotice> slice = from >= visible.size() ? List.of() : visible.subList((int) from, to);
        Set<Long> readIds = readIds(slice);
        List<NoticeItemVO> list = slice.stream()
                .map(n -> toItem(n, readIds.contains(n.getId()) ? 1 : 0)).toList();
        return new PageResult<>(list, visible.size());
    }

    /** 全部已读：把当前用户未读的所有已发布通知标记已读 */
    @Transactional
    public void readAll() {
        Long me = currentUserId();
        if (me == null) {
            return;
        }
        Set<Long> read = readIds(noticeMapper.selectList(Wrappers.<SysNotice>lambdaQuery()
                .eq(SysNotice::getStatus, ST_PUBLISHED)));
        List<SysNotice> published = noticeMapper.selectList(Wrappers.<SysNotice>lambdaQuery()
                .eq(SysNotice::getStatus, ST_PUBLISHED));
        for (SysNotice n : published) {
            if (visibleTo(n, me) && !read.contains(n.getId())) {
                SysNoticeRead r = new SysNoticeRead();
                r.setNoticeId(n.getId());
                r.setUserId(me);
                r.setReadTime(LocalDateTime.now());
                readMapper.insert(r);
            }
        }
    }

    private boolean visibleTo(SysNotice n, Long me) {
        if (n.getTargetType() == null || n.getTargetType() == 1) {
            return true;
        }
        if (me == null) {
            return false;
        }
        return containsUser(n.getTargetUsers(), me);
    }

    private boolean containsUser(String csv, Long userId) {
        if (!StringUtils.hasText(csv)) {
            return false;
        }
        for (String p : csv.split(",")) {
            if (p.trim().equals(String.valueOf(userId))) {
                return true;
            }
        }
        return false;
    }

    private Set<Long> readIds(List<SysNotice> notices) {
        Long me = currentUserId();
        if (me == null || notices.isEmpty()) {
            return new HashSet<>();
        }
        List<Long> ids = notices.stream().map(SysNotice::getId).toList();
        return readMapper.selectList(Wrappers.<SysNoticeRead>lambdaQuery()
                        .eq(SysNoticeRead::getUserId, me).in(SysNoticeRead::getNoticeId, ids))
                .stream().map(SysNoticeRead::getNoticeId).collect(Collectors.toSet());
    }

    @Transactional
    public void markRead(Long noticeId) {
        Long me = currentUserId();
        if (me == null) {
            return;
        }
        long exists = readMapper.selectCount(Wrappers.<SysNoticeRead>lambdaQuery()
                .eq(SysNoticeRead::getNoticeId, noticeId).eq(SysNoticeRead::getUserId, me));
        if (exists == 0) {
            SysNoticeRead r = new SysNoticeRead();
            r.setNoticeId(noticeId);
            r.setUserId(me);
            r.setReadTime(LocalDateTime.now());
            readMapper.insert(r);
        }
    }

    private String joinTargetUsers(List<Long> users) {
        if (users == null || users.isEmpty()) {
            return null;
        }
        return users.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private List<Long> parseTargetUsers(String csv) {
        List<Long> out = new ArrayList<>();
        if (!StringUtils.hasText(csv)) {
            return out;
        }
        for (String p : csv.split(",")) {
            try {
                out.add(Long.parseLong(p.trim()));
            } catch (Exception ignored) {
                // 跳过非法项
            }
        }
        return out;
    }

    private Long currentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    private SysNotice mustGet(Long id) {
        SysNotice n = noticeMapper.selectById(id);
        if (n == null) {
            throw BizException.validFail("通知不存在");
        }
        return n;
    }

    private NoticeItemVO toItem(SysNotice n, int isRead) {
        NoticeItemVO v = new NoticeItemVO();
        v.setId(String.valueOf(n.getId()));
        v.setTitle(n.getTitle());
        v.setContent(n.getContent());
        v.setType(n.getType());
        v.setLevel(n.getLevel());
        v.setPublishStatus(n.getStatus());
        v.setIsRead(isRead);
        v.setPublishTime(fmt(n.getPublishTime()));
        v.setRevokeTime(fmt(n.getRevokeTime()));
        return v;
    }

    private String fmt(LocalDateTime t) {
        return t == null ? null : t.format(FMT);
    }
}