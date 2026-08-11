<template>
  <div class="app-container">
    <el-card>
      <div slot="header" class="clearfix">
        <span>平台支付总览</span>
      </div>
      <el-row :gutter="12">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">支付渠道</div>
            <div class="stat-value">mock / wechat / alipay</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">接入方式</div>
            <div class="stat-value">统一支付服务 /pay</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">渠道配置</div>
            <div class="stat-value">pay.channel 切换</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">支付状态</div>
            <div class="stat-value">待支付/已支付/已退款</div>
          </el-card>
        </el-col>
      </el-row>
      <el-alert
        title="平台支付总览: 通过统一支付服务(pd-oms /pay)管理订单支付, 支持微信/支付宝/模拟渠道, 渠道由 pay.channel 配置切换。"
        type="info"
        :closable="false"
        style="margin-top: 16px;"
      />
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'OfpayPlatform'
}
</script>

<style lang="scss" scoped>
.stat-card {
  text-align: center;
}
.stat-label {
  color: #909399;
  font-size: 12px;
}
.stat-value {
  margin-top: 6px;
  font-size: 14px;
  font-weight: 600;
}
</style>
