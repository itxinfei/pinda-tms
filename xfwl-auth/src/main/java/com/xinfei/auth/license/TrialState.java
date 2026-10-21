package com.xinfei.auth.license;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import lombok.Data;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 试用计时状态。首次启动写入 {@code ~/.xinfei/license-state.json}
 * （instanceId + firstRunAt），用于计算未授权实例的运行天数。
 *
 * <p>说明：技术用户删除该文件可重置计时——但这违反 BSL、且麻烦，
 * 作为"提示 + 威慑"足够；源码可见协议下无法彻底锁死，见 COMMERCIAL-FAQ.md。</p>
 */
public class TrialState {

    private static final Path FILE = Paths.get(
            System.getProperty("user.home"), ".xinfei", "license-state.json");
    private static final ObjectMapper M = new JsonMapper();

    private String instanceId;
    private long firstRunAt;

    public synchronized void ensure() {
        try {
            if (Files.exists(FILE)) {
                State s = M.readValue(Files.readString(FILE), State.class);
                this.instanceId = s.instanceId;
                this.firstRunAt = s.firstRunAt;
                return;
            }
        } catch (Exception ignored) {
            // 解析失败则重建
        }
        this.instanceId = UUID.randomUUID().toString();
        this.firstRunAt = System.currentTimeMillis();
        try {
            Files.createDirectories(FILE.getParent());
            M.writeValue(FILE.toFile(), new State(instanceId, firstRunAt));
        } catch (Exception ignored) {
            // 写失败仅影响计时精度，不阻断业务
        }
    }

    public String getInstanceId() {
        return instanceId;
    }

    public long getFirstRunAt() {
        return firstRunAt;
    }

    @Data
    public static class State {
        public String instanceId;
        public long firstRunAt;

        public State() {
        }

        public State(String instanceId, long firstRunAt) {
            this.instanceId = instanceId;
            this.firstRunAt = firstRunAt;
        }
    }
}
