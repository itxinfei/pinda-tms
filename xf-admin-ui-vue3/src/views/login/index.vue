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
          <div class="login-brand__route">
            <span class="login-brand__node" style="--i: 0"></span>
            <span class="login-brand__node" style="--i: 1"></span>
            <span class="login-brand__node" style="--i: 2"></span>
            <span class="login-brand__node login-brand__node--end" style="--i: 3"></span>
          </div>
          <div class="login-brand__truck">
            <span class="login-brand__truck-box"></span>
            <span class="login-brand__truck-cab"></span>
            <span class="login-brand__truck-wheel login-brand__truck-wheel--rear"></span>
            <span class="login-brand__truck-wheel login-brand__truck-wheel--front"></span>
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

  /* 运输轨迹：从左下贯穿到右上 */
  &__route {
    position: absolute;
    left: 6%;
    bottom: 22%;
    width: 78%;
    height: 42%;

    &::before {
      position: absolute;
      left: 0;
      bottom: 0;
      width: 100%;
      height: 3px;
      content: "";
      background: linear-gradient(90deg, transparent 0%, rgb(255 255 255 / 55%) 100%);
      border-radius: 2px;
    }
  }

  /* 轨迹上的 4 个定位节点，依次点亮 */
  &__node {
    position: absolute;
    bottom: -7px;
    left: calc(var(--i) * 31%);
    width: 14px;
    height: 14px;
    background: rgb(255 255 255 / 85%);
    border: 3px solid rgb(255 255 255 / 25%);
    border-radius: 50%;
    animation: brand-node-pulse 3.2s ease-in-out infinite;
    animation-delay: calc(var(--i) * 0.55s);

    /* 终点：品牌红，作为画面里唯一的红色强调 */
    &--end {
      background: $brand-red;
      border-color: rgb(255 255 255 / 30%);
      box-shadow: 0 0 0 0 rgb(255 0 51 / 60%);
    }
  }

  /* CSS 货车：沿轨迹匀速行驶 + 轻微车身起伏 */
  &__truck {
    position: absolute;
    bottom: 22%;
    left: 6%;
    width: 76px;
    height: 40px;
    animation: brand-truck-run 13s linear infinite, brand-truck-bob 1.6s ease-in-out infinite;

    /* 货箱 */
    &-box {
      position: absolute;
      bottom: 9px;
      left: 0;
      width: 40px;
      height: 26px;
      background: linear-gradient(160deg, #fff 0%, #d8e2f0 100%);
      border-radius: 3px 4px 0 0;
      box-shadow: 0 4px 12px rgb(0 0 0 / 25%);
    }

    /* 驾驶室 */
    &-cab {
      position: absolute;
      bottom: 9px;
      left: 40px;
      width: 32px;
      height: 20px;
      background: linear-gradient(160deg, #ff2a55 0%, $brand-red 100%);
      border-radius: 8px 4px 0 0;
      box-shadow: 0 4px 12px rgb(0 0 0 / 25%);
    }

    /* 车轮 */
    &-wheel {
      position: absolute;
      bottom: 0;
      width: 13px;
      height: 13px;
      background: #0b1220;
      border: 2px solid rgb(255 255 255 / 55%);
      border-radius: 50%;
      animation: brand-wheel-spin 0.7s linear infinite;

      &--rear {
        left: 8px;
      }

      &--front {
        left: 52px;
      }
    }
  }

  &__slogan {
    position: absolute;
    z-index: 1;
    left: 7%;
    top: 34%;
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

/* 定位节点依次点亮 */
@keyframes brand-node-pulse {
  0%,
  70%,
  100% {
    box-shadow: 0 0 0 0 rgb(255 255 255 / 45%);
  }

  20% {
    box-shadow: 0 0 0 9px rgb(255 255 255 / 0%);
  }
}

/* 货车沿轨迹行驶 */
@keyframes brand-truck-run {
  0% {
    transform: translate3d(0, 0, 0);
  }

  100% {
    transform: translate3d(62vw, -24vh, 0);
  }
}

/* 车身轻微起伏，模拟行驶颠簸 */
@keyframes brand-truck-bob {
  0%,
  100% {
    margin-bottom: 0;
  }

  50% {
    margin-bottom: 4px;
  }
}

/* 车轮滚动 */
@keyframes brand-wheel-spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
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

/* 无障碍：系统开启"减少动态效果"时停掉品牌区所有动画 */
@media (prefers-reduced-motion: reduce) {
  .login-brand {
    &__grid,
    &__glow,
    &__node,
    &__truck,
    &__truck-wheel {
      animation: none;
    }

    &__truck {
      transform: translate3d(24vw, -10vh, 0);
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
