<script>
// 启动守卫：无 token 跳登录页
export default {
  onLaunch: function () {
    const token = uni.getStorageSync('pd_token')
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
/* 设计 Token（颜色 / 间距 / 字号 / 全局 class）唯一真源，全局注入一次 */
@import "./common/styles/variables.scss";
</style>
