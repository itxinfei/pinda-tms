// 修改点：生产基础设施地址改为从环境变量读取，禁止在源码中硬编码 IP
module.exports = {
  title: '品达物流',
  onlinePreview: process.env.VUE_APP_ONLINE_PREVIEW || 'http://127.0.0.1:8012/onlinePreview?url=',
  druid: {
    authority: {
      development: process.env.VUE_APP_DRUID_AUTHORITY_DEV || 'http://127.0.0.1:8764/druid',
      production: process.env.VUE_APP_DRUID_AUTHORITY_PROD || 'http://127.0.0.1:8764/druid'
    },
    file: {
      development: process.env.VUE_APP_DRUID_FILE_DEV || 'http://127.0.0.1:8765/druid',
      production: process.env.VUE_APP_DRUID_FILE_PROD || 'http://127.0.0.1:8765/druid'
    }
  }
}
