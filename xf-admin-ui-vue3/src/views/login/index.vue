<template>
  <div class="login-page">
    <div class="login-toolbar">
      <ThemeSwitcher />
      <LangSelect size="text-18px" />
    </div>

    <div class="login-layout">
      <div class="login-brand">
        <div class="login-brand__scene" aria-hidden="true">
          <div class="login-brand__grid"></div>
          <div class="login-brand__glow"></div>

          <!-- 运单跟踪演示卡：可视化 TMS 核心能力「全程可视」 -->
          <div class="login-brand__panel">
            <div class="track-card">
              <header class="track-card__head">
                <span class="track-card__waybill">
                  {{ t("login.scene.waybill") }}<b>XFY2026101600327</b>
                </span>
                <span class="track-card__pill">
                  <i></i>{{ t("login.scene.status") }}
                </span>
              </header>

              <!-- 干线路线：流动虚线 + 红色运单标记沿线移动 -->
              <div class="track-card__route">
                <span class="track-route__city">{{ t("login.scene.origin") }}</span>
                <span class="track-route__line">
                  <i class="track-route__fill"></i>
                  <i class="track-route__marker">
                    <el-icon :size="16"><Truck /></el-icon>
                  </i>
                </span>
                <span class="track-route__city track-route__city--end">
                  {{ t("login.scene.dest") }}
                </span>
              </div>

              <!-- 里程碑步骤：随运输进度依次点亮 -->
              <ol class="track-card__steps">
                <li
                  v-for="(key, i) in ['pickup', 'lineHaul', 'delivering', 'signed']"
                  :key="key"
                  class="track-step"
                  :style="{ '--i': i }"
                >
                  <i class="track-step__dot"></i>
                  <span>{{ t("login.scene." + key) }}</span>
                </li>
              </ol>
            </div>

            <!-- 数据徽标：体现"系统在跑"的实时感 -->
            <div class="login-brand__chip login-brand__chip--a">
              <el-icon :size="18"><Box /></el-icon>
              <span>{{ t("login.scene.inTransit") }}<b>1,284</b></span>
            </div>
            <div class="login-brand__chip login-brand__chip--b">
              <el-icon :size="18"><Timer /></el-icon>
              <span>{{ t("login.scene.onTime") }}<b>98.6%</b></span>
            </div>
            <div class="login-brand__chip login-brand__chip--c">
              <el-icon :size="18"><CircleCheck /></el-icon>
              <span>{{ t("login.scene.signedToday") }}<b>3,462</b></span>
            </div>
          </div>
        </div>
        <div class="login-brand__slogan">{{ t("login.slogan") }}</div>
        <div class="login-brand__copyright">{{ t("login.copyright") }}</div>
      </div>

      <div class="login-card">
        <div class="login-card__inner">
          <transition name="fade-slide" mode="out-in">
            <div v-if="component === 'login'" key="login" class="login-card__form">
              <div class="login-card__brand">
                <img :src="logoFull" class="login-card__logo-full" alt="心飞物流" />
              </div>
              <h2 class="login-card__title">{{ t("login.title") }}</h2>
              <p class="login-card__desc">{{ t("login.desc") }}</p>

              <el-form
                ref="loginFormRef"
                :model="loginFormData"
                :rules="loginRules"
                size="large"
                :validate-on-rule-change="false"
              >
                <el-form-item prop="username">
                  <el-input
                    v-model.trim="loginFormData.username"
                    :placeholder="t('login.usernamePlaceholder')"
                    :prefix-icon="UserIcon"
                  />
                </el-form-item>

                <el-tooltip :visible="isCapsLock" :content="t('login.capsLock')" placement="right">
                  <el-form-item prop="password">
                    <el-input
                      v-model.trim="loginFormData.password"
                      :placeholder="t('login.passwordPlaceholder')"
                      type="password"
                      show-password
                      :prefix-icon="LockIcon"
                      @keyup="checkCapsLock"
                      @keyup.enter="handleLoginSubmit"
                    />
                  </el-form-item>
                </el-tooltip>

                <el-form-item prop="captchaCode">
                  <div style="display: flex; gap: 12px; width: 100%;">
                    <el-input
                      v-model.trim="loginFormData.captchaCode"
                      :placeholder="t('login.captchaPlaceholder')"
                      style="flex: 1; min-width: 0;"
                      @keyup.enter="handleLoginSubmit"
                    />
                    <div 
                      class="login-card__captcha" 
                      @click="getCaptcha"
                      style="
                        width: 120px;
                        height: 40px;
                        flex-shrink: 0;
                        overflow: hidden;
                        cursor: pointer;
                        background: var(--el-fill-color-blank);
                        border: 1px solid var(--el-border-color);
                        border-radius: var(--el-border-radius-base);
                        display: flex;
                        align-items: center;
                        justify-content: center;
                      "
                    >
                      <el-icon v-if="codeLoading" class="is-loading" :size="16">
                        <Loading />
                      </el-icon>
                      <img v-else-if="captchaBase64" :src="captchaBase64" alt="验证码" style="width: 100%; height: 100%; object-fit: contain;" />
                      <el-icon v-else :size="16"><Refresh /></el-icon>
                    </div>
                  </div>
                </el-form-item>

                <div class="login-card__options">
                  <el-checkbox v-model="loginFormData.rememberMe">{{ t("login.rememberMe") }}</el-checkbox>
                </div>

                <el-button
                  :loading="loading"
                  type="primary"
                  size="large"
                  class="login-card__submit"
                  @click="handleLoginSubmit"
                >
                  {{ t("login.login") }}
                </el-button>

                <div class="login-card__demo">
                  <div class="login-card__demo-hint" @click="fillDemoAccount">
                    {{ t("login.demoHint") }}
                  </div>
                </div>
              </el-form>
            </div>

            <ResetPwd
              v-else
              key="resetPwd"
              class="login-card__form"
              @update:model-value="component = $event"
            />
          </transition>
        </div>

        <div class="login-footer">{{ t("login.footer") }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: "LoginPage", inheritAttrs: false });

