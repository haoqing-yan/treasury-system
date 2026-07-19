<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { CircleAlert, Plus, Search, Send, ShieldCheck } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'
import BaseModal from '@/components/BaseModal.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { jsonBody, request } from '@/services/http'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { useFormat } from '@/composables/useFormat'
import type { AccountChannel, BankAccount, Payment, PaymentStatus } from '@/types/api'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()
const format = useFormat()
const payments = ref<Payment[]>([])
const accounts = ref<BankAccount[]>([])
const loading = ref(true)
const search = ref('')
const status = ref<PaymentStatus | ''>('')
const modalOpen = ref(false)
const confirmOpen = ref(false)
const saving = ref(false)
const currentAction = ref<{ payment: Payment; action: 'submit' | 'approve' | 'reject' | 'execute' } | null>(null)
const rejectReason = ref('')
const form = reactive({ payerAccountId: 0, payeeName: '', payeeBankName: '', payeeAccountNo: '', amount: 0, purpose: '' })

const statuses: Array<{ value: PaymentStatus | ''; label: string }> = [
  { value: '', label: '全部' }, { value: 'DRAFT', label: '草稿' }, { value: 'PENDING', label: '待审批' },
  { value: 'APPROVED', label: '待支付' }, { value: 'PAID', label: '已支付' }, { value: 'REJECTED', label: '已驳回' },
]
const channelNames: Record<AccountChannel, string> = { BANK: '银行', ALIPAY: '支付宝', WECHAT: '微信支付' }
const filtered = computed(() => payments.value.filter(payment => {
  const text = `${payment.paymentNo} ${payment.payeeName} ${payment.purpose}`.toLowerCase()
  return (!search.value || text.includes(search.value.toLowerCase())) && (!status.value || payment.status === status.value)
}))
const activeAccounts = computed(() => accounts.value.filter(account => account.status === 'ACTIVE'))
const selectedAccount = computed(() => accounts.value.find(account => account.id === form.payerAccountId))
const confirmText = computed(() => {
  const action = currentAction.value?.action
  return action === 'submit' ? ['提交付款审批', '提交后将进入审批岗位待办，确认继续？']
    : action === 'approve' ? ['审批通过付款', '通过后该付款将进入支付渠道执行队列，确认继续？']
      : action === 'reject' ? ['驳回付款申请', '请填写驳回原因，内容将同步写入付款单和审计日志。']
        : [`模拟${channelNames[currentAction.value?.payment.payerChannel ?? 'BANK']}支付`, `系统将模拟${channelNames[currentAction.value?.payment.payerChannel ?? 'BANK']}渠道返回支付成功，并扣减付款账户余额。`]
})

onMounted(async () => {
  await load()
  if (route.query.create === '1' && auth.canOperate) {
    modalOpen.value = true
    void router.replace('/payments')
  }
})

async function load() {
  loading.value = true
  try {
    ;[payments.value, accounts.value] = await Promise.all([
      request<Payment[]>('/api/payments'), request<BankAccount[]>('/api/accounts'),
    ])
    if (!form.payerAccountId && activeAccounts.value.length) form.payerAccountId = activeAccounts.value[0]!.id
  } catch (error) { toast.show('付款数据加载失败', error instanceof Error ? error.message : '请稍后重试', true) }
  finally { loading.value = false }
}

function count(value: PaymentStatus | '') {
  return value ? payments.value.filter(item => item.status === value).length : payments.value.length
}

function actions(payment: Payment) {
  const result: Array<{ action: 'submit' | 'approve' | 'reject' | 'execute'; label: string; danger?: boolean }> = []
  if (payment.status === 'DRAFT' && (auth.isAdmin || (auth.canOperate && payment.applicant === auth.user?.username))) result.push({ action: 'submit', label: '提交' })
  if (payment.status === 'PENDING' && auth.canApprove) result.push({ action: 'approve', label: '通过' }, { action: 'reject', label: '驳回', danger: true })
  if (payment.status === 'APPROVED' && auth.isAdmin) result.push({ action: 'execute', label: payment.payerChannel === 'BANK' ? '发送银行' : `${channelNames[payment.payerChannel]}支付` })
  return result
}

function ask(payment: Payment, action: 'submit' | 'approve' | 'reject' | 'execute') {
  currentAction.value = { payment, action }
  rejectReason.value = ''
  confirmOpen.value = true
}

async function executeAction() {
  if (!currentAction.value) return
  if (currentAction.value.action === 'reject' && !rejectReason.value.trim()) {
    toast.show('请填写驳回原因', '驳回原因不能为空。', true)
    return
  }
  saving.value = true
  try {
    const { payment, action } = currentAction.value
    await request<Payment>(`/api/payments/${payment.id}/${action}`, {
      method: 'POST', ...(action === 'reject' ? jsonBody({ reason: rejectReason.value.trim() }) : {}),
    })
    toast.show('操作成功', action === 'execute' ? `${channelNames[payment.payerChannel]}渠道已返回支付成功状态。` : '付款单状态已更新。')
    confirmOpen.value = false
    await load()
  } catch (error) { toast.show('操作失败', error instanceof Error ? error.message : '请稍后重试', true) }
  finally { saving.value = false }
}

async function create() {
  if (!selectedAccount.value) return
  saving.value = true
  try {
    const payment = await request<Payment>('/api/payments', {
      method: 'POST',
      ...jsonBody({ ...form, currency: selectedAccount.value.currency }),
    })
    toast.show('付款草稿已保存', payment.riskFlag ? payment.riskMessage ?? '存在风险提示' : `单号：${payment.paymentNo}`)
    modalOpen.value = false
    Object.assign(form, { payerAccountId: activeAccounts.value[0]?.id ?? 0, payeeName: '', payeeBankName: '', payeeAccountNo: '', amount: 0, purpose: '' })
    await load()
  } catch (error) { toast.show('保存失败', error instanceof Error ? error.message : '请稍后重试', true) }
  finally { saving.value = false }
}
</script>

