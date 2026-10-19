<template>
  <div class="order-fee-panel">
    <div class="order-fee-panel__header">
      <span class="order-detail__title">附加费明细</span>
      <div v-if="editable" class="order-fee-panel__actions">
        <el-button
          v-hasPerm="'finance:order-fee:add'"
          size="small"
          :loading="calculating"
          @click="handleCalculate"
        >
          按配置生成
        </el-button>
        <el-button v-hasPerm="'finance:order-fee:add'" size="small" type="primary" @click="openForm">
          人工补录
        </el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="items" border size="small" empty-text="暂无附加费明细">
      <el-table-column prop="feeName" label="费用项" min-width="110" />
      <el-table-column label="金额(元)" width="100" align="right">
        <template #default="scope">{{ formatNumber(scope.row.amount) }}</template>
      </el-table-column>
      <el-table-column label="来源" width="90" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.source === 1 ? 'primary' : 'warning'" size="small" effect="plain">
            {{ scope.row.source === 1 ? "自动" : "人工" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="calcBasis" label="计算依据" min-width="180" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.calcBasis || "-" }}</template>
      </el-table-column>
      <el-table-column prop="operatorName" label="操作人" width="100" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.operatorName || "-" }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" width="170" />
    </el-table>

    <div class="order-fee-panel__total">
      合计：<span class="order-fee-panel__total-amount">{{ formatNumber(total) }}</span> 元
    </div>

    <!-- 人工补录弹窗（写操作居中弹窗 + append-to-body） -->
    <el-dialog v-model="formVisible" title="人工补录附加费" width="420px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="费用项" prop="feeCode">
          <el-select v-model="form.feeCode" placeholder="选择费用项" style="width: 100%">
            <el-option v-for="(label, value) in FEE_CODE_LABELS" :key="value" :value="value" :label="`${label}（${value}）`" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额(元)" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="事由" prop="calcBasis">
          <el-input v-model="form.calcBasis" placeholder="补录事由（如：现场确认装卸 2 次）" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="formVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import OrderFeeAPI, { type OrderFeeRecord } from "@/api/order-fee";
import { FEE_CODE_LABELS } from "@/api/fee-item";
import { formatNumber } from "@/utils/format";

/**
 * 订单附加费明细面板（订单详情抽屉内联，POD-03 第一期）
 *
 * <p>只读模式（editable=false）隐藏生成/补录按钮；明细独立存表不改写订单 amount。</p>
 */
const props = withDefaults(
  defineProps<{
    /** 订单 id（雪花 Long 字符串形态） */
    orderId: string;
    /** 是否允许生成/补录 */
    editable?: boolean;
  }>(),
  { editable: true }
);

const loading = ref(false);
const items = ref<OrderFeeRecord[]>([]);
const total = computed(() => items.value.reduce((sum, item) => sum + Number(item.amount ?? 0), 0));

async function loadList() {
  if (!props.orderId) return;
  loading.value = true;
  try {
    const res = await OrderFeeAPI.list(props.orderId);
    items.value = res?.items ?? [];
  } finally {
    loading.value = false;
  }
}

watch(() => props.orderId, loadList);
onMounted(loadList);

/** 按配置自动生成（幂等） */
const calculating = ref(false);

async function handleCalculate() {
  calculating.value = true;
  try {
    const created = await OrderFeeAPI.calculate(props.orderId);
    if (created && created.length > 0) {
      ElMessage.success(`已生成 ${created.length} 项附加费`);
    } else {
      ElMessage.info("没有可新生成的附加费（已存在或无启用的费用项配置）");
    }
    loadList();
  } finally {
    calculating.value = false;
  }
}

/** 人工补录弹窗 */
const formVisible = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<{ feeCode?: string; amount?: number; calcBasis?: string }>({});

const rules: FormRules = {
  feeCode: [{ required: true, message: "请选择费用项", trigger: "change" }],
  amount: [{ required: true, message: "金额不能为空", trigger: "blur" }],
};

function openForm() {
  form.feeCode = undefined;
  form.amount = undefined;
  form.calcBasis = undefined;
  formVisible.value = true;
}

async function handleSave() {
  await formRef.value?.validate().catch(() => Promise.reject(new Error("validate")));
  saving.value = true;
  try {
    await OrderFeeAPI.addManual({
      orderId: props.orderId,
      feeCode: form.feeCode!,
      amount: form.amount!,
      calcBasis: form.calcBasis || undefined,
    });
    ElMessage.success("补录成功");
    formVisible.value = false;
    loadList();
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.order-fee-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.order-fee-panel__actions {
  display: flex;
  gap: 8px;
}

.order-fee-panel__total {
  margin-top: 8px;
  text-align: right;
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.order-fee-panel__total-amount {
  font-weight: 600;
  color: var(--el-color-danger);
}
</style>
