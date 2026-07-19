<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, Eye, EyeOff, KeyRound, ShieldCheck, UserRound } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/services/http'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const username = ref('admin')
const password = ref('admin123')
const showPassword = ref(false)
const loading = ref(false)
const error = ref(auth.bootstrapError)

const roles = [
  { name: '资金经办', note: '申请与计划', mark: '经', username: 'operator', password: 'operator123' },
  { name: '资金审批', note: '复核与驳回', mark: '审', username: 'approver', password: 'approver123' },
  { name: '系统管理员', note: '全部权限', mark: '管', username: 'admin', password: 'admin123' },
]

function chooseRole(role: typeof roles[number]) {
  username.value = role.username
  password.value = role.password
  error.value = ''
}

async function submit() {
  loading.value = true
  error.value = ''
  try {
    await auth.login(username.value, password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect)
  } catch (caught) {
    error.value = caught instanceof ApiError ? caught.message : '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-shell">
    <section class="login-story">
      <div class="story-top">
        <div class="brand"><div class="brand-mark">T</div><div><strong>司库云控</strong><span>TREASURY CONTROL</span></div></div>
        <span class="environment-badge"><i />安全演示环境</span>
      </div>
      <div class="story-copy">
        <span>01 / 资金安全</span>
        <h1>每一笔资金，<br>都清晰、可控、可追溯。</h1>
        <p>统一账户、付款、计划与风险视图，让集团资金管理从“事后统计”走向“实时运营”。</p>
      </div>
      <div class="story-stats">
        <div><strong>100%</strong><span>关键操作留痕</span></div>
        <div><strong>7×24</strong><span>资金动态监控</span></div>
        <div><strong>3 层</strong><span>岗位制衡控制</span></div>
      </div>
      <div class="story-pattern" aria-hidden="true"><i /><i /><i /><i /></div>
    </section>

    <section class="login-panel">
      <div class="login-form-wrap">
        <div class="login-heading"><span>WELCOME BACK</span><h2>登录司库工作台</h2><p>选择演示岗位，或输入对应账号。</p></div>
        <div class="demo-roles">
          <button v-for="role in roles" :key="role.username" :class="{ selected: username === role.username }" @click="chooseRole(role)">
            <span>{{ role.mark }}</span><div><strong>{{ role.name }}</strong><small>{{ role.note }}</small></div>
          </button>
        </div>
        <form @submit.prevent="submit">
          <label class="login-field"><span>账号</span><div><UserRound :size="15" /><input v-model="username" autocomplete="username" required /></div></label>
          <label class="login-field"><span>密码</span><div><KeyRound :size="15" /><input v-model="password" :type="showPassword ? 'text' : 'password'" autocomplete="current-password" required /><button type="button" :aria-label="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword"><EyeOff v-if="showPassword" :size="15" /><Eye v-else :size="15" /></button></div></label>
          <p v-if="error" class="login-error">{{ error }}</p>
          <button class="login-submit" type="submit" :disabled="loading"><span>{{ loading ? '正在验证…' : '进入工作台' }}</span><ArrowRight :size="18" /></button>
        </form>
        <p class="login-help"><ShieldCheck :size="12" /> 演示账号已使用数据库加密存储，生产环境可继续接入企业统一认证。</p>
      </div>
      <footer>© 2026 华辰控股集团 · 司库云控平台 V0.1</footer>
    </section>
  </main>
</template>
