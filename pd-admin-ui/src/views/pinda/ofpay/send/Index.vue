<template>
  <div class="app-container">
    <el-card>
      <div slot="header" class="clearfix">
        <span>付款管理</span>
      </div>
      <el-row :gutter="12" style="margin-bottom: 12px;">
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">付款对象</div>
            <div class="stat-value">司机 / 快递员 / 供应商</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">付款方式</div>
            <div class="stat-value">对公转账 / 结算单</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">状态</div>
            <div class="stat-value">待结算 / 已结算</div>
          </el-card>
        </el-col>
      </el-row>
      <el-alert
        title="付款管理: 用于司机/快递员/供应商费用结算的付款记录管理(预留扩展, 可与工资/对账模块对接)。"
        type="info"
        :closable="false"
      />
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'OfpaySend'
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
