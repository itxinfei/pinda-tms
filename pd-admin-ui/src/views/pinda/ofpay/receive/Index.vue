<template>
  <div class="app-container">
    <el-card>
      <div slot="header" class="clearfix">
        <span>收款管理</span>
      </div>
      <div class="filter-container">
        <el-input v-model="orderId" placeholder="输入订单ID查询支付/收款状态" class="filter-item search-item" clearable />
        <el-button type="primary" @click="query">查询</el-button>
        <el-button type="danger" :loading="refunding" @click="refund">退款</el-button>
      </div>
      <el-alert
        v-if="payment"
        :title="`订单 ${payment.orderId} | 支付单 ${payment.payNo} | 渠道 ${payment.payChannel} | 金额 ${payment.amount} | 状态 ${statusText(payment.status)}`"
        :type="payment.status === 1 ? 'success' : 'warning'"
        :closable="false"
        show-icon
        style="margin-top: 12px;"
      />
      <el-alert
        title="收款管理: 查询订单支付单与收款状态, 支持对已支付订单发起退款。"
        type="info"
        :closable="false"
        style="margin-top: 12px;"
      />
    </el-card>
  </div>
</template>

<script>
import PayApi from '@/api/Pay.js'

export default {
  name: 'OfpayReceive',
  data() {
    return {
      orderId: '',
      payment: null,
      refunding: false
    }
  },
  methods: {
    statusText(status) {
      return { 0: '待支付', 1: '已支付', 2: '已关闭', 3: '已退款' }[status] || status
    },
    query() {
      if (!this.orderId) {
        this.$message.warning('请输入订单ID')
        return
      }
      PayApi.queryPayment(this.orderId).then(res => {
        this.payment = (res.data && res.data.data) || null
        if (!this.payment) {
          this.$message.info('未查询到支付单')
        }
      })
    },
    refund() {
      if (!this.orderId) {
        this.$message.warning('请输入订单ID')
        return
      }
      this.refunding = true
      PayApi.refund(this.orderId).then(res => {
        this.$message.success('退款成功')
        this.query()
      }).catch(() => {
        this.$message.error('退款失败')
      }).finally(() => {
        this.refunding = false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.search-item {
  width: 300px;
  margin-right: 8px;
}
</style>
