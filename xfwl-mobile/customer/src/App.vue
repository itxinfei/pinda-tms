<script>
// 心飞物流TMS 客户端 App 入口
export default {
  onLaunch: function () {
    // 登录守卫：无 token 引导回登录页（与 driver/courier 一致）
    const token = uni.getStorageSync('xfwl_token');
    if (!token) {
      uni.reLaunch({ url: '/pages/customer/login/index' });
    }
  },
  onShow: function () {},
  onHide: function () {},
}
</script>

<style lang="scss">
/* uview-plus 全局样式 */
@import "uview-plus/index.scss";
/* 业务设计 Token */
@import "@/common/styles/variables.scss";

/* 微交互：按钮按压态统一按压红（《70》§2.6） */
.u-button--active {
  background-color: var(--c-primary-dark) !important;
}
</style>
