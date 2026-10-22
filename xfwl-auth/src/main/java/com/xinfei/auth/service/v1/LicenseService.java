package com.xinfei.auth.service.v1;

import com.xinfei.auth.common.BizException;
import com.xinfei.auth.entity.SysLicenseUsage;
import com.xinfei.auth.license.DecodedLicense;
import com.xinfei.auth.license.LicenseInfo;
import com.xinfei.auth.license.LicenseStatus;
import com.xinfei.auth.license.LicenseVerifier;
import com.xinfei.auth.license.MachineFingerprint;
import com.xinfei.auth.mapper.SysLicenseUsageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 授权状态服务：授权真伪一律由后端判定（验签 + 机器绑定 + 到期/宽限/应急），
 * 前端只读状态展示，不参与判定。不阻断任何业务接口，仅驱动授权页/购买提示水印。
 *
 * <p>状态机：VALID / EMERGENCY / GRACE（三者为已激活）、TRIAL / TRIAL_EXPIRED /
 * MISMATCH / EXPIRED。试用按机器指纹在 sys_license_usage 落库首见时间，
 * 重建容器、删卷都不刷新试用期。</p>
 */
@Slf4j
@Service
public class LicenseService {

    private static final long DAY_MS = 86_400_000L;
    /** 授权码受众，签发器固定填 xinfei-tms；非本系统签发的码一律不认 */
    private static final String AUDIENCE = "xinfei-tms";

    private final LicenseVerifier verifier;
    private final MachineFingerprint fingerprint;
    private final SysLicenseUsageMapper usageMapper;
    private final int trialDays;
    private final int graceDays;
    private final int emergencyMaxDays;

    public LicenseService(LicenseVerifier verifier,
                          MachineFingerprint fingerprint,
                          SysLicenseUsageMapper usageMapper,
                          @Value("${xinfei.license.trial-days:7}") int trialDays,
                          @Value("${xinfei.license.grace-days:7}") int graceDays,
                          @Value("${xinfei.license.emergency-max-days:14}") int emergencyMaxDays) {
        this.verifier = verifier;
        this.fingerprint = fingerprint;
        this.usageMapper = usageMapper;
        this.trialDays = trialDays;
        this.graceDays = graceDays;
        this.emergencyMaxDays = emergencyMaxDays;
    }

    /** 当前授权状态（登录态可查；前端授权页/水印唯一数据源）。 */
    public LicenseInfo status() {
        String fp = fingerprint.get();
        Optional<String> raw = verifier.readRawKey();
        if (raw.isEmpty()) {
            return trialInfo(fp, LicenseVerifier.KeySource.NONE.name(), null);
        }
        return evaluate(raw.get(), fp);
    }

    /**
     * 激活：校验授权码并落盘。仅超管可调用（Controller 加角色校验）。
     * 环境变量托管时拒绝写入；坏码/绑别的机器/已过期一律拒绝且不落盘。
     */
    public LicenseInfo activate(String rawKey) {
        if (!StringUtils.hasText(rawKey)) {
            throw new BizException("请输入授权码");
        }
        if (verifier.isEnvManaged()) {
            throw new BizException("当前授权由环境变量 XINFEI_LICENSE 托管（只读），页面不能激活或覆盖");
        }
        String fp = fingerprint.get();
        DecodedLicense d = verifier.verify(rawKey);
        if (!AUDIENCE.equals(d.getAud())) {
            throw new BizException("授权码受众不匹配，非本系统签发");
        }
        long now = System.currentTimeMillis();

        if (DecodedLicense.TYPE_TRIAL.equalsIgnoreCase(d.getType())) {
            throw new BizException("体验授权码已停用，请使用绑定本机机器码的正式授权码");
        }
        if (d.isEmergency()) {
            long exp = requireFutureExpiry(d, now);
            Long issued = d.getIssuedAt();
            if (issued != null && exp - issued > (long) emergencyMaxDays * DAY_MS) {
                throw new BizException("应急授权码有效期超过上限 " + emergencyMaxDays + " 天，不予激活");
            }
            verifier.saveKey(rawKey);
            log.info("[license] 应急授权已激活：licensee={}, 到期={}", d.getLicensee(), exp);
            return status();
        }

        if (!isFormalType(d.getType())) {
            throw new BizException("未知授权类型：" + d.getType());
        }
        String bound = d.getMachineId();
        if (!StringUtils.hasText(bound)) {
            throw new BizException("正式授权码必须绑定机器码，请把本机机器码发给供应商重新签发");
        }
        if (!fp.equalsIgnoreCase(bound)) {
            throw new BizException("授权码绑定的是其他机器，本机无法激活。请把本机机器码发给供应商重新签发");
        }
        Long exp = d.getExpiresAt();
        if (exp != null && exp > 0 && now >= exp) {
            throw new BizException("授权码已过期，请联系供应商续期后再激活");
        }
        verifier.saveKey(rawKey);
        log.info("[license] 正式授权已激活：licensee={}, type={}, 到期={}", d.getLicensee(), d.getType(), exp);
        return status();
    }

