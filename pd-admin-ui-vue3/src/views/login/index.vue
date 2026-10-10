<template>
  <div class="login-page">
    <div class="login-toolbar">
      <ThemeSwitcher />
      <LangSelect size="text-18px" />
    </div>

    <div class="login-layout">
      <div class="login-brand">
        <img :src="loginHero" class="login-brand__hero-img" alt="物流配送" />
        <div class="login-brand__slogan">{{ t("login.slogan") }}</div>
        <div class="login-brand__copyright">{{ t("login.copyright") }}</div>
      </div>

      <div class="login-card">
        <div class="login-card__inner">
          <transition name="fade-slide" mode="out-in">
            <div v-if="component === 'login'" key="login" class="login-card__form">
              <div class="login-card__brand">
                <el-image :src="logoMark" class="login-card__logo" />
                <div class="login-card__brand-text">
                  <span class="login-card__brand-name">品达物流</span>
                  <span class="login-card__brand-en">FASTER AND SAFER</span>
                </div>
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
                    placeholder="{{ t('login.usernamePlaceholder') }}"
                    :prefix-icon="UserIcon"
                  />
                </el-form-item>

                <el-tooltip :visible="isCapsLock" :content="t('login.capsLock')" placement="right">
                  <el-form-item prop="password">
                    <el-input
                      v-model.trim="loginFormData.password"
                      placeholder="{{ t('login.passwordPlaceholder') }}"
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
                      placeholder="{{ t('login.captchaPlaceholder') }}"
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
import logoMark from "@/assets/images/logo-mark.png";
import loginHero from "@/assets/images/login-hero.png";
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
  username: "pinda",
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

// 品达物流演示账号
const demoAccounts = computed(() => [
  { username: "admin", label: t("login.accounts.admin") },
  { username: "dispatch", label: t("login.accounts.dispatch") },
  { username: "driver", label: t("login.accounts.driver") },
  { username: "customer", label: t("login.accounts.customer") },
]);

/**
 * 填充演示账号
 */
function fillDemoAccount(account: { username: string }): void {
  loginFormData.value.username = account.username;
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
$brand-red: #d7000f; // 品达品牌红：登录按钮、书法标语（参考原版登录页）

.login-page {
  // 品牌色阶：从运行时主题色派生，切换主题色板（ArcoD/AntD/ElementD）时整页跟随
  --brand-strong: color-mix(in srgb, var(--el-color-primary), #000 15%);
  --brand-deep: color-mix(in srgb, var(--el-color-primary), #000 45%);

  position: relative;
  display: flex;
  min-height: 100vh;
  overflow: auto;
  /* 浅色渐变底色；桌面端被品牌区背景与白色卡片完全覆盖，窄屏两区透明后透出 */
  background: linear-gradient(180deg, #ffffff 0%, #dbeafe 100%);
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
  /* 插画铺满整个品牌区，标语与版权叠加其上 */
  background: linear-gradient(180deg, #e8f4ff 0%, #cfe6ff 55%, #a8d0f5 100%);
  animation: login-pane-in 0.36s ease-out both;

  &__hero-img {
    position: absolute;
    inset: 0;
    z-index: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
    object-position: center;
  }

  &__slogan {
    position: absolute;
    z-index: 1;
    left: 7%;
    top: 50%;
    transform: translateY(-50%) rotate(-4deg);
    font-family: "STXingkai", "STKaiti", "KaiTi", "楷体", serif;
    font-size: 64px;
    font-weight: 700;
    line-height: 1.2;
    color: $brand-red;
    text-shadow:
      0 2px 4px rgb(255 255 255 / 60%),
      0 6px 20px rgb(255 255 255 / 45%);
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
    color: rgb(39 50 72 / 55%);
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
    gap: 12px;
    align-items: center;
    margin-bottom: 22px;
  }

  &__logo {
    width: 46px;
    height: 46px;
    flex-shrink: 0;
  }

  &__brand-text {
    display: flex;
    flex-direction: column;
    gap: 3px;
    min-width: 0;
  }

  &__brand-name {
    font-size: 22px;
    font-weight: 700;
    line-height: 1.15;
    color: $text-primary;
    letter-spacing: 0;
  }

  &__brand-en {
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 2.5px;
    line-height: 1;
    color: $brand-red;
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
    box-shadow: 0 12px 24px rgb(215 0 15 / 22%);

    &:hover,
    &:focus {
      background: #b8000c;
      border-color: #b8000c;
      box-shadow: 0 14px 28px rgb(215 0 15 / 28%);
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
  // 深蓝渐变底，插画之上叠加轻微暗化遮罩提升标语可读性
  background: linear-gradient(135deg, #0f1a33 0%, #0c1428 55%, #0a1020 100%);

  &::after {
    position: absolute;
    inset: 0;
    z-index: 0;
    content: "";
    background: linear-gradient(180deg, rgb(10 16 32 / 10%) 0%, rgb(10 16 32 / 35%) 100%);
  }

  &__slogan {
    color: $brand-red;
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

  &__brand-name {
    color: rgb(255 255 255 / 88%);
  }

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

  /* 深色品牌区的渐变与网格纹理不适合窄屏，透出页面底色 */
  .dark .login-brand {
    background: none;

    &::before,
    &::after {
      display: none;
    }
  }

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
