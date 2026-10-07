# 仓库推送规范（PUSH_GUIDE）

> 目的：**不要把 `.workbuddy/`、构建产物、密钥等不必要文件推到 Gitee 公网仓库**。
> Gitea（内网）可以存全量（含 `.workbuddy`），用独立分支隔离。

---

## 分支模型

| 分支 | 内容 | 推送到 | 说明 |
|------|------|--------|------|
| `master` | 干净代码（**不含** `.workbuddy/`、`.env`、`.idea/`、`target/*.jar` 等） | **Gitee + Gitea 都推** | 主分支，对外/对内都用它 |
| `local` | `master` 全量 + `.workbuddy/`（AI 工具目录、Nacos 配置备份等） | **仅推 Gitea（内网）** | 本地/内网全量备份分支，已设为受保护（禁 force-push） |

- 默认分支 = `master`（已确认）。
- `local` 分支已在 Gitea 上设为**分支保护**（禁止 force-push，保持历史稳定）。

---

## ⚠️ 铁律

**严禁把 `local` 反向合并进 `master`！**

```
# 错误示范（会污染 master，导致 .workbuddy 溜上 Gitee）
git checkout master && git merge local

# 正确：需要 .workbuddy 时，单向从 master 同步到 local
git checkout local && git merge master   # 单向，master → local
```

日常开发在 `master` 上提交；只有需要内网备份 `.workbuddy` 时，才 `git checkout local && git merge master` 单向同步，然后只推 Gitea。

---

## .gitignore 已覆盖（勿手改）

以下已被 `.gitignore` 忽略，正常 `git add` 不会带上：

```
.workbuddy/   .codebuddy/   .idea/   .env
logs/   log.path_IS_UNDEFINED/   node_modules/   dist/   .DS_Store   Thumbs.db
```

> 注意：`pd-admin-ui/build/` 是 vue-cli 的 **webpack 配置源码**，已被跟踪，**不能**忽略。

手动加文件时如果不确定，先跑：
```bash
git status --short -uall        # 看会不会带出 .workbuddy / node_modules
git check-ignore -v 路径        # 验证某路径是否被忽略
```

---

## 推送命令

```bash
# 1) master 推 Gitee（公网，必须干净）
git push origin master

# 2) master 推 Gitea（内网）
git push gitea master

# 3) local（仅含 .workbuddy 全量）推 Gitea，绝不推 Gitea 之外
git push gitea local
```

> 内网 Gitea 地址含内网 IP，本机若有 `HTTP_PROXY` 会把 `192.168.x.x` 也塞进代理导致推送挂死。
> 推送前请先绕开代理：
> ```bash
> git config http.http://192.168.20.130:3000.proxy ""
> # 或临时：env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY git push gitea master
> ```

---

## 如果 `.workbuddy` 不小心进了 master / 被推上 Gitee

1. **立即**从工作区取消跟踪（不删文件）：
   ```bash
   git rm -r --cached .workbuddy
   echo '.workbuddy/' >> .gitignore
   git add .gitignore && git commit -m "chore: 从跟踪中移除 .workbuddy"
   git push origin master
   ```
2. 若已 commit 进历史：用 `git filter-repo` 彻底清除（见项目 memory 记录），再 `git push -f`。
   > 注意：强推会改写历史，多人协作时需协调。
3. **不要**把 `local` 合并进 `master` 来"取回" `.workbuddy`——它本就只在 `local` 分支。

---

## 远端地址速查

- Gitee（公网）：`https://gitee.com/itxinfei/pinda-tms.git`
- Gitea（内网）：`http://192.168.20.130:3000/pinda/pinda-tms.git`（账号 `pinda`）

---

_最后更新：2026-10-07_
