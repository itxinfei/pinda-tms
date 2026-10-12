<template>
  <div class="profile-page">
    <section class="profile-hero">
      <div class="profile-hero__body">
        <div class="profile-avatar">
          <el-avatar :src="displayAvatar" :size="72">
            <el-icon><UserFilled /></el-icon>
          </el-avatar>
          <el-button
            type="info"
            class="profile-avatar__action"
            circle
            :icon="Camera"
            :loading="avatarUploading"
            size="small"
            title="更换头像"
            @click="triggerFileUpload"
          />
          <input
            ref="fileInput"
            class="profile-avatar__input"
            type="file"
            accept="image/*"
            @change="handleFileChange"
          />
        </div>

        <div class="profile-hero__info">
          <div class="profile-hero__title">
            <h2 class="profile-hero__name">{{ displayName }}</h2>
            <el-tag type="primary" effect="light" round>{{ primaryRole }}</el-tag>
          </div>
          <p class="profile-hero__desc">
            {{ userProfile.username || "-" }} / {{ userProfile.deptName || "未分配部门" }}
          </p>
          <div class="profile-hero__meta">
            <span class="profile-hero__meta-item">
              <el-icon><Calendar /></el-icon>
              加入 {{ formatValue(userProfile.createTime) }}
            </span>
          </div>
        </div>
      </div>

      <div class="profile-hero__actions">
        <el-button :icon="Edit" @click="handleOpenDialog(DialogType.ACCOUNT)">编辑资料</el-button>
        <el-button type="primary" :icon="Lock" @click="handleOpenDialog(DialogType.PASSWORD)">
          修改密码
        </el-button>
        <el-button :icon="SwitchButton" @click="handleLogout">退出登录</el-button>
      </div>
    </section>

    <div class="profile-page__layout">
      <aside class="profile-page__side">
        <section class="profile-card">
          <header class="profile-card__header">
            <h3 class="profile-card__title">个人资料</h3>
            <el-tag size="small" effect="plain">{{ genderText }}</el-tag>
          </header>

          <dl class="profile-info">
            <div v-for="item in profileInfoItems" :key="item.label" class="profile-info__item">
              <dt class="profile-info__label">
                <el-icon><component :is="item.icon" /></el-icon>
                {{ item.label }}
              </dt>
              <dd class="profile-info__value" :class="{ 'is-muted': item.muted }">
                {{ item.value }}
              </dd>
            </div>
          </dl>
        </section>

        <section class="profile-card">
          <header class="profile-card__header">
            <h3 class="profile-card__title">账号概览</h3>
          </header>

          <div class="profile-stats">
            <div v-for="item in profileStats" :key="item.label" class="profile-stats__item">
              <span :class="['profile-icon', 'profile-icon--' + item.tone]">
                <el-icon><component :is="item.icon" /></el-icon>
              </span>
              <div class="profile-stats__body">
                <span class="profile-stats__label">{{ item.label }}</span>
                <strong class="profile-stats__value">
                  {{ item.value }}
                  <em>{{ item.suffix }}</em>
                </strong>
              </div>
            </div>
          </div>
        </section>

        <section class="profile-card">
          <header class="profile-card__header">
            <h3 class="profile-card__title">角色权限</h3>
            <span class="profile-card__extra">{{ permissionCount }} 个权限</span>
          </header>

          <div class="profile-tags">
            <el-tag v-for="role in roleList" :key="role" class="m-0" size="small" effect="light">
              {{ role }}
            </el-tag>
            <span v-if="!roleList.length" class="profile-empty">暂无角色</span>
          </div>
        </section>
      </aside>

      <main class="profile-page__main">
        <section class="profile-card">
          <header class="profile-card__header">
            <div>
              <h3 class="profile-card__title">安全设置</h3>
              <p class="profile-card__desc">维护账号登录凭证与身份验证方式</p>
            </div>
            <el-tooltip
              content="按凭证绑定情况评估：基础 60 分，每绑定手机/邮箱各加 20 分"
              placement="top"
            >
              <el-tag :type="securityLevel.type" effect="light">
                安全等级 {{ securityLevel.label }}
              </el-tag>
            </el-tooltip>
          </header>

          <div class="profile-security">
            <div v-for="item in securityItems" :key="item.key" class="profile-security__item">
              <span :class="['profile-icon', 'profile-icon--large', 'profile-icon--' + item.tone]">
                <el-icon><component :is="item.icon" /></el-icon>
              </span>
              <div class="profile-security__body">
                <div class="profile-security__title">
                  <span>{{ item.title }}</span>
                  <el-tag size="small" :type="item.statusType" effect="plain">
                    {{ item.status }}
                  </el-tag>
                </div>
                <p class="profile-security__desc">{{ item.description }}</p>
              </div>
              <div class="profile-security__actions">
                <el-button
                  v-for="action in item.actions"
                  :key="action.label"
                  :type="action.type"
                  link
                  @click="action.onClick"
                >
                  {{ action.label }}
                </el-button>
              </div>
            </div>
          </div>
        </section>

        <section class="profile-card">
          <header class="profile-card__header">
            <div>
              <h3 class="profile-card__title">账号状态</h3>
              <p class="profile-card__desc">仅统计本人可维护的资料项</p>
            </div>
            <span class="profile-card__extra">完善度 {{ profileCompletion }}%</span>
          </header>

          <div class="profile-status">
            <div
              v-for="item in accountStatusItems"
              :key="item.label"
              class="profile-status__item"
              :class="{ 'is-warning': !item.done }"
            >
              <el-icon class="profile-status__icon">
                <CircleCheck v-if="item.done" />
                <Warning v-else />
              </el-icon>
              <div class="profile-status__body">
                <strong class="profile-status__title">{{ item.label }}</strong>
                <span class="profile-status__desc">{{ item.value }}</span>
              </div>
            </div>
          </div>

          <el-progress
            :percentage="profileCompletion"
            :stroke-width="8"
            :status="profileCompletion >= 80 ? 'success' : undefined"
            class="profile-completion"
          />
        </section>
      </main>
    </div>

    <el-dialog
      v-model="dialogState.visible"
      :title="dialogState.title"
      width="min(520px, 92vw)"
      @closed="handleDialogClosed"
    >
      <el-form
        v-if="dialogState.type === DialogType.ACCOUNT"
        ref="userProfileFormRef"
        :model="userProfileForm"
        :rules="userProfileRules"
        label-width="88px"
        class="pr-10px"
      >
        <el-form-item label="昵称" prop="nickname">
          <el-input
            v-model="userProfileForm.nickname"
            placeholder="请输入昵称"
            maxlength="20"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="性别">
          <DictSelect v-model="userProfileForm.gender" code="gender" />
        </el-form-item>
      </el-form>

      <el-form
        v-else-if="dialogState.type === DialogType.PASSWORD"
        ref="passwordChangeFormRef"
        :model="passwordChangeForm"
        :rules="passwordChangeRules"
        label-width="88px"
        class="pr-10px"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordChangeForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordChangeForm.newPassword"
            type="password"
            show-password
            placeholder="至少 8 位，需包含字母和数字"
            @input="revalidateConfirmPassword"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordChangeForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>

      <el-form
        v-else-if="dialogState.type === DialogType.MOBILE"
        ref="mobileBindingFormRef"
        :model="mobileUpdateForm"
        :rules="mobileBindingRules"
        label-width="88px"
        class="pr-10px"
      >
        <el-form-item label="手机号码" prop="mobile">
          <el-input v-model="mobileUpdateForm.mobile" />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <el-input v-model="mobileUpdateForm.code">
            <template #append>
              <el-button
                :loading="sendingMobileCode"
                :disabled="mobileCountdown > 0"
                @click="handleSendMobileCode"
              >
                {{ mobileCountdown > 0 ? mobileCountdown + "s后重新发送" : "发送验证码" }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="当前密码" prop="password">
          <el-input v-model="mobileUpdateForm.password" type="password" show-password />
        </el-form-item>
      </el-form>

      <el-form
        v-else-if="dialogState.type === DialogType.EMAIL"
        ref="emailBindingFormRef"
        :model="emailUpdateForm"
        :rules="emailBindingRules"
        label-width="88px"
        class="pr-10px"
      >
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="emailUpdateForm.email" />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <el-input v-model="emailUpdateForm.code">
            <template #append>
              <el-button
                :loading="sendingEmailCode"
                :disabled="emailCountdown > 0"
                @click="handleSendEmailCode"
              >
                {{ emailCountdown > 0 ? emailCountdown + "s后重新发送" : "发送验证码" }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="当前密码" prop="password">
          <el-input v-model="emailUpdateForm.password" type="password" show-password />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="inline-flex gap-2">
          <el-button :disabled="submitting" @click="handleCancel">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import UserAPI from "@/api/system/user";
import type {
  UserProfileDetail,
  PasswordChangeForm,
  MobileUpdateForm,
  EmailUpdateForm,
  UserProfileForm,
} from "@/api/system/user";

import type { Component } from "vue";
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import type { FormInstance } from "element-plus";
import FileAPI from "@/api/file";
import { useUserStoreHook } from "@/stores";
import { UserGender } from "@/enums";

import {
  Calendar,
  Camera,
  CircleCheck,
  DataLine,
  Edit,
  Female,
  Iphone,
  Key,
  Lock,
  Male,
  Message,
  OfficeBuilding,
  SwitchButton,
  Timer,
  User,
  UserFilled,
  Warning,
} from "@element-plus/icons-vue";

interface ProfileInfoItem {
  label: string;
  value: string;
  icon: Component;
  muted?: boolean;
}

interface SecurityAction {
  label: string;
  type: "primary" | "danger";
  onClick: () => void;
}

interface SecurityItem {
  key: string;
  title: string;
  description: string;
  status: string;
  statusType: "success" | "warning" | "info";
  icon: Component;
  tone: "primary" | "success" | "warning";
  actions: SecurityAction[];
}

const userStore = useUserStoreHook();
const router = useRouter();

const userProfile = ref<UserProfileDetail>({});

const enum DialogType {
  ACCOUNT = "account",
  PASSWORD = "password",
  MOBILE = "mobile",
  EMAIL = "email",
}

const dialogState = reactive({
  visible: false,
  title: "",
  type: "" as DialogType,
});

const userProfileFormRef = ref<FormInstance>();
const passwordChangeFormRef = ref<FormInstance>();
const mobileBindingFormRef = ref<FormInstance>();
const emailBindingFormRef = ref<FormInstance>();

/** 弹窗提交中：确定按钮 loading + 防重复提交 */
const submitting = ref(false);
/** 头像上传中：头像角标 loading */
const avatarUploading = ref(false);
/** 验证码发送中：发送按钮 loading，防重复点击 */
const sendingMobileCode = ref(false);
const sendingEmailCode = ref(false);

const userProfileForm = reactive<UserProfileForm>({});
const passwordChangeForm = reactive<PasswordChangeForm>({});
const mobileUpdateForm = reactive<MobileUpdateForm>({});
const emailUpdateForm = reactive<EmailUpdateForm>({});

const mobileCountdown = ref(0);
const mobileTimer = ref<ReturnType<typeof setInterval>>();

const emailCountdown = ref(0);
const emailTimer = ref<ReturnType<typeof setInterval>>();

/** 手机号（1[3-9] 号段，11 位） */
const MOBILE_PATTERN = /^1[3-9]\d{9}$/;
/** 邮箱 */
const EMAIL_PATTERN = /\w[-\w.+]*@([A-Za-z0-9][-A-Za-z0-9]+\.)+[A-Za-z]{2,14}/;
/** 新密码强度：至少 8 位，必须同时包含字母和数字 */
const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;

const userProfileRules = {
  nickname: [
    { required: true, whitespace: true, message: "请输入昵称", trigger: "blur" },
    { min: 2, max: 20, message: "昵称长度为 2-20 个字符", trigger: "blur" },
  ],
};

const passwordChangeRules = {
  oldPassword: [{ required: true, message: "请输入原密码", trigger: "blur" }],
  newPassword: [
    { required: true, message: "请输入新密码", trigger: "blur" },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (!value) {
          callback();
          return;
        }
        if (value === passwordChangeForm.oldPassword) {
          callback(new Error("新密码不能与原密码相同"));
          return;
        }
        if (!PASSWORD_PATTERN.test(value)) {
          callback(new Error("至少 8 位，且需同时包含字母和数字"));
          return;
        }
        callback();
      },
      trigger: "blur",
    },
  ],
  confirmPassword: [
    { required: true, message: "请再次输入新密码", trigger: "blur" },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (value !== passwordChangeForm.newPassword) {
          callback(new Error("两次输入的密码不一致"));
          return;
        }
        callback();
      },
      trigger: "blur",
    },
  ],
};