    /** 清除落盘授权码（回到试用态）。环境变量托管时拒绝。 */
    public LicenseInfo clear() {
        if (verifier.isEnvManaged()) {
            throw new BizException("当前授权由环境变量 XINFEI_LICENSE 托管（只读），页面不能清除");
        }
        boolean removed = verifier.deleteKey();
        log.info("[license] 清除落盘授权码，removed={}", removed);
        return status();
    }

    /**
     * 评估一枚已存在的授权码，产出状态。验签/受众/类型异常不抛错，
     * 回落未激活（试用）态并把原因写进 message，避免坏码让状态接口 500。
     */
    private LicenseInfo evaluate(String raw, String fp) {
        String source = verifier.currentSource().name();
        DecodedLicense d;
        try {
            d = verifier.verify(raw);
        } catch (BizException e) {
            log.warn("[license] 已保存授权码校验失败：{}", e.getMessage());
            return trialInfo(fp, source, "已保存的授权码校验失败（" + e.getMessage() + "），请清除后重新激活");
        }
        if (!AUDIENCE.equals(d.getAud())) {
            return trialInfo(fp, source, "授权码受众不匹配，非本系统签发");
        }
        if (DecodedLicense.TYPE_TRIAL.equalsIgnoreCase(d.getType())) {
            return trialInfo(fp, source, "体验授权码已停用，请使用绑定本机的正式授权码");
        }

        LicenseInfo info = baseInfo(fp, source);
        fillLicense(info, d);

        if (d.isEmergency()) {
            return applyEmergency(info, d);
        }
        if (!isFormalType(d.getType())) {
            return trialInfo(fp, source, "未知授权类型：" + d.getType());
        }
        String bound = d.getMachineId();
        if (!StringUtils.hasText(bound)) {
            return trialInfo(fp, source, "正式授权码未绑定机器码，无法在本机生效");
        }
        if (!fp.equalsIgnoreCase(bound)) {
            info.setStatus(LicenseStatus.MISMATCH);
            info.setActivated(false);
            info.setMessage("授权码绑定的是其他机器，本机不生效。请用本机机器码联系供应商重新签发");
            return info;
        }
        return applyExpiry(info, d);
    }

    /** 应急码：不绑机、短期；过期即 EXPIRED（无宽限）。 */
    private LicenseInfo applyEmergency(LicenseInfo info, DecodedLicense d) {
        long now = System.currentTimeMillis();
        Long exp = d.getExpiresAt();
        Long issued = d.getIssuedAt();
        if (exp == null || exp <= 0) {
            return downgradeTrial(info, "应急授权码缺少到期时间，不予生效");
        }
        if (issued != null && exp - issued > (long) emergencyMaxDays * DAY_MS) {
            return downgradeTrial(info, "应急授权码有效期超过上限 " + emergencyMaxDays + " 天，不予生效");
        }
        if (now >= exp) {
            info.setStatus(LicenseStatus.EXPIRED);
            info.setActivated(false);
            info.setEmergencyDaysLeft(0);
            info.setMessage("应急授权已过期，请尽快激活正式授权码");
            return info;
        }
        info.setStatus(LicenseStatus.EMERGENCY);
        info.setActivated(true);
        info.setEmergencyDaysLeft(daysCeil(exp - now));
        info.setMessage("应急授权有效，剩余 " + info.getEmergencyDaysLeft() + " 天，请尽快激活绑定本机的正式授权码");
        return info;
    }