import { Clock, Lock, Loading, Refresh, User } from "@element-plus/icons-vue";
import type { FormInstance } from "element-plus";
import AuthAPI from "@/api/auth";
import type { LoginRequest } from "@/api/auth";
import router from "@/router";
import { useUserStore } from "@/stores";
import { AuthStorage } from "@/utils/auth";
import ResetPwd from "./components/ResetPwd.vue";
import logoFull from "@/assets/images/logo-full.png";
import { useI18n } from "vue-i18n";

const { t } = useI18n();
const userStore = useUserStore();
const route = useRoute();
const component = ref<"login" | "resetPwd">("login");

const loginFormRef = ref<FormInstance>();
const loading = ref(false);
const isCapsLock = ref(false);
const captchaBase64 = ref<string>();
const codeLoading = ref(false);

const UserIcon = markRaw(User);
const LockIcon = markRaw(Lock);

const loginFormData = ref<LoginRequest>({
  username: "demo",
  password: "123456",
  captchaId: "",
  captchaCode: "",
  rememberMe: AuthStorage.getRememberMe(),
});

const loginRules = computed(() => ({
  username: [{ required: true, trigger: "blur", message: t("login.message.username.required") }],
  password: [
    { required: true, trigger: "blur", message: t("login.message.password.required") },
    { min: 6, message: t("login.message.password.min"), trigger: "blur" },
  ],
  captchaCode: [{ required: true, trigger: "blur", message: t("login.message.captchaCode.required") }],
}));

// 四端统一演示账号 demo/123456（必须是 sys_user 中真实存在的账号，口径见《57-账号密码与凭据总表》）
/**
 * 一键填充演示账号
 */
function fillDemoAccount(): void {
  loginFormData.value.username = "demo";
  loginFormData.value.password = "123456";
}

