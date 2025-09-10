// 统一的头像URL生成工具
// 规则：
// - avatar 以 'avatar_' 开头 => 使用 /api/user/avatar/{id}
// - avatar 以 '/api/user/avatar/' 开头 => 自动补全 http://localhost:8080 前缀
// - 其它情况 => 直接返回 avatar
// - 缺失或异常 => 返回占位头像
export interface AvatarUserLike {
  id?: number | string
  avatar?: string | null
}

const FALLBACK = 'https://avatars.githubusercontent.com/u/0?v=4'
const API_PREFIX = 'http://localhost:8080'

export function getUserAvatarUrl(user?: AvatarUserLike): string {
  if (!user) return FALLBACK
  const ava = typeof user.avatar === 'string' ? user.avatar : ''
  if (!ava) return FALLBACK

  if (ava.startsWith('avatar_')) {
    if (user.id !== undefined && user.id !== null && String(user.id).length > 0) {
      return `${API_PREFIX}/api/user/avatar/${String(user.id)}`
    }
    return FALLBACK
  }
  if (ava.startsWith('/api/user/avatar/')) {
    return `${API_PREFIX}${ava}`
  }
  return ava
}