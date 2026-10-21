<template>
  <div>
    <h3 text-center m-0 mb-20px>{{ t("login.resetPassword") }}</h3>
    <el-form ref="formRef" :model="model" :rules="rules" size="large">
      <!-- 用户名"-->
      <el-form-item prop="username">
        <el-input v-model.trim="model.username" :placeholder="t('login.username')" :aria-label="t('login.username')">
          <template #prefix>
            <el-icon><User /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="warning" class="w-full" @click="submit">
          {{ t("login.resetPassword") }}
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 自助重置需短信/邮件验证码通道，平台暂未开通；给出可执行的管理员路径，避免"点了没反应" -->
    <el-alert type="info" :closable="false" show-icon>
      <template #title>{{ t("login.adminResetTip") }}</template>
      <div class="reset-hint">{{ t("login.adminResetPath") }}</div>
    </el-alert>

    <div flex-center gap-10px>
      <el-text size="default">{{ t("login.thinkOfPasswd") }}</el-text>
      <el-link type="primary" underline="never" @click="toLogin">{{ t("login.login") }}</el-link>
    </div>
  </div>
</template>
<script setup lang="ts">
import { useI18n } from "vue-i18n";
import type { FormInstance } from "element-plus";

const { t } = useI18n();

/**
 * 切换登录或重置密码表单
 */
const emit = defineEmits(["update:modelValue"]);
/**
 * 切回登录表单
 */
const toLogin = () => emit("update:modelValue", "login");

const model = ref({
  username: "",
});

const rules = computed(() => {
  return {
    username: [
      {
        required: true,
        trigger: "blur",
        message: t("login.message.username.required"),
      },
    ],
  };
});

const formRef = ref<FormInstance>();

/**
 * 提交重置密码表单
 *
 * <p>平台未开通短信/邮件验证码通道，自助重置暂不可用：这里不静默失败，
 * 明确引导用户联系系统管理员（管理员可在「系统管理 → 用户管理 → 重置密码」完成）。</p>
 */
const submit = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  try {
    await ElMessageBox.alert(t("login.adminResetPath"), t("login.resetDone"), {
      confirmButtonText: t("login.login"),
      type: "info",
    });
  } finally {
    // 提示完回到登录页，避免停留在无后续动作的表单上
    toLogin();
  }
};
</script>

<style scoped lang="scss">
.reset-hint {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
}
</style>
