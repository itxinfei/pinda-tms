// 脱敏工具：手机号 / 姓名展示脱敏，符合隐私合规
// 修改点：三端复用，无需改动。
export function maskPhone(phone?: string | null): string {
  if (!phone) return ''
  const s = String(phone)
  if (s.length < 7) return s
  return s.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

export function maskName(name?: string | null): string {
  if (!name) return ''
  const s = String(name)
  if (s.length <= 1) return s
  if (s.length === 2) return s[0] + '*'
  return s[0] + '*'.repeat(s.length - 2) + s[s.length - 1]
}
