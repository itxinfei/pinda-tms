<template>
  <div class="app-container">
    <!-- 查询条件 -->
    <div class="filter-container">
      <el-input
        v-model="queryParams.businessId"
        placeholder="业务ID（车辆/快递员ID）"
        class="filter-item search-item"
        clearable
      />
      <el-select v-model="queryParams.type" placeholder="类型" class="filter-item search-item" clearable>
        <el-option label="车辆" value="truck" />
        <el-option label="快递员" value="courier" />
      </el-select>
      <el-input
        v-model="queryParams.licensePlate"
        placeholder="车牌号"
        class="filter-item search-item"
        clearable
      />
      <el-button class="filter-item" plain type="primary" @click="search">查询</el-button>
      <el-button class="filter-item" plain type="warning" @click="reset">重置</el-button>
    </div>

    <!-- 最近位置概览 -->
    <el-row v-if="latestRecord" :gutter="12" class="latest-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="latest-label">业务ID</div>
          <div class="latest-value">{{ latestRecord.businessId }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="latest-label">车牌号 / 名称</div>
          <div class="latest-value">{{ latestRecord.licensePlate || '-' }} / {{ latestRecord.name || '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="latest-label">位置（经度, 纬度）</div>
          <div class="latest-value">{{ latestRecord.lng }}, {{ latestRecord.lat }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="latest-label">上报时间</div>
          <div class="latest-value">{{ latestRecord.currentTime }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 轨迹地图 -->
    <el-card class="chart-card">
      <div slot="header" class="clearfix">
        <span>轨迹回放</span>
        <el-button v-if="traceData.length > 1" class="filter-item" size="mini" style="float: right;" @click="playTrace">
          {{ playing ? '暂停' : '播放' }}
        </el-button>
      </div>
      <div v-loading="chartLoading" id="traceChart" class="chart-container" />
    </el-card>

    <!-- 轨迹明细表格 -->
    <el-card>
      <div slot="header" class="clearfix">
        <span>轨迹明细</span>
      </div>
      <el-table
        :key="tableKey"
        ref="table"
        v-loading="loading"
        :data="tableData.records"
        border
        fit
        size="mini"
        highlight-current-row
      >
        <el-table-column prop="businessId" label="业务ID" min-width="120" />
        <el-table-column prop="type" label="类型" width="80">
          <template slot-scope="{ row }">
            <el-tag :type="row.type === 'truck' ? 'primary' : 'success'">
              {{ row.type === 'truck' ? '车辆' : '快递员' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="licensePlate" label="车牌号" width="100" />
        <el-table-column prop="name" label="名称" width="100" />
        <el-table-column prop="lng" label="经度" width="120" />
        <el-table-column prop="lat" label="纬度" width="120" />
        <el-table-column prop="transportTaskId" label="运输任务ID" min-width="140" />
        <el-table-column prop="currentTime" label="上报时间" min-width="140" />
      </el-table>
      <pagination
        v-show="tableData.total > 0"
        :total="tableData.total"
        :page.sync="queryParams.page"
        :limit.sync="queryParams.pageSize"
        @pagination="findByPage"
      />
    </el-card>
  </div>
</template>

<script>
import echarts from 'echarts'
import Pagination from '@/components/Pagination'
import GpsTraceApi from '@/api/GpsTrace.js'

export default {
  name: 'GpsTraceMonitor',
  components: { Pagination },
  data() {
    return {
      tableKey: 0,
      loading: false,
      chartLoading: false,
      playing: false,
      traceData: [],
      latestRecord: null,
      queryParams: {
        page: 1,
        pageSize: 20,
        businessId: '',
        type: '',
        licensePlate: ''
      },
      tableData: {
        records: [],
        total: 0
      },
      chart: null,
      playTimer: null,
      playIndex: 0,
      markerPoint: null
    }
  },
  mounted() {
    this.initChart()
  },
  beforeDestroy() {
    this.stopPlay()
    // 移除 resize 监听器，避免组件销毁后监听器泄漏
    window.removeEventListener('resize', this.resizeChart)
    if (this.chart) {
      this.chart.dispose()
      this.chart = null
    }
  },
  methods: {
    initChart() {
      this.chart = echarts.init(document.getElementById('traceChart'))
      this.chart.setOption({
        title: { text: '车辆/快递员轨迹', left: 'center', textStyle: { fontSize: 14 } },
        tooltip: { trigger: 'item', formatter: params => this.traceTooltip(params) },
        xAxis: { type: 'value', name: '经度', scale: true },
        yAxis: { type: 'value', name: '纬度', scale: true },
        series: [{
          name: '轨迹',
          type: 'line',
          data: [],
          smooth: true,
          symbolSize: 6,
          lineStyle: { width: 2, color: '#409EFF' },
          itemStyle: { color: '#409EFF' }
        }]
      })
      window.addEventListener('resize', this.resizeChart)
    },
    resizeChart() {
      if (this.chart) this.chart.resize()
    },
    traceTooltip(params) {
      const point = this.traceData[params.dataIndex]
      if (!point) return ''
      return `业务ID: ${point.businessId}<br/>时间: ${point.currentTime}<br/>位置: ${point.lng}, ${point.lat}`
    },
    search() {
      this.queryParams.page = 1
      this.findByPage()
      this.loadReplay()
    },
    reset() {
      this.queryParams = { page: 1, pageSize: 20, businessId: '', type: '', licensePlate: '' }
      this.latestRecord = null
      this.traceData = []
      this.clearChart()
      this.findByPage()
    },
    clearChart() {
      if (!this.chart) return
      this.chart.setOption({
        series: [{ data: [] }],
        title: { text: '车辆/快递员轨迹' }
      })
    },
    loadReplay() {
      const { businessId, type } = this.queryParams
      if (!businessId) {
        this.latestRecord = null
        this.traceData = []
        this.clearChart()
        return
      }
      this.chartLoading = true
      GpsTraceApi.replay({ businessId, type }).then(res => {
        const list = (res.data && res.data.data) || []
        this.traceData = list
        this.renderTrace(list)
        this.loadLatest()
      }).catch(() => {
        this.traceData = []
        this.clearChart()
      }).finally(() => {
        this.chartLoading = false
      })
    },
    loadLatest() {
      const { businessId, type } = this.queryParams
      GpsTraceApi.latest({ businessId, type }).then(res => {
        this.latestRecord = (res.data && res.data.data) || null
      }).catch(() => {
        this.latestRecord = null
      })
    },
    renderTrace(list) {
      if (!this.chart) return
      const points = list.map(item => [parseFloat(item.lng), parseFloat(item.lat)])
      this.chart.setOption({
        title: { text: `车辆/快递员轨迹（${list.length} 个轨迹点）` },
        series: [{ type: 'line', data: points, smooth: true }]
      })
    },
    playTrace() {
      if (this.playing) {
        this.stopPlay()
        return
      }
      if (this.traceData.length < 2) {
        this.$message.warning('轨迹点不足，无法播放')
        return
      }
      this.playing = true
      this.playIndex = 0
      this.playTimer = setInterval(() => {
        this.playIndex++
        if (this.playIndex >= this.traceData.length) {
          this.stopPlay()
          return
        }
        const point = this.traceData[this.playIndex]
        this.chart.dispatchAction({
          type: 'showTip',
          seriesIndex: 0,
          dataIndex: this.playIndex
        })
        this.markerPoint = [parseFloat(point.lng), parseFloat(point.lat)]
        this.chart.setOption({
          series: [{
            type: 'scatter',
            symbolSize: 12,
            data: [this.markerPoint],
            itemStyle: { color: '#E6A23C' },
            zlevel: 10
          }]
        })
      }, 500)
    },
    stopPlay() {
      this.playing = false
      if (this.playTimer) {
        clearInterval(this.playTimer)
        this.playTimer = null
      }
      this.chart && this.chart.setOption({
        series: [{
          type: 'scatter',
          data: []
        }]
      })
    },
    findByPage() {
      const params = {
        page: this.queryParams.page,
        pageSize: this.queryParams.pageSize,
        businessId: this.queryParams.businessId,
        type: this.queryParams.type,
        licensePlate: this.queryParams.licensePlate
      }
      this.loading = true
      GpsTraceApi.page(params).then(res => {
        const data = (res.data && res.data.data) || {}
        this.tableData.records = data.items || []
        this.tableData.total = data.total || 0
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.search-item {
  width: 200px;
  margin-right: 8px;
}
.latest-row {
  margin-bottom: 12px;
}
.latest-label {
  color: #909399;
  font-size: 12px;
}
.latest-value {
  margin-top: 6px;
  font-size: 14px;
  font-weight: 600;
  word-break: break-all;
}
.chart-card {
  margin-bottom: 12px;
}
.chart-container {
  width: 100%;
  height: 460px;
}
</style>