/**
 * 刷新验证码
 */
function getCaptcha() {
  codeLoading.value = true;
  AuthAPI.getCaptcha()
    .then((d) => {
      loginFormData.value.captchaId = d.captchaId;
      captchaBase64.value = d.captchaBase64;
    })
    .finally(() => (codeLoading.value = false));
}

/**
 * 提交登录表单
 */
async function handleLoginSubmit() {
  const valid = await loginFormRef.value?.validate().then(
    () => true,
    () => false
  );
  if (!valid) return;

  loading.value = true;
  try {
    await userStore.login(loginFormData.value).then(
      async () => {
        const redirectPath = (route.query.redirect as string) || "/";
        await router.push(decodeURIComponent(redirectPath));
      },
      () => getCaptcha()
    );
  } finally {
    loading.value = false;
  }
}

/**
 * 检测大写锁定是否打开
 */
function checkCapsLock(event: KeyboardEvent) {
  if (event instanceof KeyboardEvent) {
    isCapsLock.value = event.getModifierState("CapsLock");
  }
}

/**
 * 切换登录区展示的表单
 */
function showForm(type: "resetPwd") {
  component.value = type;
}

onMounted(() => getCaptcha());
</script>

<style lang="scss" scoped>
$text-primary: #273248;
$text-secondary: #667085;
$text-muted: #98a2b3;
$input-h: 44px;
// 心飞品牌主色 #FF0033：仅用于登录按钮（红色只留这一处，不与左侧品牌区抢视觉）
$brand-red: #FF0033;
// 左侧品牌区配色（深蓝夜航）：与 hero 图渐变一致
$navy-top: #1e3a5f;
$navy-mid: #16294a;
$navy-bot: #0e1c33;

