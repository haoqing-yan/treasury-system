<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowDownLeft, ArrowUpRight, Landmark, Plus, RefreshCw, ShieldAlert, Wallet } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import StatusBadge from '@/components/StatusBadge.vue'
import { request } from '@/services/http'
import { useFormat } from '@/composables/useFormat'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import type { DashboardData } from '@/types/api'

const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()
const format = useFormat()
const data = ref<DashboardData | null>(null)
const loading = ref(true)
const updatedAt = ref('')
const maxFlow = computed(() => Math.max(1, ...(data.value?.cashFlow.flatMap(day => [day.inflow, day.outflow]) ?? [1])))
const bankTotal = computed(() => data.value?.balanceByBank.reduce((sum, item) => sum + item.value, 0) ?? 0)

onMounted(load)

async function load() {
  loading.value = true
  try {
    data.value = await request<DashboardData>('/api/dashboard')
    updatedAt.value = new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date())
  } catch (error) {
    toast.show('驾驶舱加载失败', error instanceof Error ? error.message : '请稍后重试', true)
  } finally { loading.value = false }
}
</script>

<template>
  <section class="page-view">
    <div class="page-intro dashboard-intro">
      <div><p class="section-kicker">GOOD MORNING</p><h2>集团资金，尽在掌握</h2><p>数据更新至 {{ updatedAt || '—' }}，以下金额默认折算口径为人民币。</p></div>
      <div class="quick-actions"><button class="button secondary" @click="load"><RefreshCw :size="14" />刷新</button><button v-if="auth.canOperate" class="button primary" @click="router.push('/payments?create=1')"><Plus :size="14" />新建付款</button></div>
    </div>

    <div v-if="loading" class="metric-grid"><div v-for="index in 4" :key="index" class="metric-card skeleton" /></div>
    <template v-else-if="data">
      <div class="metric-grid">
        <article class="metric-card featured"><div class="metric-top"><span>集团资金余额</span><i><Wallet :size="15" /></i></div><strong>{{ format.moneyWan(data.overview.totalBalance) }} <small>万元</small></strong><p><em>● 实时</em>覆盖 {{ data.overview.accountCount }} 个资金账户</p></article>
        <article class="metric-card"><div class="metric-top"><span>可用资金</span><i><Landmark :size="15" /></i></div><strong>{{ format.moneyWan(data.overview.availableBalance) }} <small>万元</small></strong><p>受限及冻结资金已扣除</p></article>
        <article class="metric-card"><div class="metric-top"><span>今日资金流出</span><i><ArrowUpRight :size="15" /></i></div><strong>{{ format.moneyWan(data.overview.todayOutflow) }} <small>万元</small></strong><p class="warning">{{ data.overview.pendingPaymentCount }} 笔待审批</p></article>
        <article class="metric-card"><div class="metric-top"><span>未来七日净流量</span><i><ArrowDownLeft :size="15" /></i></div><strong>{{ data.overview.nextSevenDaysNet >= 0 ? '+' : '-' }}{{ format.moneyWan(Math.abs(data.overview.nextSevenDaysNet)) }} <small>万元</small></strong><p>{{ data.overview.nextSevenDaysNet >= 0 ? '预计资金净流入' : '关注资金缺口' }}</p></article>
      </div>

      <div class="dashboard-grid">
        <article class="panel cashflow-panel">
          <header class="panel-heading"><div><span>7 DAY FORECAST</span><h3>未来七日资金流</h3></div><div class="legend"><span><i class="inflow-dot" />流入</span><span><i class="outflow-dot" />流出</span></div></header>
          <div class="cashflow-chart">
            <div v-for="day in data.cashFlow" :key="day.date" class="flow-day">
              <div class="flow-bars"><i class="flow-bar inflow" :style="{ height: `${Math.max(3, day.inflow / maxFlow * 138)}px` }" :title="`流入 ${format.wan(day.inflow)}`" /><i class="flow-bar outflow" :style="{ height: `${Math.max(3, day.outflow / maxFlow * 138)}px` }" :title="`流出 ${format.wan(day.outflow)}`" /></div>
              <span>{{ format.shortDate(day.date) }}</span>
            </div>
          </div>
        </article>

        <article class="panel bank-panel">
          <header class="panel-heading"><div><span>CHANNEL POSITION</span><h3>资金渠道分布</h3></div><button @click="router.push('/accounts')">详情 →</button></header>
          <div class="bank-distribution"><div class="bank-total"><span>人民币账户合计</span><strong>¥ {{ format.wan(bankTotal) }}</strong></div><div v-for="bank in data.balanceByBank" :key="bank.name" class="bank-row"><span>{{ bank.name }}</span><div><i :style="{ width: `${bankTotal ? bank.value / bankTotal * 100 : 0}%` }" /></div><em>{{ format.wan(bank.value) }}</em></div></div>
        </article>
      </div>

      <div class="dashboard-grid lower-grid">
        <article class="panel">
          <header class="panel-heading"><div><span>RECENT PAYMENTS</span><h3>近期付款动态</h3></div><button @click="router.push('/payments')">全部付款 →</button></header>
          <div class="table-wrap"><table><thead><tr><th>付款单</th><th>收款方</th><th>金额</th><th>状态</th></tr></thead><tbody><tr v-for="payment in data.recentPayments.slice(0, 5)" :key="payment.id"><td><b>{{ payment.paymentNo }}</b><small>{{ payment.purpose }}</small></td><td>{{ payment.payeeName }}</td><td class="amount">{{ format.symbol(payment.currency) }} {{ format.number(payment.amount) }}</td><td><StatusBadge :status="payment.status" /></td></tr></tbody></table></div>
        </article>
        <article class="panel">
          <header class="panel-heading"><div><span>RISK CENTER</span><h3>风险与待办</h3></div><em class="count-badge">{{ data.alerts.length }}</em></header>
          <div class="risk-list"><div v-for="alert in data.alerts" :key="`${alert.title}-${alert.description}`" class="risk-item"><span :class="alert.level"><ShieldAlert :size="14" /></span><div><strong>{{ alert.title }}</strong><small>{{ alert.description }}</small></div></div><div v-if="!data.alerts.length" class="empty-state">当前没有需要处理的风险事项</div></div>
        </article>
      </div>
    </template>
  </section>
</template>
