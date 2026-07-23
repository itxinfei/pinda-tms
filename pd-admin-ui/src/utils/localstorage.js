// 修改点：localStorage 读写增加 try-catch，避免非法 JSON / 存储不可用导致页面崩溃
const db = {
  save (key, value) {
    try {
      localStorage.setItem(key, JSON.stringify(value))
    } catch (e) {
      console.error('localStorage save error:', e)
    }
  },
  get (key, defaultValue = {}) {
    const item = localStorage.getItem(key)
    if (item === null || item === undefined) {
      return defaultValue
    }
    try {
      return JSON.parse(item)
    } catch (e) {
      console.error('localStorage parse error for key:', key, e)
      return defaultValue
    }
  },
  remove (key) {
    localStorage.removeItem(key)
  },
  clear () {
    localStorage.clear()
  }
}

export default db