.login-page {
  // 品牌色阶：从运行时主题色派生，切换主题色板（ArcoD/AntD/ElementD）时整页跟随
  --brand-strong: color-mix(in srgb, var(--el-color-primary), #000 15%);
  --brand-deep: color-mix(in srgb, var(--el-color-primary), #000 45%);

  position: relative;
  display: flex;
  min-height: 100vh;
  overflow: auto;
  /* 窄屏隐藏品牌区后透出的页面底色（取浅蓝调，与深蓝品牌区分明） */
  background: linear-gradient(180deg, #f7f9fc 0%, #dce7f5 100%);
}

.login-toolbar {
  position: fixed;
  top: 28px;
  right: 32px;
  z-index: 10;
  display: flex;
  gap: 12px;
  align-items: center;

  :deep(*) {
    cursor: pointer;
  }
}

.login-layout {
  display: flex;
  flex: 1;
  min-height: 100%;
}

.login-brand {
  position: relative;
  flex: 0 0 65%;
  min-height: 100vh;
  overflow: hidden;
  /* 深蓝夜航底：纯 CSS 场景，红色只在终点定位针与品牌标识处点缀 */
  background: linear-gradient(160deg, $navy-top 0%, $navy-mid 55%, $navy-bot 100%);
  animation: login-pane-in 0.36s ease-out both;

  /* CSS 动画场景层（网格 + 光晕 + 行驶轨迹 + 货车） */
  &__scene {
    position: absolute;
    inset: 0;
    z-index: 0;
    overflow: hidden;
  }

  /* 几何网格：暗示运力网络，缓慢横向漂移 */
  &__grid {
    position: absolute;
    inset: -20% -60%;
    background:
      repeating-linear-gradient(90deg, transparent 0 87px, rgb(255 255 255 / 5%) 87px 89px),
      repeating-linear-gradient(0deg, transparent 0 87px, rgb(255 255 255 / 5%) 87px 89px);
    animation: brand-grid-drift 26s linear infinite;
  }

  /* 顶部品牌红光晕：呼应主色，但极低透明度不刺眼 */
  &__glow {
    position: absolute;
    top: -22%;
    left: -10%;
    width: 72%;
    aspect-ratio: 1;
    background: radial-gradient(circle, rgb(255 0 51 / 22%) 0%, transparent 62%);
    animation: brand-glow-breathe 7s ease-in-out infinite;
  }

  /* 运单跟踪演示面板（玻璃拟态卡 + 浮动数据徽标） */
  &__panel {
    position: absolute;
    z-index: 1;
    top: 46%;
    right: 7%;
    left: 7%;
  }

  /* ---------- 运单跟踪卡 ---------- */
  .track-card {
    width: min(520px, 100%);
    padding: 18px 22px 20px;
    margin: 0 auto;
    background: rgb(255 255 255 / 8%);
    border: 1px solid rgb(255 255 255 / 14%);
    border-radius: 14px;
    box-shadow: 0 18px 44px rgb(5 10 25 / 35%);
    backdrop-filter: blur(14px);
    animation: track-card-in 0.6s ease-out 0.15s both;
  }

  .track-card__head {
    display: flex;
    gap: 12px;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 18px;
  }

  .track-card__waybill {
    display: inline-flex;
    gap: 8px;
    align-items: baseline;
    min-width: 0;
    font-size: 12px;
    color: rgb(255 255 255 / 60%);

    b {
      overflow: hidden;
      font-family: ui-monospace, "Cascadia Mono", Consolas, monospace;
      font-size: 14px;
      font-weight: 600;
      color: #fff;
      letter-spacing: 0.5px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  /* 状态徽标：品牌红，是卡片的"在途"信号 */
  .track-card__pill {
    display: inline-flex;
    flex-shrink: 0;
    gap: 6px;
    align-items: center;
    padding: 3px 10px;
    font-size: 12px;
    color: #fff;
    background: linear-gradient(135deg, #ff2a55 0%, $brand-red 100%);
    border-radius: 999px;
    box-shadow: 0 4px 12px rgb(255 0 51 / 30%);

    i {
      width: 6px;
      height: 6px;
      background: #fff;
      border-radius: 50%;
      animation: brand-pill-blink 1.2s ease-in-out infinite;
    }
  }

  .track-card__route {
    display: flex;
    gap: 14px;
    align-items: center;
  }

  .track-route__city {
    flex-shrink: 0;
    max-width: 96px;
    overflow: hidden;
    font-size: 13px;
    color: rgb(255 255 255 / 88%);
    text-overflow: ellipsis;
    white-space: nowrap;

    &--end {
      text-align: right;
    }
  }

  .track-route__line {
    position: relative;
    flex: 1;
    height: 4px;
    background: rgb(255 255 255 / 16%);
    border-radius: 2px;

    /* 流动虚线纹理：暗示线路活跃 */
    &::before {
      position: absolute;
      inset: 0;
      content: "";
      opacity: 0.45;
      background: repeating-linear-gradient(
        90deg,
        rgb(255 255 255 / 40%) 0 10px,
        transparent 10px 20px
      );
      border-radius: inherit;
      animation: brand-dash-flow 1.2s linear infinite;
    }

    /* 运输进度填充：品牌红 */
    .track-route__fill {
      position: absolute;
      top: 0;
      left: 0;
      height: 100%;
      background: linear-gradient(90deg, rgb(255 0 51 / 50%) 0%, $brand-red 100%);
      border-radius: inherit;
      box-shadow: 0 0 10px rgb(255 0 51 / 45%);
      animation: brand-track-fill 14s ease-in-out infinite;
    }

    /* 运单标记：品牌红圆徽 + 货车图标，沿线路移动，是画面的视觉焦点 */
    .track-route__marker {
      position: absolute;
      top: 50%;
      left: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      width: 30px;
      height: 30px;
      color: #fff;
      background: linear-gradient(135deg, #ff2a55 0%, $brand-red 100%);
      border-radius: 50%;
      box-shadow:
        0 0 0 6px rgb(255 0 51 / 18%),
        0 8px 20px rgb(0 0 0 / 40%);
      transform: translate(-50%, -50%);
      animation: brand-track-marker 14s ease-in-out infinite;
    }

    /* 到达终点的脉冲 */
    &::after {
      position: absolute;
      top: 50%;
      right: -3px;
      width: 8px;
      height: 8px;
      content: "";
      background: $brand-red;
      border-radius: 50%;
      transform: translateY(-50%);
      animation: brand-end-pop 14s ease-in-out infinite;
    }
  }

  .track-card__steps {
    display: flex;
    justify-content: space-between;
    padding: 0;
    margin: 16px 2px 0;
    list-style: none;
  }

  .track-step {
    display: flex;
    gap: 6px;
    align-items: center;
    font-size: 12px;
    color: rgb(255 255 255 / 42%);

    &__dot {
      width: 8px;
      height: 8px;
      flex-shrink: 0;
      background: rgb(255 255 255 / 28%);
      border-radius: 50%;
      animation: brand-step-done 14s linear infinite;
    }
  }

  /* ---------- 浮动数据徽标 ---------- */
  &__chip {
    position: absolute;
    display: inline-flex;
    gap: 8px;
    align-items: center;
    padding: 10px 14px;
    font-size: 12px;
    color: rgb(255 255 255 / 65%);
    background: rgb(255 255 255 / 8%);
    border: 1px solid rgb(255 255 255 / 12%);
    border-radius: 10px;
    box-shadow: 0 10px 26px rgb(5 10 25 / 30%);
    backdrop-filter: blur(10px);
    animation: brand-chip-float 6s ease-in-out infinite;

    .el-icon {
      color: $brand-red;
    }

    b {
      margin-left: 4px;
      font-size: 15px;
      font-weight: 700;
      color: #fff;
      font-variant-numeric: tabular-nums;
    }

    &--a {
      top: -52px;
      right: 6%;
    }

    &--b {
      bottom: -56px;
      left: 4%;
      animation-delay: -2s;
    }

    &--c {
      right: 4%;
      bottom: -56px;
      animation-delay: -4s;
    }
  }

  &__slogan {
    position: absolute;
    z-index: 1;
    left: 7%;
    top: 12%;
    font-family: "STXingkai", "STKaiti", "KaiTi", "楷体", serif;
    font-size: 68px;
    font-weight: 700;
    line-height: 1.2;
    color: #fff;
    text-shadow:
      0 2px 4px rgb(0 0 0 / 30%),
      0 6px 22px rgb(0 0 0 / 45%);
    letter-spacing: 8px;
    white-space: nowrap;
    user-select: none;
  }

  &__copyright {
    position: absolute;
    z-index: 1;
    bottom: 26px;
    left: 0;
    right: 0;
    font-size: 12px;
    text-align: center;
    color: rgb(255 255 255 / 55%);
  }
}

/* 网格缓慢漂移 */
@keyframes brand-grid-drift {
  from {
    transform: translate3d(0, 0, 0);
  }

  to {
    transform: translate3d(87px, -87px, 0);
  }
}

/* 品牌红光晕呼吸 */
@keyframes brand-glow-breathe {
  0%,
  100% {
    opacity: 0.75;
    transform: scale(1);
  }

  50% {
    opacity: 1;
    transform: scale(1.12);
  }
}

/* 演示卡入场 */
@keyframes track-card-in {
  from {
    opacity: 0;
    transform: translateY(14px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 状态徽标呼吸点 */
@keyframes brand-pill-blink {
  0%,
  100% {
    opacity: 1;
  }

  50% {
    opacity: 0.25;
  }
}

/* 虚线流动 */
@keyframes brand-dash-flow {
  to {
    background-position-x: 20px;
  }
}

/*
 * 主循环 14s：0-71%（10s）运输中 → 71-88% 到达停留 → 88-100% 软复位。
 * 标记与进度线同步；steps 用 var(--i) 在关键帧内错峰点亮，保证每轮循环同步。
 */
@keyframes brand-track-marker {
  0% {
    left: 0;
    opacity: 1;
  }

  71%,
  88% {
    left: 100%;
    opacity: 1;
  }

  94% {
    left: 100%;
    opacity: 0;
  }

  95% {
    left: 0;
    opacity: 0;
  }

  100% {
    left: 0;
    opacity: 1;
  }
}

@keyframes brand-track-fill {
  0% {
    width: 0;
    opacity: 1;
  }

  71%,
  88% {
    width: 100%;
    opacity: 1;
  }

  94% {
    width: 100%;
    opacity: 0;
  }

  95% {
    width: 0;
    opacity: 0;
  }

  100% {
    width: 0;
    opacity: 1;
  }
}

/* 里程碑点：随进度依次点亮（0-23.8-47.6-71.4% 四等分运输段） */
@keyframes brand-step-done {
  0%,
  calc(var(--i) * 23.8% + 0.5%) {
    background: rgb(255 255 255 / 28%);
    box-shadow: none;
  }

  calc(var(--i) * 23.8% + 3%),
  84% {
    background: #fff;
    box-shadow: 0 0 0 3px rgb(255 255 255 / 18%);
  }

  90%,
  100% {
    background: rgb(255 255 255 / 28%);
    box-shadow: none;
  }
}

/* 到达终点：脉冲一下 */
@keyframes brand-end-pop {
  0%,
  68% {
    box-shadow: 0 0 0 0 rgb(255 0 51 / 0%);
    transform: translateY(-50%) scale(1);
  }

  72%,
  84% {
    box-shadow: 0 0 0 7px rgb(255 0 51 / 35%);
    transform: translateY(-50%) scale(1.15);
  }

  90%,
  100% {
    box-shadow: 0 0 0 0 rgb(255 0 51 / 0%);
    transform: translateY(-50%) scale(1);
  }
}

/* 数据徽标缓慢浮动 */
@keyframes brand-chip-float {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-8px);
  }
}

.login-card {
  position: relative;
  z-index: 1;
  display: flex;
  flex: 0 0 35%;
  flex-direction: column;
  align-items: center;
  padding: 0 0 32px;
  background: #fff;
  animation: login-pane-in 0.36s ease-out 0.04s both;

  &__inner {
    box-sizing: border-box;
    display: flex;
    flex: 1;
    flex-direction: column;
    justify-content: center;
    width: 100%;
    max-width: 430px;
    padding: 0 20px;
  }

  &__form {
    width: 100%;
  }

  &__brand {
    display: flex;
    justify-content: center;
    margin-bottom: 26px;
  }

  /* Logo 放大：登录页门面，品牌识别要够醒目 */
  &__logo-full {
    height: 68px;
    width: auto;
    display: block;
  }

  &__title {
    margin: 0 0 10px;
    font-size: 28px;
    font-weight: 750;
    line-height: 1.1;
    color: $text-primary;
    letter-spacing: 0;
  }

  &__desc {
    margin: 10px 0 28px;
    font-size: 15px;
    color: $text-muted;
  }

  &__prefix-icon {
    display: inline-flex;
    width: 14px;
    height: 14px;
    color: var(--el-text-color-placeholder);
  }

  &__captcha {
    box-sizing: border-box;
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    width: 108px;
    height: $input-h;
    overflow: hidden;
    cursor: pointer;
    background: var(--el-fill-color-blank);
    border: 1px solid var(--el-border-color);
    border-radius: var(--el-border-radius-base);
    transition: border-color 0.2s;

    &:hover {
      border-color: var(--el-color-primary);
    }

    img {
      display: block;
      width: 100%;
      height: 100%;
      object-fit: contain;
    }
  }

  &__options {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 22px;
    font-size: 14px;
    color: $text-secondary;

    a {
      font-weight: 500;
      color: var(--el-color-primary);
      cursor: pointer;
      transition: opacity 0.15s;

      &:hover {
        opacity: 0.8;
      }
    }
  }

  &__submit {
    width: 100%;
    height: 44px;
    font-size: 16px;
    font-weight: 600;
    background: $brand-red;
    border-color: $brand-red;
    border-radius: 8px;
    box-shadow: 0 12px 24px rgb(255 0 51 / 20%);

    &:hover,
    &:focus {
      background: #e6002e;
      border-color: #e6002e;
      box-shadow: 0 14px 28px rgb(255 0 51 / 26%);
    }

    &:focus,
    &:focus-visible {
      outline: none;
    }
  }

  &__demo {
    margin-top: 20px;

    &-title {
      margin-bottom: 10px;
      font-size: 12px;
      color: $text-muted;
    }

    &-hint {
      padding: 8px 12px;
      font-size: 12px;
      line-height: 1.5;
      color: $text-secondary;
      text-align: center;
      cursor: pointer;
      background: color-mix(in srgb, var(--el-color-primary) 5%, transparent);
      border: 1px dashed color-mix(in srgb, var(--el-color-primary) 30%, transparent);
      border-radius: 8px;
      transition:
        color 0.2s,
        background 0.2s,
        border-color 0.2s;

      &:hover {
        color: var(--el-color-primary);
        background: color-mix(in srgb, var(--el-color-primary) 10%, transparent);
        border-color: color-mix(in srgb, var(--el-color-primary) 50%, transparent);
      }
    }

    &-chip {
      height: 28px;
      padding: 0 12px;
      font-size: 12px;
      color: $text-secondary;
      cursor: pointer;
      background: color-mix(in srgb, var(--el-color-primary) 5%, transparent);
      border: 1px solid color-mix(in srgb, var(--el-color-primary) 16%, transparent);
      border-radius: 999px;
      transition:
        color 0.2s,
        background 0.2s,
        border-color 0.2s;

      &:hover {
        color: var(--el-color-primary);
        background: color-mix(in srgb, var(--el-color-primary) 10%, transparent);
        border-color: color-mix(in srgb, var(--el-color-primary) 40%, transparent);
      }
    }
  }

  /* 半透明白底按钮，在渐变背景上保持可见；hover 时提实并加深投影 */
  &__alt {
    margin-top: 28px;

    &-divider {
      display: flex;
      gap: 12px;
      align-items: center;
      margin-bottom: 14px;
      font-size: 13px;
      color: $text-muted;

      &::before,
      &::after {
        flex: 1;
        height: 1px;
        content: "";
        background: color-mix(in srgb, var(--brand-deep) 12%, transparent);
      }
    }

    &-btn {
      display: flex;
      flex: 1;
      gap: 8px;
      align-items: center;
      justify-content: center;
      height: 44px;
      padding: 0;
      font-size: 14px;
      color: $text-secondary;
      cursor: pointer;
      background: rgb(255 255 255 / 80%);
      border: 1px solid color-mix(in srgb, var(--brand-deep) 10%, transparent);
      border-radius: 8px;
      box-shadow: 0 1px 2px color-mix(in srgb, var(--brand-deep) 6%, transparent);
      backdrop-filter: blur(4px);
      transition:
        color 0.2s,
        background 0.2s,
        border-color 0.2s,
        box-shadow 0.2s;

      &:hover {
        color: var(--el-color-primary);
        background: #fff;
        border-color: color-mix(in srgb, var(--el-color-primary) 35%, transparent);
        box-shadow: 0 4px 12px color-mix(in srgb, var(--el-color-primary) 14%, transparent);
      }
    }

    &-icon {
      width: 16px;
      height: 16px;
    }
  }
}

:deep(.el-form-item) {
  margin-bottom: 14px;
}

:deep(.el-input__wrapper) {
  height: $input-h;
}

.login-footer {
  flex-shrink: 0;
  font-size: 12px;
  color: $text-muted;
}

.dark .login-page {
  background: #0b1020;
}

.dark .login-brand {
  // 深蓝品牌区：网格纹理已足够表达层次，叠加轻微暗化遮罩提升标语可读性
  background: linear-gradient(160deg, $navy-top 0%, $navy-mid 55%, $navy-bot 100%);

  &::after {
    position: absolute;
    inset: 0;
    z-index: 0;
    content: "";
    background: linear-gradient(180deg, rgb(10 16 32 / 18%) 0%, rgb(10 16 32 / 42%) 100%);
  }

  &__slogan {
    color: #fff;
    text-shadow:
      0 2px 4px rgb(0 0 0 / 35%),
      0 6px 22px rgb(0 0 0 / 45%);
  }

  &__copyright {
    color: rgb(255 255 255 / 45%);
  }
}

.dark .login-card {
  background: #0b1020;

  &__title {
    color: rgb(255 255 255 / 85%);
  }

  &__desc {
    color: rgb(255 255 255 / 30%);
  }

  &__demo-title {
    color: rgb(255 255 255 / 35%);
  }

  &__demo-chip {
    color: rgb(255 255 255 / 70%);
    background: rgb(255 255 255 / 6%);
    border-color: rgb(255 255 255 / 14%);

    &:hover {
      color: #fff;
      background: rgb(255 255 255 / 12%);
      border-color: rgb(255 255 255 / 28%);
    }
  }

  &__demo-hint {
    color: rgb(255 255 255 / 65%);
    background: rgb(255 255 255 / 6%);
    border-color: rgb(255 255 255 / 18%);

    &:hover {
      color: #fff;
      background: rgb(255 255 255 / 12%);
      border-color: rgb(255 255 255 / 35%);
    }
  }

  &__alt-divider {
    color: rgb(255 255 255 / 20%);

    &::before,
    &::after {
      background: rgb(255 255 255 / 12%);
    }
  }

  &__alt-btn {
    color: rgb(255 255 255 / 65%);
    background: rgb(255 255 255 / 6%);
    border-color: rgb(255 255 255 / 12%);
    box-shadow: none;

    &:hover {
      color: #fff;
      background: rgb(255 255 255 / 12%);
      border-color: rgb(255 255 255 / 24%);
    }
  }
}

.dark .login-footer {
  color: rgb(255 255 255 / 15%);
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.2s ease;
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

@keyframes login-pane-in {
  from {
    opacity: 0;
    filter: blur(4px);
  }

  to {
    opacity: 1;
    filter: blur(0);
  }
}

/* 无障碍：系统开启"减少动态效果"时停掉品牌区所有动画，定格在运输中段 */
@media (prefers-reduced-motion: reduce) {
  .login-brand {
    &__grid,
    &__glow,
    .track-card,
    .track-card__pill i,
    .track-route__line::before,
    .track-route__line::after,
    .track-route__fill,
    .track-route__marker,
    .track-step__dot,
    &__chip {
      animation: none;
    }

    .track-route__fill {
      width: 46%;
    }

    .track-route__marker {
      left: 46%;
    }
  }
}

@media (max-width: 1024px) {
  .login-layout {
    flex-direction: column;
  }

  .login-toolbar {
    position: absolute;
    top: 37px;
  }

  /* 窄屏收起左侧插画品牌区，登录卡片占满视口 */
  .login-brand {
    display: none;
  }

  /* 窄屏已整块隐藏 .login-brand，其子元素样式无需再处理 */

  /* 卡片占满剩余高度并透出页面渐变，inner 沿用全局的垂直居中，页脚沉底 */
  .login-card {
    flex: 1;
    padding: 40px 48px 32px;
    background: transparent;
  }

  .dark .login-card {
    background: transparent;
  }
}

@media (max-width: 640px) {
  .login-toolbar {
    top: 33px;
    right: 20px;
  }

  .login-brand {
    padding: 24px 24px 28px;
  }

  .login-card {
    padding: 32px 24px 24px;

    &__inner {
      width: 100%;
      padding: 0;
    }
  }
}
</style>