// 手机号校验规则
const mobileBindingRules = {
  mobile: [
    { required: true, message: "请输入手机号", trigger: "blur" },
    {
      pattern: MOBILE_PATTERN,
      message: "请输入正确的手机号码",
      trigger: "blur",
    },
  ],
  code: [{ required: true, message: "请输入验证码", trigger: "blur" }],
  password: [{ required: true, message: "请输入当前密码", trigger: "blur" }],
};

// 邮箱校验规则
const emailBindingRules = {
  email: [
    { required: true, message: "请输入邮箱", trigger: "blur" },
    {
      pattern: EMAIL_PATTERN,
      message: "请输入正确的邮箱地址",
      trigger: "blur",
    },
  ],
  code: [{ required: true, message: "请输入验证码", trigger: "blur" }],
  password: [{ required: true, message: "请输入当前密码", trigger: "blur" }],
};

const displayAvatar = computed(() => userProfile.value.avatar || userStore.userInfo.avatar || "");

const displayName = computed(() => {
  return (
    userProfile.value.nickname ||
    userStore.userInfo.nickname ||
    userProfile.value.username ||
    userStore.userInfo.username ||
    "未命名用户"
  );
});

const roleList = computed(() => {
  return (userProfile.value.roleNames || "")
    .split(/[,，]/)
    .map((role) => role.trim())
    .filter(Boolean);
});

