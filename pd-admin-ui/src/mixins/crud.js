// 修改点：CRUD 列表页通用逻辑 Mixin（批次 D 提取）
// 覆盖 6 个高度同构的列表页（role/user/Application/Attachment/SMS/SMSStatus）的
// search / reset / fetch / sortChange / onSelectChange / singleDelete / batchDelete / clearSelections，
// 以及通用 data 字段。各页只需：
//   1) import crud 并 mixins: [crud]
//   2) data 中提供 apiModule（含 page / delete 方法的 API 对象）
//   3) 差异逻辑通过可选 beforeDelete(selection) 钩子实现
//      - 返回 true 表示已拦截删除（钩子内部自行提示并清理选择），Mixin 不再继续。
// 注意：Vue mixin 与组件同名方法合并时组件优先级更高；本 Mixin 提供的方法
// 在各页均被移除，因此实际生效的就是本实现。

export default {
  data() {
    return {
      tableKey: 0,
      queryParams: {},
      sort: {},
      selection: [],
      // 以下为批次 B/D 整改后统一字段
      loading: false,
      tableData: {
        total: 0
      },
      pagination: {
        size: 10,
        current: 1
      },
      dialog: {
        isVisible: false,
        type: 'add'
      }
    }
  },
  methods: {
    search() {
      this.fetch({
        ...this.queryParams,
        ...this.sort
      })
    },
    reset() {
      this.queryParams = {}
      this.sort = {}
      if (this.$refs.table) {
        this.$refs.table.clearSort()
        this.$refs.table.clearFilter()
      }
      this.search()
    },
    sortChange(val) {
      this.sort.field = val.prop
      this.sort.order = val.order
      this.search()
    },
    onSelectChange(selection) {
      this.selection = selection
    },
    clearSelections() {
      if (this.$refs.table) {
        this.$refs.table.clearSelection()
      }
    },
    singleDelete(row) {
      if (this.$refs.table) {
        this.$refs.table.toggleRowSelection(row, true)
      }
      this.batchDelete()
    },
    batchDelete() {
      if (!this.selection.length) {
        this.$message({
          message: this.$t('tips.noDataSelected'),
          type: 'warning'
        })
        return
      }
      // 可选钩子：返回 true 表示拦截删除（如含当前用户、内置数据）
      if (typeof this.beforeDelete === 'function' && this.beforeDelete(this.selection) === true) {
        return
      }
      this.$confirm(this.$t('tips.confirmDelete'), this.$t('common.tips'), {
        confirmButtonText: this.$t('common.confirm'),
        cancelButtonText: this.$t('common.cancel'),
        type: 'warning'
      })
        .then(() => {
          const ids = this.selection.map(item => item.id)
          this.apiModule.delete({ ids }).then(response => {
            const res = response.data
            if (res.isSuccess) {
              this.$message({
                message: this.$t('tips.deleteSuccess'),
                type: 'success'
              })
            }
            this.search()
          })
        })
        .catch(() => {
          this.clearSelections()
        })
    },
    fetch(params = {}) {
      this.loading = true
      params.size = this.pagination.size
      params.current = this.pagination.current
      if (this.queryParams.timeRange) {
        params.startCreateTime = this.queryParams.timeRange[0]
        params.endCreateTime = this.queryParams.timeRange[1]
      }
      this.apiModule.page(params).then(response => {
        const res = response.data
        this.loading = false
        this.tableData = res.data
      })
    }
  }
}
