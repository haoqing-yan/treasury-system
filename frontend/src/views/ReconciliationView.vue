<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { AlertTriangle, ArrowDownLeft, ArrowUpRight, Link2, RefreshCw, Search, Sparkles } from 'lucide-vue-next'
import BaseModal from '@/components/BaseModal.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { jsonBody, request } from '@/services/http'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { useFormat } from '@/composables/useFormat'
import type { AccountChannel, BankTransaction, Payment, ReconciliationStatus, ReconciliationSummary } from '@/types/api'

const auth = useAuthStore()
const toast = useToastStore()
const format = useFormat()
const transactions = ref<BankTransaction[]>([])
const payments = ref<Payment[]>([])
const summary = ref<ReconciliationSummary>({ totalCount: 0, matchedCount: 0, unmatchedCount: 0, exceptionCount: 0, matchedAmount: 0, unmatchedAmount: 0 })
const loading = ref(true)
const processing = ref(false)
const search = ref('')
const status = ref<ReconciliationStatus | ''>('')
const channel = ref<AccountChannel | ''>('')
const modalOpen = ref(false)
const modalMode = ref<'match' | 'exception'>('match')
const selectedTransaction = ref<BankTransaction | null>(null)
const selectedPaymentId = ref(0)
const exceptionReason = ref('')
const channelNames: Record<AccountChannel, string> = { BANK: '银行', ALIPAY: '支付宝', WECHAT: '微信支付' }

const filtered = computed(() => transactions.value.filter(transaction => {
  const text = `${transaction.transactionNo} ${transaction.counterpartyName} ${transaction.purpose}`.toLowerCase()
  return (!search.value || text.includes(search.value.toLowerCase()))
    && (!channel.value || transaction.channel === channel.value)
    && (!status.value || transaction.reconciliationStatus === status.value)
}))
const matchedPaymentIds = computed(() => new Set(transactions.value.map(item => item.matchedPaymentId).filter(Boolean)))
const matchablePayments = computed(() => payments.value.filter(payment => payment.status === 'PAID' && !matchedPaymentIds.value.has(payment.id)))

onMounted(load)

async function load() {
  loading.value = true
  try {
    ;[transactions.value, summary.value, payments.value] = await Promise.all([
      request<BankTransaction[]>('/api/reconciliations'),
      request<ReconciliationSummary>('/api/reconciliations/summary'),
      request<Payment[]>('/api/payments'),
    ])
  } catch (error) { toast.show('对账数据加载失败', error instanceof Error ? error.message : '请稍后重试', true) }
  finally { loading.value = false }
}

async function autoMatch() {
  processing.value = true
  try {
    const result = await request<{ matchedCount: number }>('/api/reconciliations/auto-match', { method: 'POST' })
    toast.show('自动对账完成', result.matchedCount ? `成功匹配 ${result.matchedCount} 笔渠道流水。` : '没有发现新的可匹配流水。')
    await load()
  } catch (error) { toast.show('自动对账失败', error instanceof Error ? error.message : '请稍后重试', true) }
  finally { processing.value = false }
}

function openMatch(transaction: BankTransaction) {
  selectedTransaction.value = transaction
  modalMode.value = 'match'
  selectedPaymentId.value = matchablePayments.value[0]?.id ?? 0
  modalOpen.value = true
}

function openException(transaction: BankTransaction) {
  selectedTransaction.value = transaction
  modalMode.value = 'exception'
  exceptionReason.value = ''
  modalOpen.value = true
}

async function submitModal() {
  if (!selectedTransaction.value) return
  if (modalMode.value === 'match' && !selectedPaymentId.value) {
    toast.show('没有可匹配付款单', '请选择一笔未匹配的已支付付款单。', true)
    return
  }
  if (modalMode.value === 'exception' && !exceptionReason.value.trim()) {
    toast.show('请填写异常原因', '异常原因不能为空。', true)
    return
  }
  processing.value = true
  try {
    const path = modalMode.value === 'match'
      ? `/api/reconciliations/${selectedTransaction.value.id}/match`
      : `/api/reconciliations/${selectedTransaction.value.id}/exception`
    const body = modalMode.value === 'match'
      ? { paymentId: selectedPaymentId.value }
      : { reason: exceptionReason.value.trim() }
    await request<BankTransaction>(path, { method: 'POST', ...jsonBody(body) })
    toast.show(modalMode.value === 'match' ? '手工匹配成功' : '异常已登记', '流水状态与审计日志已同步更新。')
    modalOpen.value = false
    await load()
  } catch (error) { toast.show('操作失败', error instanceof Error ? error.message : '请稍后重试', true) }
  finally { processing.value = false }
}
</script>

