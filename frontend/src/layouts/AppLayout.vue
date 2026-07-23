<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Landmark, LayoutDashboard, ListChecks, Menu, ReceiptText, ScrollText, ShieldAlert, ShieldCheck, WalletCards, X } from 'lucide-vue-next'
import ToastStack from '@/components/ToastStack.vue'
import { useAuthStore } from '@/stores/auth'
import { request } from '@/services/http'
import type { DashboardData, ExceptionSummary } from '@/types/api'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const sidebarOpen = ref(false)
const profileOpen = ref(false)
const pendingCount = ref(0)
const exceptionCount = ref(0)

const displayName = computed(() => auth.isAdmin ? '管理员' : auth.canApprove ? '审批专员' : '资金专员')
const roleName = computed(() => auth.isAdmin ? '系统管理员' : auth.canApprove ? '资金审批' : '资金经办')
const today = new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'short' }).format(new Date())
const navItems = computed(() => [
  { to: '/dashboard', label: '资金驾驶舱', icon: LayoutDashboard, show: true },
  { to: '/accounts', label: '资金账户', icon: Landmark, show: true },
  { to: '/payments', label: '付款管理', icon: ReceiptText, show: true, badge: pendingCount.value },
  { to: '/exceptions', label: '异常中心', icon: ShieldAlert, show: true, badge: exceptionCount.value },
  { to: '/reconciliations', label: '渠道对账', icon: ListChecks, show: true },
  { to: '/plans', label: '资金计划', icon: WalletCards, show: true },
  { to: '/audits', label: '审计日志', icon: ScrollText, show: auth.hasPermission('audit:read') },
])

onMounted(async () => {
  try { pendingCount.value = (await request<DashboardData>('/api/dashboard')).overview.pendingPaymentCount } catch { /* 页面自行展示错误 */ }
  try {
    const summary = await request<ExceptionSummary>('/api/exceptions/summary')
    exceptionCount.value = summary.openCount + summary.processingCount
  } catch { /* 页面自行展示错误 */ }
})

async function logout() {
  await auth.logout()
  await router.replace('/login')
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar" :class="{ open: sidebarOpen }">
      <div class="brand">
        <div class="brand-mark">T</div>
        <div><strong>司库云控</strong><span>TREASURY CONTROL</span></div>
        <button class="sidebar-close" aria-label="关闭菜单" @click="sidebarOpen = false"><X :size="18" /></button>
      </div>
      <div class="tenant-card">
        <span>当前管理组织</span><strong>华辰控股集团</strong><small><i />集团资金中心</small>
      </div>
      <nav class="main-nav" aria-label="主导航">
        <RouterLink v-for="item in navItems.filter(item => item.show)" :key="item.to" :to="item.to" class="nav-item" @click="sidebarOpen = false">
          <component :is="item.icon" :size="17" /><span>{{ item.label }}</span><em v-if="item.badge">{{ item.badge }}</em>
        </RouterLink>
      </nav>
      <div class="sidebar-footer">
        <div class="security-note"><span><ShieldCheck :size="15" /></span><div><strong>安全连接</strong><small>本次会话受保护</small></div></div>
        <p>V0.1 · 前后端分离版</p>
      </div>
    </aside>
    <button v-if="sidebarOpen" class="mobile-overlay" aria-label="关闭菜单" @click="sidebarOpen = false" />

    <main class="main-area">
      <header class="topbar">
        <div class="topbar-left">
          <button class="icon-button menu-button" aria-label="展开菜单" @click="sidebarOpen = true"><Menu :size="17" /></button>
          <div><p>{{ route.meta.eyebrow }}</p><h1>{{ route.meta.title }}</h1></div>
        </div>
        <div class="topbar-actions">
          <span class="date-chip">{{ today }}</span>
          <div class="user-menu">
            <button class="user-button" :aria-expanded="profileOpen" @click.stop="profileOpen = !profileOpen">
              <span class="avatar">{{ roleName.slice(0, 1) }}</span>
              <span class="user-copy"><strong>{{ displayName }}</strong><small>{{ roleName }}</small></span>
              <span>⌄</span>
            </button>
            <div v-if="profileOpen" class="user-dropdown">
              <div><strong>{{ auth.user?.username }}</strong><span>华辰控股集团</span></div>
              <button @click="logout">退出登录</button>
            </div>
          </div>
        </div>
      </header>
      <div class="content-wrap"><RouterView /></div>
    </main>
    <ToastStack />
  </div>
</template>
