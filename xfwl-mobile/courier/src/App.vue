<script>
// 启动守卫：无 token 跳登录页
export default {
  onLaunch: function () {
    const token = uni.getStorageSync('xfwl_token')
    if (!token) {
      uni.reLaunch({ url: '/pages/courier/login/index' })
    }
  },
  onShow: function () {},
  onHide: function () {},
}
</script>

<style lang="scss">
/* uview-plus 全局样式 */
@import "uview-plus/index.scss";
/* 业务 Token：品牌色 / 间距 / 字号 / 全局 class（唯一来源，全端注入一份） */
@import "./common/styles/variables.scss";

/* 微交互：按钮按压态统一按压红（《70》§2.6） */
.u-button--active {
  background-color: var(--c-primary-dark) !important;
}
</style>
