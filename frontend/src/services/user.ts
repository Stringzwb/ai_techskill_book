import type { UserProfile } from '../types'
import { apiRequest } from './http'

export interface UpdateProfilePayload {
  username: string
  phone: string
  email: string
}

/** 保存当前用户允许修改的个人资料。 */
export function updateProfile(payload: UpdateProfilePayload) {
  return apiRequest<UserProfile>('/api/users/me', {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}