const primaryRole = computed(() => roleList.value[0] || "普通用户");

const permissionCount = computed(() => userStore.userInfo.perms?.length || 0);

const genderText = computed(() => {
  if (userProfile.value.gender === UserGender.MALE) return "男";
  if (userProfile.value.gender === UserGender.FEMALE) return "女";
  return "未设置";
});

const boundCount = computed(() => {
  return [userProfile.value.mobile, userProfile.value.email].filter(Boolean).length;
});

const profileCompletion = computed(() => {
  // 只统计本人可在"编辑资料"/安全设置里维护的项；账号、部门、角色、创建时间不可自助维护，不计入
  const fields = [
    userProfile.value.nickname,
    displayAvatar.value,
    userProfile.value.gender,
    userProfile.value.mobile,
    userProfile.value.email,
  ];
  return Math.round((fields.filter((value) => value !== undefined && value !== null && value !== "").length / fields.length) * 100);
});

const securityLevel = computed(() => {
  const score = 60 + boundCount.value * 20;
  if (score >= 100) {
    return { score, label: "高", type: "success" as const };
  }
  if (score >= 80) {
    return { score, label: "中", type: "warning" as const };
  }
  return { score, label: "低", type: "info" as const };
});

const profileInfoItems = computed<ProfileInfoItem[]>(() => [
  {
    label: "用户名",
    value: userProfile.value.username || "-",
    icon:
      userProfile.value.gender === UserGender.FEMALE
        ? Female
        : userProfile.value.gender === UserGender.MALE
          ? Male
          : User,
  },
  {
    label: "手机号码",
    value: userProfile.value.mobile || "未绑定",
    icon: Iphone,
    muted: !userProfile.value.mobile,
  },
  {
    label: "邮箱",
    value: userProfile.value.email || "未绑定",
    icon: Message,
    muted: !userProfile.value.email,
  },
  {
    label: "部门",
    value: userProfile.value.deptName || "-",
    icon: OfficeBuilding,
    muted: !userProfile.value.deptName,
  },
  {
    label: "创建时间",
    value: formatValue(userProfile.value.createTime),
    icon: Timer,
    muted: !userProfile.value.createTime,
  },
]);