<template>
  <section class="page-view">
    <div class="page-intro"><div><p class="section-kicker">PAYMENT CONTROL</p><h2>付款管理</h2><p>统一管理银行、支付宝和微信支付渠道的付款申请、复核与执行。</p></div><button v-if="auth.canOperate" class="button primary" @click="modalOpen = true"><Plus :size="14" />新建付款</button></div>
    <div class="payment-tabs"><button v-for="item in statuses" :key="item.value" :class="{ active: status === item.value }" @click="status = item.value">{{ item.label }} <span>{{ count(item.value) }}</span></button></div>
    <article class="panel list-panel">
      <div class="toolbar"><label class="search-box wide"><Search :size="15" /><input v-model="search" placeholder="搜索付款单号、收款方或用途" /></label><span class="toolbar-note"><ShieldCheck :size="13" />关键操作均写入审计日志</span></div>
      <div class="table-wrap"><table class="data-table"><thead><tr><th>付款单 / 用途</th><th>付款账户</th><th>收款方</th><th>金额</th><th>申请人</th><th>状态</th><th class="align-right">操作</th></tr></thead><tbody>
        <tr v-if="loading"><td colspan="7"><div class="empty-state">正在加载付款数据…</div></td></tr>
        <tr v-for="payment in filtered" v-else :key="payment.id"><td><b>{{ payment.paymentNo }} <em v-if="payment.riskFlag" class="risk-flag">风险提示</em></b><small>{{ payment.purpose }}</small></td><td><b>{{ payment.payerAccountName }}</b><small><span class="channel-text" :class="`channel-text-${payment.payerChannel}`">{{ channelNames[payment.payerChannel] }}</span> · {{ payment.organizationName }}</small></td><td><b>{{ payment.payeeName }}</b><small>{{ payment.payeeAccountNoMasked }}</small></td><td class="amount">{{ format.symbol(payment.currency) }} {{ format.number(payment.amount) }}</td><td>{{ payment.applicant }}</td><td><StatusBadge :status="payment.status" /><small v-if="payment.rejectReason">{{ payment.rejectReason }}</small></td><td><div class="row-actions"><button v-for="action in actions(payment)" :key="action.action" class="row-button" :class="{ danger: action.danger, primary: !action.danger }" @click="ask(payment, action.action)">{{ action.label }}</button><span v-if="!actions(payment).length">—</span></div></td></tr>
        <tr v-if="!loading && !filtered.length"><td colspan="7"><div class="empty-state">没有符合条件的付款单</div></td></tr>
      </tbody></table></div>
    </article>

    <BaseModal :open="modalOpen" kicker="NEW PAYMENT" title="新建付款申请" @close="modalOpen = false">
      <form id="payment-form" @submit.prevent="create">
        <div class="form-section-title"><span>01</span><div><strong>付款信息</strong><small>选择集团内正常运行的付款账户</small></div></div>
        <div class="form-grid">
          <label class="field full"><span>付款账户 *</span><select v-model.number="form.payerAccountId" required><option v-for="account in activeAccounts" :key="account.id" :value="account.id">[{{ channelNames[account.channel] }}] {{ account.accountName }} · {{ account.accountNoMasked }} · 可用 {{ format.symbol(account.currency) }} {{ format.number(account.availableBalance) }}</option></select></label>
          <label class="field"><span>收款方名称 *</span><input v-model="form.payeeName" maxlength="120" required placeholder="请输入企业或个人名称" /></label><label class="field"><span>收款机构 / 平台 *</span><input v-model="form.payeeBankName" maxlength="80" required placeholder="例如：中国建设银行、支付宝" /></label>
          <label class="field"><span>收款账号 / 用户标识 *</span><input v-model="form.payeeAccountNo" pattern="[A-Za-z0-9@._-]{6,64}" required placeholder="银行账号、商户号或用户标识" /></label><label class="field"><span>付款金额 *</span><input v-model.number="form.amount" type="number" min="0.01" step="0.01" required placeholder="0.00" /></label>
          <label class="field full"><span>付款用途 *</span><textarea v-model="form.purpose" maxlength="240" required placeholder="请填写合同、项目或费用说明" /></label>
        </div>
        <div class="control-tip"><CircleAlert :size="15" /><p>系统将在保存时检查付款账户状态、币种和 24 小时内的重复付款风险。</p></div>
      </form>
      <template #footer><button class="button secondary" @click="modalOpen = false">取消</button><button class="button primary" type="submit" form="payment-form" :disabled="saving"><Send :size="13" />{{ saving ? '保存中…' : '保存为草稿' }}</button></template>
    </BaseModal>

    <BaseModal :open="confirmOpen" :kicker="currentAction?.action.toUpperCase()" :title="confirmText[0]" width="small" @close="confirmOpen = false">
      <div class="confirm-content"><span><CircleAlert :size="20" /></span><p>{{ confirmText[1] }}</p><label v-if="currentAction?.action === 'reject'" class="field"><span>驳回原因 *</span><textarea v-model="rejectReason" maxlength="240" placeholder="请输入具体原因" /></label></div>
      <template #footer><button class="button secondary" @click="confirmOpen = false">取消</button><button class="button primary" :disabled="saving" @click="executeAction">{{ saving ? '处理中…' : '确认操作' }}</button></template>
    </BaseModal>
  </section>
</template>
