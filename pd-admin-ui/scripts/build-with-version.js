/**
 * 构建前置脚本：把 git 提交号与构建时间注入为 VUE_APP_* 环境变量，
 * 供 BuildBadge 组件在界面角落显示“当前线上跑的是哪份代码”。
 *
 * 用法（package.json 已接好）：
 *   node scripts/build-with-version.js --mode docker
 *   node scripts/build-with-version.js            # 默认 production
 *
 * 变量优先级：环境变量 GIT_SHA / BUILD_TIME 优先（CI 可显式传入），
 * 否则回退到 git 命令 / 当前时间。取不到时填 unknown，不会让构建失败。
 */
const { execSync, spawnSync } = require('child_process')
const path = require('path')

function getSha() {
  if (process.env.GIT_SHA) return process.env.GIT_SHA
  try {
    return execSync('git rev-parse --short HEAD').toString().trim()
  } catch (e) {
    return 'unknown'
  }
}

function getTime() {
  if (process.env.BUILD_TIME) return process.env.BUILD_TIME
  return new Date().toISOString()
}

process.env.VUE_APP_GIT_SHA = getSha()
process.env.VUE_APP_BUILD_TIME = getTime()

// 使用与 @vue/cli-service 同版本的 cli 入口，避免依赖 PATH 上的 vue-cli-service
const cli = require.resolve('@vue/cli-service/bin/vue-cli-service.js')
const args = ['build', ...process.argv.slice(2)]

const result = spawnSync(process.execPath, [cli, ...args], {
  stdio: 'inherit',
  shell: false,
  env: process.env
})

process.exit(result.status === null ? 1 : result.status)