const profileStats = computed(() => [
  {
    label: "安全评分",
    value: securityLevel.value.score,
    suffix: "分",
    icon: Key,
    tone: "primary",
  },
  {
    label: "安全绑定",
    value: boundCount.value,
    suffix: "/2",
    icon: CircleCheck,
    tone: "success",
  },
  {
    label: "角色数量",
    value: roleList.value.length,
    suffix: "个",
    icon: UserFilled,
    tone: "warning",
  },
  {
    label: "权限标识",
    value: permissionCount.value,
    suffix: "个",
    icon: DataLine,
    tone: "info",
  },
]);

const accountStatusItems = computed(() => [
  {
    label: "登录账号",
    value: userProfile.value.username || "未获取",
    done: !!userProfile.value.username,
  },
  {
    label: "手机号验证",
    value: userProfile.value.mobile ? "已绑定" : "未绑定",
    done: !!userProfile.value.mobile,
  },
  {
    label: "邮箱验证",
    value: userProfile.value.email ? "已绑定" : "未绑定",
    done: !!userProfile.value.email,
  },
  {
    label: "资料完善",
    value: `${profileCompletion.value}%`,
    done: profileCompletion.value >= 80,
  },
]);

const securityItems = computed<SecurityItem[]>(() => [
  {
    key: "password",
    title: "账户密码",
    description: "定期修改密码有助于保护账户安全",
    status: "已设置",
    statusType: "success",
    icon: Lock,
    tone: "primary",
    actions: [
      {
        label: "修改",
        type: "primary",
        onClick: () => handleOpenDialog(DialogType.PASSWORD),
      },
    ],
  },
  {
    key: "mobile",
    title: "手机号",
    description: mobileSecurityDesc.value,
    status: userProfile.value.mobile ? "已绑定" : "未绑定",
    statusType: userProfile.value.mobile ? "success" : "warning",
    icon: Iphone,
    tone: "success",
    actions: userProfile.value.mobile
      ? [
          {
            label: "更换",
            type: "primary",
            onClick: () => handleOpenDialog(DialogType.MOBILE),
          },
          {
            label: "解绑",
            type: "danger",
            onClick: handleUnbindMobile,
          },
        ]
      : [
          {
            label: "绑定",
            type: "primary",
            onClick: () => handleOpenDialog(DialogType.MOBILE),
          },
        ],
  },
  {
    key: "email",
    title: "邮箱",
    description: emailSecurityDesc.value,
    status: userProfile.value.email ? "已绑定" : "未绑定",
    statusType: userProfile.value.email ? "success" : "warning",
    icon: Message,
    tone: "warning",
    actions: userProfile.value.email
      ? [
          {
            label: "更换",
            type: "primary",
            onClick: () => handleOpenDialog(DialogType.EMAIL),
          },
          {
            label: "解绑",
            type: "danger",
            onClick: handleUnbindEmail,
          },
        ]
      : [
          {
            label: "绑定",
            type: "primary",
            onClick: () => handleOpenDialog(DialogType.EMAIL),
          },
        ],
  },
]);