<template>
  <section class="page-view">
    <div class="page-intro">
      <div><p class="section-kicker">CHANNEL RECONCILIATION</p><h2>渠道对账</h2><p>集中核对银行、支付宝和微信支付流水，跟踪未达账项和异常事项。</p></div>
      <div class="quick-actions"><button class="button secondary" @click="load"><RefreshCw :size="14" />刷新流水</button><button v-if="auth.canApprove" class="button primary" :disabled="processing" @click="autoMatch"><Sparkles :size="14" />{{ processing ? '匹配中…' : '自动匹配' }}</button></div>
    </div>

    <div class="reconciliation-summary">
      <article><span>流水总数</span><strong>{{ summary.totalCount }}</strong><small>各资金渠道已同步</small></article>
      <article class="matched"><span>已匹配</span><strong>{{ summary.matchedCount }}</strong><small>金额 ¥ {{ format.wan(summary.matchedAmount) }}</small></article>
      <article class="unmatched"><span>待匹配</span><strong>{{ summary.unmatchedCount }}</strong><small>金额 ¥ {{ format.wan(summary.unmatchedAmount) }}</small></article>
      <article class="exception"><span>异常事项</span><strong>{{ summary.exceptionCount }}</strong><small>等待人工核查</small></article>
    </div>

    <article class="panel list-panel">
      <div class="toolbar"><label class="search-box wide"><Search :size="15" /><input v-model="search" placeholder="搜索流水号、对方户名或摘要" /></label><div class="exception-filters"><select v-model="channel"><option value="">全部渠道</option><option value="BANK">银行</option><option value="ALIPAY">支付宝</option><option value="WECHAT">微信支付</option></select><select v-model="status"><option value="">全部状态</option><option value="UNMATCHED">待匹配</option><option value="MATCHED">已匹配</option><option value="EXCEPTION">异常</option></select></div></div>
      <div class="table-wrap"><table class="data-table reconciliation-table"><thead><tr><th>交易时间 / 流水号</th><th>账户</th><th>对方户名</th><th>方向</th><th>金额</th><th>摘要</th><th>匹配结果</th><th class="align-right">操作</th></tr></thead><tbody>
        <tr v-if="loading"><td colspan="8"><div class="empty-state">正在同步渠道流水…</div></td></tr>
        <tr v-for="transaction in filtered" v-else :key="transaction.id">
          <td><b>{{ format.dateTime(transaction.transactionTime) }}</b><small>{{ transaction.transactionNo }}</small></td>
          <td><b>{{ transaction.bankAccountName }}</b><small><span class="channel-text" :class="`channel-text-${transaction.channel}`">{{ channelNames[transaction.channel] }}</span> · {{ transaction.bankAccountNoMasked }}</small></td>
          <td><b>{{ transaction.counterpartyName }}</b><small>{{ transaction.counterpartyAccountNoMasked }}</small></td>
          <td><span class="direction" :class="transaction.direction === 'INFLOW' ? 'in' : 'out'"><ArrowDownLeft v-if="transaction.direction === 'INFLOW'" :size="13" /><ArrowUpRight v-else :size="13" />{{ transaction.direction === 'INFLOW' ? '流入' : '流出' }}</span></td>
          <td class="amount" :class="transaction.direction === 'INFLOW' ? 'positive-amount' : ''">{{ transaction.direction === 'INFLOW' ? '+' : '-' }} {{ format.symbol(transaction.currency) }} {{ format.number(transaction.amount) }}</td>
          <td>{{ transaction.purpose }}</td>
          <td><StatusBadge :status="transaction.reconciliationStatus" /><small v-if="transaction.matchedPaymentNo">付款单 {{ transaction.matchedPaymentNo }}</small><small v-else-if="transaction.matchMessage">{{ transaction.matchMessage }}</small></td>
          <td><div class="row-actions"><template v-if="transaction.reconciliationStatus === 'UNMATCHED' && auth.canApprove"><button v-if="transaction.direction === 'OUTFLOW'" class="row-button primary" @click="openMatch(transaction)"><Link2 :size="11" />手工匹配</button><button class="row-button danger" @click="openException(transaction)"><AlertTriangle :size="11" />标记异常</button></template><span v-else>—</span></div></td>
        </tr>
        <tr v-if="!loading && !filtered.length"><td colspan="8"><div class="empty-state">没有符合条件的渠道流水</div></td></tr>
      </tbody></table></div>
    </article>

    <BaseModal :open="modalOpen" :kicker="modalMode === 'match' ? 'MANUAL MATCH' : 'EXCEPTION REVIEW'" :title="modalMode === 'match' ? '手工匹配付款单' : '登记异常流水'" width="small" @close="modalOpen = false">
      <div v-if="selectedTransaction" class="transaction-preview"><span>{{ selectedTransaction.transactionNo }}</span><strong>{{ selectedTransaction.counterpartyName }}</strong><em>{{ format.symbol(selectedTransaction.currency) }} {{ format.number(selectedTransaction.amount) }}</em></div>
      <label v-if="modalMode === 'match'" class="field"><span>选择已支付付款单 *</span><select v-model.number="selectedPaymentId" required><option v-if="!matchablePayments.length" :value="0">暂无未匹配的已支付付款单</option><option v-for="payment in matchablePayments" :key="payment.id" :value="payment.id">{{ payment.paymentNo }} · {{ payment.payeeName }} · {{ format.symbol(payment.currency) }} {{ format.number(payment.amount) }}</option></select></label>
      <label v-else class="field"><span>异常原因 *</span><textarea v-model="exceptionReason" maxlength="240" placeholder="例如：缺少业务单据、金额不一致或对方信息异常" /></label>
      <template #footer><button class="button secondary" @click="modalOpen = false">取消</button><button class="button primary" :disabled="processing" @click="submitModal">{{ processing ? '处理中…' : '确认提交' }}</button></template>
    </BaseModal>
  </section>
</template>
