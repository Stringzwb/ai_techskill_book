import type { AuthResponse } from '../types'
import { apiRequest } from './http'

export interface LoginPayload {
  account: string
  password: string
}

export interface RegisterPayload {
  username: string
  phone: string
  email: string
  password: string
}

/** 使用用户名、手机号或邮箱登录。 */
export function login(payload: LoginPayload) {
  return apiRequest<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

/** 注册密码用户。 */
export function register(payload: RegisterPayload) {
  return apiRequest<AuthResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

/** 查询当前会话对应的用户。 */
export function fetchCurrentUser() {
  return apiRequest<AuthResponse>('/api/auth/me')
}

/** 注销当前浏览器会话。 */
export function logout() {
  return apiRequest<void>('/api/auth/logout', { method: 'POST' })
}
