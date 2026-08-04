import type { ApiErrorPayload } from '../types'

export class ApiError extends Error {
  status: number
  code: string
  fieldErrors: Record<string, string>

  /** 创建便于页面展示的接口错误。 */
  constructor(status: number, payload: ApiErrorPayload) {
    super(payload.message)
    this.name = 'ApiError'
    this.status = status
    this.code = payload.code
    this.fieldErrors = payload.fieldErrors ?? {}
  }
}

/** 发送同源 API 请求，并统一处理 JSON 和错误响应。 */
export async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const hasJsonBody = options.body !== undefined && !(options.body instanceof FormData)
  const response = await fetch(path, {
    ...options,
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      ...(hasJsonBody ? { 'Content-Type': 'application/json' } : {}),
      ...options.headers,
    },
  })

  if (!response.ok) {
    const payload = await response.json().catch(() => ({
      code: 'REQUEST_FAILED',
      message: `请求失败（${response.status}）`,
    })) as ApiErrorPayload
    throw new ApiError(response.status, payload)
  }

  if (response.status === 204) {
    return undefined as T
  }
  return await response.json() as T
}