    /** 正式码到期判定：未到期 VALID；过期在宽限期内 GRACE；超宽限 EXPIRED。exp 为空视为永久。 */
    private LicenseInfo applyExpiry(LicenseInfo info, DecodedLicense d) {
        long now = System.currentTimeMillis();
        Long exp = d.getExpiresAt();
        if (exp == null || exp <= 0) {
            markValid(info, "已授权（永久）");
            return info;
        }
        if (now <= exp) {
            markValid(info, "已授权，剩余 " + daysCeil(exp - now) + " 天");
            return info;
        }
        long graceEnd = exp + (long) graceDays * DAY_MS;
        if (now <= graceEnd) {
            int left = daysCeil(graceEnd - now);
            info.setStatus(LicenseStatus.GRACE);
            info.setActivated(true);
            info.setGraceDaysLeft(left);
            info.setMessage("授权已到期，宽限期剩余 " + left + " 天，请尽快续期");
            return info;
        }
        info.setStatus(LicenseStatus.EXPIRED);
        info.setActivated(false);
        info.setMessage("授权已过期且超出宽限期，请联系供应商续期");
        return info;
    }

    private void markValid(LicenseInfo info, String message) {
        info.setStatus(LicenseStatus.VALID);
        info.setActivated(true);
        info.setMessage(message);
    }

    /** 已填了授权信息后又判失效：回落试用态，但保留 licensee 等展示字段无意义，故直接重建试用对象。 */
    private LicenseInfo downgradeTrial(LicenseInfo filled, String reason) {
        return trialInfo(filled.getFingerprint(), filled.getSource(), reason);
    }

    private long requireFutureExpiry(DecodedLicense d, long now) {
        Long exp = d.getExpiresAt();
        if (exp == null || exp <= 0) {
            throw new BizException("应急授权码缺少到期时间");
        }
        if (now >= exp) {
            throw new BizException("应急授权码已过期，无法激活");
        }
        return exp;
    }

    private boolean isFormalType(String type) {
        return DecodedLicense.TYPE_PERSONAL.equalsIgnoreCase(type)
                || DecodedLicense.TYPE_ENTERPRISE.equalsIgnoreCase(type)
                || DecodedLicense.TYPE_OEM.equalsIgnoreCase(type);
    }

    private void fillLicense(LicenseInfo info, DecodedLicense d) {
        info.setLicensee(d.getLicensee());
        info.setType(d.getType());
        info.setInstances(d.getInstances());
        info.setIssuedAt(d.getIssuedAt());
        info.setExpiresAt(d.getExpiresAt());
        info.setBoundMachineId(d.getMachineId());
    }

    private LicenseInfo baseInfo(String fp, String source) {
        LicenseInfo info = new LicenseInfo();
        info.setFingerprint(fp);
        info.setFingerprintMasked(fingerprint.masked());
        info.setSource(source);
        return info;
    }

    /** 构造未激活（试用）状态，并按机器指纹首见时间计时落库。 */
    private LicenseInfo trialInfo(String fp, String source, String reason) {
        LocalDateTime firstSeen = recordFirstSeen(fp);
        long elapsedMs = Duration.between(firstSeen, LocalDateTime.now()).toMillis();
        int used = (int) (elapsedMs / DAY_MS);

        LicenseInfo info = baseInfo(fp, source);
        if (used >= trialDays) {
            info.setStatus(LicenseStatus.TRIAL_EXPIRED);
            info.setActivated(false);
            info.setTrialDaysLeft(0);
            info.setMessage(StringUtils.hasText(reason)
                    ? reason
                    : "试用已到期，请购买商业授权后激活");
        } else {
            int left = trialDays - used;
            info.setStatus(LicenseStatus.TRIAL);
            info.setActivated(false);
            info.setTrialDaysLeft(left);
            info.setMessage(StringUtils.hasText(reason)
                    ? reason
                    : "未授权试用中，剩余 " + left + " 天。商用请购买商业授权");
        }
        return info;
    }

    /** 上报机器指纹首见：不存在则插入（首见=现在），存在仅刷新 last_seen，首见时间永不回改。 */
    private LocalDateTime recordFirstSeen(String fp) {
        SysLicenseUsage exist = usageMapper.selectById(fp);
        LocalDateTime now = LocalDateTime.now();
        if (exist == null) {
            SysLicenseUsage u = new SysLicenseUsage();
            u.setFingerprint(fp);
            u.setFirstSeenAt(now);
            u.setLastSeenAt(now);
            try {
                usageMapper.insert(u);
                return now;
            } catch (DuplicateKeyException e) {
                // 并发首见：以已落库记录为准
                SysLicenseUsage again = usageMapper.selectById(fp);
                if (again != null) {
                    return again.getFirstSeenAt();
                }
            }
        } else {
            exist.setLastSeenAt(now);
            usageMapper.updateById(exist);
            return exist.getFirstSeenAt();
        }
        return now;
    }

    private static int daysCeil(long ms) {
        return (int) ((ms + DAY_MS - 1) / DAY_MS);
    }
}
