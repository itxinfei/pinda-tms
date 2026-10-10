package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.itheima.pinda.entity.ChargeRule;
import com.itheima.pinda.mapper.ChargeRuleMapper;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.runtime.KieContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 重新加载数据库中的规则，创建Drools相关对象
 */
@Service
public class ReloadDroolsRulesService {
    @Autowired
    private ChargeRuleMapper chargeRuleMapper;

    // 使用volatile保证可见性，使用AtomicReference保证原子性
    private volatile KieContainer kieContainer;
    private final Object reloadLock = new Object();

    /**
     * 查询系统计费规则（rule_key='tms'）。
     * 启动/调用时租户回落0，多租户插件自动补 tenant_id=0，恰好加载系统默认规则。
     *
     * @return 计费规则列表
     */
    public List<ChargeRule> loadRules() {
        QueryWrapper<ChargeRule> wrapper = new QueryWrapper<>();
        wrapper.eq("rule_key", "tms");
        return chargeRuleMapper.selectList(wrapper);
    }

    /**
     * 重新创建KieContainer对象
     */
    public void reload() {
        KieContainer newContainer;
        synchronized (reloadLock) {
            newContainer = this.loadContainerFromString(loadRules());
        }
        this.kieContainer = newContainer;
    }

    /**
     * 获取KieContainer实例，如果尚未加载则返回null
     */
    public KieContainer getKieContainer() {
        return kieContainer;
    }

    /**
     * 根据规则内容创建KieContainer对象
     *
     * @param ruleList 计费规则列表
     * @return KieContainer
     */
    public KieContainer loadContainerFromString(List<ChargeRule> ruleList) {
        KieServices ks = KieServices.Factory.get();
        KieRepository kr = ks.getRepository();
        KieFileSystem kfs = ks.newKieFileSystem();//文件系统

        for (ChargeRule rule : ruleList) {
            String drl = rule.getContent();
            kfs.write("src/main/resources/" + drl.hashCode() + ".drl", drl);
        }

        KieBuilder kb = ks.newKieBuilder(kfs);
        kb.buildAll();

        KieContainer kieContainer = ks.newKieContainer(kr.getDefaultReleaseId());

        return kieContainer;
    }
}