/**
 * 空值统一显示占位符；Date 实例格式化为 yyyy-MM-dd HH:mm:ss，
 * 字符串直接展示（兼容后端 ISO 带 T 形态）
 */
function formatValue(value?: Date | string) {
  if (!value) return "-";
  if (value instanceof Date) {
    const pad = (n: number) => String(n).padStart(2, "0");
    return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())} ${pad(
      value.getHours(),
    )}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`;
  }
  return String(value).replace("T", " ").slice(0, 19);
}

/**
 * 从弹窗结果里取出输入值
 */
function getPromptValue(result: unknown) {
  if (result && typeof result === "object" && "value" in result) {
    return String(result.value || "");
  }
  return "";
}

/**
 * 手机号中间四位打码
 */
function maskMobile(mobile?: string) {
  if (!mobile) return "";
  return mobile.replace(/^(\d{3})\d{4}(\d{4})$/, "$1****$2");
}

/**
 * 邮箱名打码
 */
function maskEmail(email?: string) {
  if (!email) return "";
  const [name, domain] = email.split("@");
  if (!domain) return email;
  if (name.length <= 2) return `${name[0] || ""}***@${domain}`;
  return `${name.slice(0, 2)}***@${domain}`;
}

const mobileSecurityDesc = computed(() => {
  return userProfile.value.mobile
    ? `已绑定：${maskMobile(userProfile.value.mobile)}`
    : "未绑定手机号，建议立即绑定";
});

const emailSecurityDesc = computed(() => {
  return userProfile.value.email
    ? `已绑定：${maskEmail(userProfile.value.email)}`
    : "未绑定邮箱，建议立即绑定";
});

/**
 * 打开账号/手机/邮箱修改弹窗
 */
const handleOpenDialog = (type: DialogType) => {
  dialogState.type = type;
  dialogState.visible = true;
  switch (type) {
    case DialogType.ACCOUNT:
      dialogState.title = "编辑资料";
      userProfileForm.nickname = userProfile.value.nickname;
      userProfileForm.avatar = userProfile.value.avatar;
      userProfileForm.gender = userProfile.value.gender;
      break;
    case DialogType.PASSWORD:
      dialogState.title = "修改密码";
      break;
    case DialogType.MOBILE:
      dialogState.title = userProfile.value.mobile ? "更换手机号" : "绑定手机号";
      mobileUpdateForm.mobile = "";
      mobileUpdateForm.code = "";
      mobileUpdateForm.password = "";
      break;
    case DialogType.EMAIL:
      dialogState.title = userProfile.value.email ? "更换邮箱" : "绑定邮箱";
      emailUpdateForm.email = "";
      emailUpdateForm.code = "";
      emailUpdateForm.password = "";
      break;
  }
};

/**
 * 解绑手机号
 */
async function handleUnbindMobile() {
  if (!userProfile.value.mobile) return;
  try {
    const result = await ElMessageBox.prompt("请输入当前密码以解绑手机号", "解绑手机号", {
      type: "warning",
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      inputType: "password",
      inputPlaceholder: "当前密码",
      inputValidator: (val) => !!val || "请输入当前密码",
    });
    const value = getPromptValue(result);
    await UserAPI.unbindMobile({ password: value });
    ElMessage.success("手机号解绑成功");
    await loadUserProfile();
  } catch {
    // ignore
  }
}

/**
 * 解绑邮箱
 */
async function handleUnbindEmail() {
  if (!userProfile.value.email) return;
  try {
    const result = await ElMessageBox.prompt("请输入当前密码以解绑邮箱", "解绑邮箱", {
      type: "warning",
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      inputType: "password",
      inputPlaceholder: "当前密码",
      inputValidator: (val) => !!val || "请输入当前密码",
    });
    const value = getPromptValue(result);
    await UserAPI.unbindEmail({ password: value });
    ElMessage.success("邮箱解绑成功");
    await loadUserProfile();
  } catch {
    // ignore
  }
}

/** 启动/停止手机验证码倒计时 */
function startMobileCountdown() {
  stopMobileCountdown();
  mobileCountdown.value = 60;
  mobileTimer.value = setInterval(() => {
    if (mobileCountdown.value > 0) {
      mobileCountdown.value -= 1;
    } else {
      stopMobileCountdown();
    }
  }, 1000);
}

function stopMobileCountdown() {
  if (mobileTimer.value) {
    clearInterval(mobileTimer.value);
    mobileTimer.value = undefined;
  }
  mobileCountdown.value = 0;
}

/** 启动/停止邮箱验证码倒计时 */
function startEmailCountdown() {
  stopEmailCountdown();
  emailCountdown.value = 60;
  emailTimer.value = setInterval(() => {
    if (emailCountdown.value > 0) {
      emailCountdown.value -= 1;
    } else {
      stopEmailCountdown();
    }
  }, 1000);
}

function stopEmailCountdown() {
  if (emailTimer.value) {
    clearInterval(emailTimer.value);
    emailTimer.value = undefined;
  }
  emailCountdown.value = 0;
}

/**
 * 发送手机验证码
 */
async function handleSendMobileCode() {
  if (!mobileUpdateForm.mobile) {
    ElMessage.error("请输入手机号");
    return;
  }
  if (!MOBILE_PATTERN.test(mobileUpdateForm.mobile)) {
    ElMessage.error("手机号格式不正确");
    return;
  }
  sendingMobileCode.value = true;
  try {
    await UserAPI.sendMobileCode(mobileUpdateForm.mobile);
    ElMessage.success("验证码发送成功");
    startMobileCountdown();
  } catch {
    // 错误提示由请求拦截器统一弹出
  } finally {
    sendingMobileCode.value = false;
  }
}

/**
 * 发送邮箱验证码
 */
async function handleSendEmailCode() {
  if (!emailUpdateForm.email) {
    ElMessage.error("请输入邮箱");
    return;
  }
  if (!EMAIL_PATTERN.test(emailUpdateForm.email)) {
    ElMessage.error("邮箱格式不正确");
    return;
  }
  sendingEmailCode.value = true;
  try {
    await UserAPI.sendEmailCode(emailUpdateForm.email);
    ElMessage.success("验证码发送成功");
    startEmailCountdown();
  } catch {
    // 错误提示由请求拦截器统一弹出
  } finally {
    sendingEmailCode.value = false;
  }
}

/**
 * 提交弹窗表单
 */
const handleSubmit = async () => {
  submitting.value = true;
  try {
    if (dialogState.type === DialogType.ACCOUNT) {
      const valid = await userProfileFormRef.value?.validate();
      if (!valid) return;

      await UserAPI.updateProfile(userProfileForm);
      ElMessage.success("账号资料修改成功");
      dialogState.visible = false;
      if (userProfileForm.nickname) {
        userStore.userInfo.nickname = userProfileForm.nickname;
      }
      await loadUserProfile();
    } else if (dialogState.type === DialogType.PASSWORD) {
      const valid = await passwordChangeFormRef.value?.validate();
      if (!valid) return;

      await UserAPI.changePassword(passwordChangeForm);
      dialogState.visible = false;
      await userStore.redirectToLogin("password-changed");
    } else if (dialogState.type === DialogType.MOBILE) {
      const valid = await mobileBindingFormRef.value?.validate();
      if (!valid) return;

      await UserAPI.bindOrChangeMobile(mobileUpdateForm);
      ElMessage.success(userProfile.value.mobile ? "手机号更换成功" : "手机号绑定成功");
      dialogState.visible = false;
      await loadUserProfile();
    } else if (dialogState.type === DialogType.EMAIL) {
      const valid = await emailBindingFormRef.value?.validate();
      if (!valid) return;

      await UserAPI.bindOrChangeEmail(emailUpdateForm);
      ElMessage.success(userProfile.value.email ? "邮箱更换成功" : "邮箱绑定成功");
      dialogState.visible = false;
      await loadUserProfile();
    }
  } catch {
    // 错误提示由请求拦截器统一弹出，这里只复位提交状态
  } finally {
    submitting.value = false;
  }
};

/**
 * 关闭弹窗；表单重置、红态与验证码倒计时统一在 @closed 里处理
 */
const handleCancel = () => {
  dialogState.visible = false;
};

/**
 * 弹窗关闭动画结束后：重置对应表单的校验红态与输入值，
 * 并清掉验证码倒计时，避免下次打开仍显示"N s 后重发"
 */
const handleDialogClosed = () => {
  if (dialogState.type === DialogType.ACCOUNT) {
    userProfileFormRef.value?.resetFields();
  } else if (dialogState.type === DialogType.PASSWORD) {
    passwordChangeFormRef.value?.resetFields();
  } else if (dialogState.type === DialogType.MOBILE) {
    mobileBindingFormRef.value?.resetFields();
    stopMobileCountdown();
  } else if (dialogState.type === DialogType.EMAIL) {
    emailBindingFormRef.value?.resetFields();
    stopEmailCountdown();
  }
};

/**
 * 新密码变化后，若确认密码已填写则重新校验一致性
 */
const revalidateConfirmPassword = () => {
  if (passwordChangeForm.confirmPassword) {
    passwordChangeFormRef.value?.validateField("confirmPassword");
  }
};

/**
 * 退出登录（与工具栏退出入口行为一致：确认后清登录态并跳登录页）
 */
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm("确定注销并退出系统吗？", "提示", {
      type: "warning",
      confirmButtonText: "确定",
      cancelButtonText: "取消",
    });
  } catch {
    return;
  }
  try {
    await userStore.logout();
    router.push("/login");
  } catch {
    // 错误提示由请求拦截器统一弹出
  }
};

const fileInput = ref<HTMLInputElement | null>(null);

/**
 * 触发头像文件选择
 */
const triggerFileUpload = () => {
  fileInput.value?.click();
};

/** 头像大小上限 2MB */
const AVATAR_MAX_SIZE = 2 * 1024 * 1024;

/**
 * 选择头像后上传
 */
const handleFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement;
  const file = target.files ? target.files[0] : null;
  // 先清空 input 的值，保证连续选择同一个文件也能再次触发 change
  target.value = "";
  if (!file) return;
  if (!file.type.startsWith("image/")) {
    ElMessage.error("仅支持上传图片文件");
    return;
  }
  if (file.size > AVATAR_MAX_SIZE) {
    ElMessage.error("头像图片不能超过 2MB");
    return;
  }
  avatarUploading.value = true;
  try {
    const data = await FileAPI.uploadFile(file);
    if (!data?.url) {
      ElMessage.error("头像上传失败：响应缺少图片地址");
      return;
    }
    await UserAPI.updateProfile({ avatar: data.url });
    userProfile.value.avatar = data.url;
    userStore.userInfo.avatar = data.url;
    ElMessage.success("头像更新成功");
  } catch {
    // 错误提示由请求拦截器统一弹出
  } finally {
    avatarUploading.value = false;
  }
};

/**
 * 加载个人中心用户信息
 */
const loadUserProfile = async () => {
  const data = await UserAPI.getProfile();
  userProfile.value = data;
};

onMounted(() => {
  loadUserProfile().catch(() => {
    // 错误提示由请求拦截器统一弹出
  });
});

onBeforeUnmount(() => {
  if (mobileTimer.value) {
    clearInterval(mobileTimer.value);
  }
  if (emailTimer.value) {
    clearInterval(emailTimer.value);
  }
});
</script>

<style lang="scss" scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100%;
  padding: 16px;
}

.profile-hero,
.profile-card {
  background: var(--content-bg);
  border: 1px solid var(--card-border);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
}

.profile-hero {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
}

.profile-hero__body,
.profile-hero__title,
.profile-hero__meta,
.profile-hero__actions,
.profile-hero__meta-item {
  display: flex;
  align-items: center;
}

.profile-hero__body {
  gap: 16px;
  min-width: 0;
}

.profile-hero__info {
  min-width: 0;
}

.profile-hero__title {
  flex-wrap: wrap;
  gap: 10px;
}

.profile-hero__name {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  line-height: 30px;
  color: var(--el-text-color-primary);
}

.profile-hero__desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.profile-hero__meta {
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.profile-hero__meta-item {
  gap: 4px;
}

.profile-hero__actions {
  flex-shrink: 0;
  gap: 8px;
}

.profile-avatar {
  position: relative;
  flex-shrink: 0;
}

.profile-avatar__action {
  position: absolute;
  right: -2px;
  bottom: -2px;
  border: 2px solid var(--content-bg);
}

.profile-avatar__input {
  display: none;
}

.profile-page__layout {
  display: grid;
  grid-template-columns: minmax(280px, 340px) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.profile-page__side,
.profile-page__main {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.profile-card {
  padding: 18px 20px;
}

.profile-card__header {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 14px;
}

.profile-card__title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  line-height: 22px;
  color: var(--el-text-color-primary);
}

.profile-card__desc {
  margin: 3px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.profile-card__extra,
.profile-empty {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.profile-info {
  display: grid;
  gap: 10px;
  margin: 0;
}

.profile-info__item {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-height: 34px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--el-border-color-extra-light);
}

.profile-info__item:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.profile-info__label {
  display: flex;
  gap: 6px;
  align-items: center;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.profile-info__value {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 13px;
  color: var(--el-text-color-primary);
  white-space: nowrap;
}

.profile-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.profile-stats__item {
  display: flex;
  gap: 10px;
  align-items: center;
  min-width: 0;
  padding: 12px;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-extra-light);
  border-radius: 8px;
}

.profile-stats__body {
  min-width: 0;
}

.profile-stats__label,
.profile-stats__value {
  display: block;
}

.profile-stats__label {
  margin-bottom: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.profile-stats__value {
  font-size: 20px;
  line-height: 24px;
  color: var(--el-text-color-primary);
}

.profile-stats__value em {
  display: inline;
  margin-left: 2px;
  font-size: 12px;
  font-style: normal;
  font-weight: 400;
  color: var(--el-text-color-secondary);
}

.profile-icon {
  display: flex;
  flex: 0 0 36px;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  font-size: 18px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border-radius: 8px;
}

.profile-icon--large {
  flex-basis: 40px;
  width: 40px;
  height: 40px;
  font-size: 20px;
}

.profile-icon--success {
  color: var(--el-color-success);
  background: var(--el-color-success-light-9);
}

.profile-icon--warning {
  color: var(--el-color-warning);
  background: var(--el-color-warning-light-9);
}

.profile-icon--info {
  color: var(--el-color-info);
  background: var(--el-fill-color-light);
}

.profile-security,
.profile-status {
  display: grid;
  gap: 12px;
}

.profile-completion {
  margin-top: 14px;
}

.profile-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.profile-security__item {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  border: 1px solid var(--el-border-color-extra-light);
  border-radius: 8px;
}

.profile-security__body,
.profile-status__body {
  min-width: 0;
}

.profile-security__title {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.profile-security__desc {
  margin: 4px 0 0;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.profile-security__actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.profile-security__actions .el-button + .el-button {
  margin-left: 0;
}

.profile-status__title,
.profile-status__desc {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-status__title {
  font-size: 13px;
  color: var(--el-text-color-primary);
}

.profile-status__desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.profile-status__item {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-height: 38px;
  color: var(--el-color-success);
}

.profile-status__item.is-warning {
  color: var(--el-color-warning);
}

.profile-status__title {
  margin-bottom: 2px;
}

.is-muted {
  color: var(--el-text-color-placeholder);
}

@media (width <= 1200px) {
  .profile-page__layout {
    grid-template-columns: 1fr;
  }
}

@media (width <= 768px) {
  .profile-page {
    padding: 12px;
  }

  .profile-hero {
    align-items: flex-start;
  }

  .profile-hero,
  .profile-hero__body,
  .profile-hero__actions {
    flex-direction: column;
  }

  .profile-hero__actions {
    align-items: stretch;
    width: 100%;
  }

  .profile-hero__actions .el-button {
    width: 100%;
    margin-left: 0;
  }

  .profile-security__item {
    grid-template-columns: 40px minmax(0, 1fr);
  }

  .profile-security__actions {
    grid-column: 2;
    justify-self: start;
  }
}

@media (width <= 520px) {
  .profile-stats,
  .profile-info__item {
    grid-template-columns: 1fr;
  }

  .profile-info__item {
    gap: 4px;
  }
}
</style>
