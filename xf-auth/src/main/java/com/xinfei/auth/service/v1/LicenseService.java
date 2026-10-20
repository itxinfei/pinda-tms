package com.xinfei.auth.service.v1;

import com.xinfei.auth.license.DecodedLicense;
import com.xinfei.auth.license.LicenseInfo;
import com.xinfei.auth.license.LicenseStatus;
import com.xinfei.auth.license.LicenseVerifier;
import com.xinfei.auth.license.TrialState;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 授权状态服务：组合 license 验签 + 试用计时，产出前端 banner 所需的
 * {@link LicenseInfo}。不阻断任何业务接口——仅用于展示提示。
 */
@Service
public class LicenseService {

    /** 未授权试用天数上限（超过则 TRIAL_EXPIRED，页面提示购买）。 */
    public static final int TRIAL_DAYS = 7;

    private final LicenseVerifier verifier;
    private final TrialState trialState = new TrialState();
    private final AtomicReference<LicenseInfo> last = new AtomicReference<>();

    public LicenseService() {
        this.verifier = new LicenseVerifier();
        this.trialState.ensure();
    }

    public LicenseInfo getStatus() {
        LicenseInfo info = new LicenseInfo();

        Optional<DecodedLicense> lic = verifier.verify();
        if (lic.isPresent()) {
            DecodedLicense d = lic.get();
            info.setStatus(LicenseStatus.VALID);
            info.setLicensee(d.getLicensee());
            info.setType(d.getType());
            info.setExpiresAt(d.getExpiresAt());
            info.setMessage("已授权" + (d.getLicensee() != null ? "：" + d.getLicensee() : ""));
            last.set(info);
            return info;
        }

        long days = (System.currentTimeMillis() - trialState.getFirstRunAt()) / 86_400_000L;
        if (days >= TRIAL_DAYS) {
            info.setStatus(LicenseStatus.TRIAL_EXPIRED);
            info.setTrialDaysLeft(0);
            info.setMessage("未授权：试用已到期（已运行 " + days + " 天）。商用请联系作者购买授权（QQ 747011882）。");
        } else {
            info.setStatus(LicenseStatus.TRIAL);
            int left = (int) (TRIAL_DAYS - days);
            info.setTrialDaysLeft(left);
            info.setMessage("未授权试用中，剩余 " + left + " 天。");
        }
        last.set(info);
        return info;
    }
}
