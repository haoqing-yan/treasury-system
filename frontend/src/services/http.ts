import type { ApiErrorBody, CsrfToken } from '@/types/api'

let csrf: CsrfToken | null = null

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly fields: Record<string, string> = {},
  ) {
    super(message)
  }
}

export async function ensureCsrf(): Promise<CsrfToken> {
  if (!csrf) csrf = await request<CsrfToken>('/api/auth/csrf', {}, false)
  return csrf
}

export function resetCsrf() {
  csrf = null
}

export async function request<T>(path: string, options: RequestInit = {}, addCsrf = true): Promise<T> {
  const method = (options.method ?? 'GET').toUpperCase()
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')

  if (options.body && typeof options.body === 'string') {
    headers.set('Content-Type', 'application/json')
  }
  if (addCsrf && !['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const token = await ensureCsrf()
    headers.set(token.headerName, token.token)
  }

  let response: Response
  try {
    response = await fetch(path, { ...options, headers, credentials: 'same-origin' })
  } catch {
    throw new ApiError('无法连接后端服务，请确认服务已启动后重试', 0)
  }
  const raw = await response.text()
  let body: unknown = null
  try { body = raw ? JSON.parse(raw) : null } catch { body = raw }

  if (!response.ok) {
    const error = (typeof body === 'object' && body ? body : {}) as ApiErrorBody
    const firstField = error.fields ? Object.values(error.fields)[0] : undefined
    throw new ApiError(firstField ?? error.message ?? '请求失败', response.status, error.fields)
  }
  return body as T
}

export function jsonBody(data: unknown): Pick<RequestInit, 'body'> {
  return { body: JSON.stringify(data) }
}
