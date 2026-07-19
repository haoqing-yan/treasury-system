import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppLayout from '@/layouts/AppLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
    {
      path: '/',
      component: AppLayout,
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', name: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { title: '资金驾驶舱', eyebrow: '资金运营总览' } },
        { path: 'accounts', name: 'accounts', component: () => import('@/views/AccountsView.vue'), meta: { title: '资金账户', eyebrow: '多渠道账户全生命周期' } },
        { path: 'payments', name: 'payments', component: () => import('@/views/PaymentsView.vue'), meta: { title: '付款管理', eyebrow: '支付流程与风险控制' } },
        { path: 'exceptions', name: 'exceptions', component: () => import('@/views/ExceptionCenterView.vue'), meta: { title: '异常中心', eyebrow: '异常工单闭环处置' } },
        { path: 'reconciliations', name: 'reconciliations', component: () => import('@/views/ReconciliationView.vue'), meta: { title: '渠道对账', eyebrow: '银行与支付平台流水匹配' } },
        { path: 'plans', name: 'plans', component: () => import('@/views/CashPlansView.vue'), meta: { title: '资金计划', eyebrow: '现金流预测' } },
        { path: 'audits', name: 'audits', component: () => import('@/views/AuditLogsView.vue'), meta: { title: '审计日志', eyebrow: '资金操作追溯', admin: true } },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  await auth.bootstrap()
  if (to.meta.public) return auth.user ? '/dashboard' : true
  if (!auth.user) return { name: 'login', query: { redirect: to.fullPath } }
  if (to.meta.admin && !auth.isAdmin) return '/dashboard'
  return true
})

export default router
