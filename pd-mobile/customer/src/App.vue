<script>
// 品达TMS 客户端 App 入口
export default {
  onLaunch: function () {
    // 登录态检查：无 token 引导回登录页
    const token = uni.getStorageSync('token');
    if (!token) {
      // 首次启动页为 home，home onShow 会跳登录；此处仅预检
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
