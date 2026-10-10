<script>
// 品达TMS 客户端 App 入口
export default {
  onLaunch: function () {
    // 登录守卫：无 token 引导回登录页（与 driver/courier 一致）
    const token = uni.getStorageSync('pd_token');
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
</style>
