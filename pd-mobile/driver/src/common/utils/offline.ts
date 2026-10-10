// 离线队列：无网络时缓冲上报任务，恢复后补传（司机端定位上报 / 异常上报使用）
// 修改点：三端复用，无需改动。
const KEY = 'pd_offline_queue'

export interface OfflineItem {
  id: string
  url: string
  method: string
  data: any
  ts: number
}

export function enqueueOffline(item: Omit<OfflineItem, 'ts'>) {
  const list: OfflineItem[] = uni.getStorageSync(KEY) || []
  list.push({ ...item, ts: Date.now() })
  uni.setStorageSync(KEY, list)
}

export function getOfflineQueue(): OfflineItem[] {
  return uni.getStorageSync(KEY) || []
}

export function removeOfflineItem(id: string) {
  const list = getOfflineQueue().filter((i) => i.id !== id)
  uni.setStorageSync(KEY, list)
}
