<template>
  <div class="app-container">
    <el-card>
      <div slot="header" class="clearfix">
        <span>客户支付</span>
      </div>
      <div class="filter-container">
        <el-input v-model="orderId" placeholder="输入订单ID" class="filter-item search-item" clearable />
        <el-button type="primary" :loading="creating" @click="create">创建支付</el-button>
        <el-button type="info" :loading="querying" @click="query">查询支付状态</el-button>
      </div>
      <el-alert
        v-if="payment"
        :title="`支付单: ${payment.payNo} | 渠道: ${payment.payChannel} | 金额: ${payment.amount} | 状态: ${statusText(payment.status)}`"
        type="info"
        :closable="false"
        show-icon
        style="margin-top: 12px;"
      />
      <el-input
        v-if="payment && payment.prepayParams"
        v-model="payment.prepayParams"
        type="textarea"
        :rows="6"
        readonly
        label="预支付参数"
        style="margin-top: 12px;"
      />
      <div v-if="payment && payment.payChannel === 'mock'" style="margin-top: 12px;">
        <el-button type="success" @click="mockConfirm">模拟支付成功</el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
import PayApi from '@/api/Pay.js'

export default {
  name: 'OfpayCustomer',
  data() {
    return {
      orderId: '',
      payment: null,
      creating: false,
      querying: false
    }
  },
  methods: {
    statusText(status) {
      return { 0: '待支付', 1: '已支付', 2: '已关闭', 3: '已退款' }[status] || status
    },
    create() {
      if (!this.orderId) {
        this.$message.warning('请输入订单ID')
        return
      }
      this.creating = true
      PayApi.createPayment(this.orderId).then(res => {
        const data = (res.data && res.data.data) || null
        this.payment = data
        if (data && data.status === 1) {
          this.$message.success('该订单已支付')
        } else {
          this.$message.success('支付单已创建')
        }
      }).finally(() => {
        this.creating = false
      })
    },
    query() {
      if (!this.orderId) {
        this.$message.warning('请输入订单ID')
        return
      }
      this.querying = true
      PayApi.queryPayment(this.orderId).then(res => {
        this.payment = (res.data && res.data.data) || null
        if (!this.payment) {
          this.$message.info('未查询到支付单')
        }
      }).finally(() => {
        this.querying = false
      })
    },
    mockConfirm() {
      // 模拟渠道支付成功：直接调用支付回调接口
      const body = { payNo: this.payment.payNo, tradeNo: 'MOCK' + Date.now() }
      const url = `${window.location.origin}/api/pay/callback/mock`
      fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', token: localStorage.getItem('TOKEN') || '' },
        body: JSON.stringify(body)
      }).then(() => {
        this.$message.success('模拟支付成功')
        this.query()
      }).catch(() => {
        this.$message.error('模拟支付回调失败')
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
