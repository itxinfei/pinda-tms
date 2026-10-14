<template>
  <section class="dash-bottom">
    <div class="card">
      <div class="card__head">
        <h3 class="card__title">待处理订单</h3>
        <div class="card__head-right">
          <el-button link type="primary" size="small" @click="emit('go-orders')">查看更多</el-button>
          <el-tag size="small">{{ todoList.length }} 单</el-tag>
        </div>
      </div>
      <div class="card__body">
        <el-skeleton v-if="todoLoading" :rows="5" animated class="dash-list__skeleton" />
        <template v-else>
          <el-result v-if="todoError" icon="error" title="待办加载失败" class="dash-result">
            <template #extra>
              <el-button size="small" type="primary" @click="emit('retry-todo')">重试</el-button>
            </template>
          </el-result>
          <el-empty v-else-if="todoList.length === 0" :image-size="56" description="暂无待处理订单" />
          <div
            v-for="item in todoList"
            :key="item.id"
            class="todo-row"
            @click="emit('go-orders')"
          >
            <el-icon :size="16" class="todo-row__icon--pending">
              <Clock />
            </el-icon>
            <el-tooltip
              :content="`${item.orderNo}${item.senderName ? ` - ${item.senderName}` : ''}`"
              placement="top"
              :show-after="300"
            >
              <span class="todo-row__title">
                {{ item.orderNo }}<template v-if="item.senderName"> - {{ item.senderName }}</template>
              </span>
            </el-tooltip>
            <el-tag type="warning" size="small" effect="plain" class="todo-row__tag">
              {{ item.statusText || "待处理" }}
            </el-tag>
            <el-tooltip :content="`下单时间 ${item.createTime || '-'}`" placement="top" :show-after="300">
              <span class="todo-row__wait" :class="{ 'is-over': todoWaitOverdue(item.createTime) }">
                已等待 {{ formatDuration(item.createTime) }}
              </span>
            </el-tooltip>
          </div>
        </template>
      </div>
    </div>

    <div class="card">
      <div class="card__head">
        <h3 class="card__title">最新告警</h3>
        <div class="card__head-right">
          <el-button link type="primary" size="small" @click="emit('go-alarms')">查看更多</el-button>
          <el-tag type="danger" size="small">{{ alertCountText }}</el-tag>
        </div>
      </div>
      <div class="card__body card__body--scroll">
        <div class="feed">
          <el-skeleton v-if="alarmLoading" :rows="5" animated class="dash-list__skeleton" />
          <template v-else>
            <el-result v-if="alarmError" icon="error" title="告警加载失败" class="dash-result">
              <template #extra>
                <el-button size="small" type="primary" @click="emit('retry-alarm')">重试</el-button>
              </template>
            </el-result>
            <el-empty v-else-if="alertList.length === 0" :image-size="56" description="暂无未处理告警" />
            <div v-for="item in alertList" :key="item.id" class="feed__item" @click="emit('go-alarms')">
              <el-tag
                size="small"
                :type="alarmTagType(item.alarmType)"
                effect="light"
                class="feed__tag"
              >
                {{ alarmTypeText(item.alarmType) }}
              </el-tag>
              <el-tooltip
                :content="item.alarmContent || '运输过程异常，请及时关注'"
                placement="top"
                :show-after="300"
              >
                <span class="feed__text">{{ item.alarmContent || "运输过程异常，请及时关注" }}</span>
              </el-tooltip>
              <el-tooltip
                :content="`告警时间 ${formatShortTime(item.alarmTime || item.createTime)}`"
                placement="top"
                :show-after="300"
              >
                <span class="feed__time" :class="{ 'is-over': alarmWaitOverdue(item.alarmTime || item.createTime) }">
                  未处理 {{ formatDuration(item.alarmTime || item.createTime) }}
                </span>
              </el-tooltip>
            </div>
          </template>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { Clock } from "@element-plus/icons-vue";
import { alarmTypeText, type AlarmRecord } from "@/api/alarm";

export interface DashboardTodoItem {
  id: string;
  orderNo: string;
  senderName?: string;
  statusText?: string;
  createTime?: string;
}

export interface DashboardAlertItem extends AlarmRecord {
  alarmContent?: string;
}

defineProps<{
  todoList: DashboardTodoItem[];
  todoLoading: boolean;
  todoError: boolean;
  alertList: DashboardAlertItem[];
  alarmLoading: boolean;
  alarmError: boolean;
  /** 未处理告警数文案（含错误态 "--"） */
  alertCountText: string;
}>();

const emit = defineEmits<{
  "go-orders": [];
  "go-alarms": [];
  "retry-todo": [];
  "retry-alarm": [];
}>();

/** 相对时长文案：从 start 到现在的间隔（X 分钟 / X 小时 Y 分 / X 天 Y 小时） */
function formatDuration(start?: string): string {
  if (!start) return "--";
  const s = new Date(start.replace(/-/g, "/")).getTime();
  if (Number.isNaN(s)) return "--";
  const mins = Math.floor((Date.now() - s) / 60000);
  if (mins < 0) return "刚刚";
  if (mins < 60) return `${mins} 分钟`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours} 小时 ${mins % 60} 分`;
  return `${Math.floor(hours / 24)} 天 ${hours % 24} 小时`;
}

/** 待办订单超 24 小时未处理视为超时（红色高亮） */
function todoWaitOverdue(start?: string): boolean {
  if (!start) return false;
  const s = new Date(start.replace(/-/g, "/")).getTime();
  if (Number.isNaN(s)) return false;
  return Date.now() - s > 24 * 60 * 60 * 1000;
}

/** 告警超 2 小时未处理视为超时（红色高亮） */
function alarmWaitOverdue(time?: string): boolean {
  if (!time) return false;
  const s = new Date(time.replace(/-/g, "/")).getTime();
  if (Number.isNaN(s)) return false;
  return Date.now() - s > 2 * 60 * 60 * 1000;
}

/** 紧凑时间：仅显示 "MM-DD HH:mm"（跨年显示年份） */
function formatShortTime(time?: string): string {
  if (!time) return "--";
  const d = new Date(time.replace(/-/g, "/"));
  if (Number.isNaN(d.getTime())) return "--";
  const pad = (n: number) => String(n).padStart(2, "0");
  const md = `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
  return d.getFullYear() === new Date().getFullYear() ? md : `${d.getFullYear()}-${md}`;
}

/** 告警类型 → Element Plus tag 类型（配色）：超速/离线危险红，停留/偏离警示橙，人工上报类中性蓝 */
function alarmTagType(type?: number): "danger" | "warning" | "info" {
  if (type == null) return "info";
  if (type === 1 || type === 2) return "danger";
  if (type === 3 || type === 4) return "warning";
  return "info";
}
</script>

<style lang="scss">
@use "./dashboard-card.scss";
</style>
