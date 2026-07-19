import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { ApiError, ensureCsrf, jsonBody, request, resetCsrf } from '@/services/http'
import type { CurrentUser, Role } from '@/types/api'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<CurrentUser | null>(null)
  const initialized = ref(false)
  const bootstrapError = ref('')

  const isAdmin = computed(() => hasRole('ADMIN'))
  const canOperate = computed(() => hasRole('ADMIN') || hasRole('OPERATOR'))
  const canApprove = computed(() => hasRole('ADMIN') || hasRole('APPROVER'))

  function hasRole(role: Role) {
    return user.value?.roles.includes(role) ?? false
  }

  async function bootstrap() {
    if (initialized.value) return user.value
    try {
      await ensureCsrf()
      user.value = await request<CurrentUser>('/api/auth/me')
      bootstrapError.value = ''
    } catch (error) {
      if (!(error instanceof ApiError) || ![0, 401].includes(error.status)) throw error
      user.value = null
      bootstrapError.value = error.status === 0 ? error.message : ''
    } finally {
      initialized.value = true
    }
    return user.value
  }

  async function login(username: string, password: string) {
    await ensureCsrf()
    user.value = await request<CurrentUser>('/api/auth/login', {
      method: 'POST',
      ...jsonBody({ username, password }),
    })
    bootstrapError.value = ''
    initialized.value = true
    return user.value
  }

  async function logout() {
    try {
      await request<void>('/logout', { method: 'POST' })
    } finally {
      user.value = null
      initialized.value = true
      resetCsrf()
    }
  }

  return { user, initialized, bootstrapError, isAdmin, canOperate, canApprove, hasRole, bootstrap, login, logout }
})
