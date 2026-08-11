<template>
  <div class="app-container">
    <el-card>
      <div slot="header" class="clearfix">
        <span>支付中心</span>
      </div>
      <el-row :gutter="20">
        <el-col v-for="item in entries" :key="item.path" :sm="6" :xs="12">
          <el-card shadow="hover" class="entry-card" @click.native="go(item.path)">
            <i :class="item.icon" class="entry-icon" />
            <div class="entry-name">{{ item.name }}</div>
            <div class="entry-desc">{{ item.desc }}</div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'OfpayIndex',
  data() {
    return {
      entries: [
        { name: '客户支付', path: '/ofpay/customer', icon: 'el-icon-shopping-cart-full', desc: '客户下单支付与查询' },
        { name: '平台支付', path: '/ofpay/platform', icon: 'el-icon-platform-eleme', desc: '平台支付总览' },
        { name: '收款管理', path: '/ofpay/receive', icon: 'el-icon-coin', desc: '收款记录与对账' },
        { name: '付款管理', path: '/ofpay/send', icon: 'el-icon-money', desc: '付款记录与结算' }
      ]
    }
  },
  methods: {
    go(path) {
      this.$router.push(path)
    }
  }
}
</script>

<style lang="scss" scoped>
.entry-card {
  margin-bottom: 16px;
  cursor: pointer;
  text-align: center;
}
.entry-icon {
  font-size: 40px;
  color: #409EFF;
  display: block;
  margin-bottom: 8px;
}
.entry-name {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
}
.entry-desc {
  font-size: 12px;
  color: #909399;
}
</style>
