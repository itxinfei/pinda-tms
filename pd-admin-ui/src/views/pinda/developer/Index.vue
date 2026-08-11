<template>
  <div class="app-container">
    <el-card>
      <div slot="header" class="clearfix">
        <span>开发者中心</span>
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
  name: 'DeveloperIndex',
  data() {
    return {
      entries: [
        { name: '应用管理', path: '/developer/application', icon: 'el-icon-coin', desc: '应用接入与密钥管理' },
        { name: '数据库监控', path: '/developer/db', icon: 'el-icon-database', desc: 'SQL监控与统计' },
        { name: '操作日志', path: '/developer/optLog', icon: 'el-icon-document', desc: '用户操作审计日志' },
        { name: '登录日志', path: '/developer/loginLog', icon: 'el-icon-user', desc: '登录行为记录' },
        { name: '接口管理', path: '/developer/systemApi', icon: 'el-icon-s-platform', desc: '系统API定义' }
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
