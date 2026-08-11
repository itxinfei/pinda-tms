<template>
  <div class="app-container">
    <el-card>
      <div slot="header" class="clearfix">
        <span>系统管理</span>
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
  name: 'UserIndex',
  data() {
    return {
      entries: [
        { name: '组织管理', path: '/user/org', icon: 'el-icon-office-building', desc: '机构/网点管理' },
        { name: '岗位管理', path: '/user/station', icon: 'el-icon-postcard', desc: '岗位定义与维护' },
        { name: '用户管理', path: '/user/user', icon: 'el-icon-user', desc: '系统用户与角色分配' }
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
