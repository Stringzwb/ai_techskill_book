import { reactive, readonly } from 'vue'
import type { LoginPayload, RegisterPayload } from '../services/auth'
import * as authApi from '../services/auth'
import { ApiError } from '../services/http'
import type { UserProfile } from '../types'

const state = reactive({
  user: null as UserProfile | null,
  initialized: false,
  loading: false,
})

/** 首次进入应用时恢复服务端会话。 */
async function initialize() {
  if (state.initialized || state.loading) return
  state.loading = true
  try {
    const response = await authApi.fetchCurrentUser()
    state.user = response.user
  } catch (error) {
    if (!(error instanceof ApiError && error.status === 401)) throw error
    state.user = null
  } finally {
    state.loading = false
    state.initialized = true
  }
}

/** 登录并写入全局用户状态。 */
async function signIn(payload: LoginPayload) {
  const response = await authApi.login(payload)
  state.user = response.user
  state.initialized = true
}

/** 注册并写入全局用户状态。 */
async function signUp(payload: RegisterPayload) {
  const response = await authApi.register(payload)
  state.user = response.user
  state.initialized = true
}

/** 注销并清空全局用户状态。 */
async function signOut() {
  try {
    await authApi.logout()
  } finally {
    state.user = null
    state.initialized = true
  }
}

/** 在修改资料后同步当前用户。 */
function setUser(user: UserProfile) {
  state.user = user
}

export const authStore = {
  state: readonly(state),
  initialize,
  signIn,
  signUp,
  signOut,
  setUser,
}
