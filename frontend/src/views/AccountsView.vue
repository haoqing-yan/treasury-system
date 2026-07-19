<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus, Search, TriangleAlert } from 'lucide-vue-next'
import BaseModal from '@/components/BaseModal.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { jsonBody, request } from '@/services/http'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { useFormat } from '@/composables/useFormat'
import type { AccountChannel, AccountStatus, AccountType, BankAccount } from '@/types/api'

const auth = useAuthStore()
const toast = useToastStore()
const format = useFormat()
const accounts = ref<BankAccount[]>([])
const loading = ref(true)
const search = ref('')
const status = ref('')
const channel = ref<AccountChannel | ''>('')
const modalOpen = ref(false)
const saving = ref(false)
const form = reactive({
  channel: 'BANK' as AccountChannel, organizationName: '华辰控股集团', bankName: '', bankCode: '', accountName: '', accountNo: '',
  currency: 'CNY', balance: 0, availableBalance: 0, lowBalanceThreshold: 100000,
  accountType: 'GENERAL' as AccountType, status: 'ACTIVE' as AccountStatus,
})

const filtered = computed(() => accounts.value.filter(account => {
  const text = `${account.organizationName} ${account.bankName} ${account.accountName} ${account.accountNoMasked}`.toLowerCase()
  return (!search.value || text.includes(search.value.toLowerCase()))
    && (!channel.value || account.channel === channel.value)
    && (!status.value || account.status === status.value)
}))
const cnyBalance = computed(() => accounts.value.filter(item => item.currency === 'CNY' && item.status !== 'CLOSED').reduce((sum, item) => sum + item.balance, 0))
const typeNames: Record<AccountType, string> = { BASIC: '基本账户', GENERAL: '一般账户', SPECIAL: '专用账户', SETTLEMENT: '结算账户', CASH_POOL: '资金池账户', PAYMENT_PLATFORM: '支付平台账户' }
const channelNames: Record<AccountChannel, string> = { BANK: '银行', ALIPAY: '支付宝', WECHAT: '微信支付' }

onMounted(load)

async function load() {
  loading.value = true
  try { accounts.value = await request<BankAccount[]>('/api/accounts') }
  catch (error) { toast.show('账户加载失败', error instanceof Error ? error.message : '请稍后重试', true) }
  finally { loading.value = false }
}

async function create() {
  saving.value = true
  try {
    await request<BankAccount>('/api/accounts', { method: 'POST', ...jsonBody(form) })
    toast.show('账户新增成功', '账户台账与审计日志已同步更新。')
    modalOpen.value = false
    Object.assign(form, { channel: 'BANK', organizationName: '华辰控股集团', bankName: '', bankCode: '', accountName: '', accountNo: '', currency: 'CNY', balance: 0, availableBalance: 0, lowBalanceThreshold: 100000, accountType: 'GENERAL', status: 'ACTIVE' })
    await load()
  } catch (error) { toast.show('保存失败', error instanceof Error ? error.message : '请稍后重试', true) }
  finally { saving.value = false }
}

function selectChannel(value: AccountChannel | '') {
  channel.value = value
}

function configureChannel() {
  if (form.channel === 'ALIPAY') Object.assign(form, { bankName: '支付宝', bankCode: 'ALIPAY', accountType: 'PAYMENT_PLATFORM' })
  else if (form.channel === 'WECHAT') Object.assign(form, { bankName: '微信支付', bankCode: 'WECHAT', accountType: 'PAYMENT_PLATFORM' })
  else Object.assign(form, { bankName: '', bankCode: '', accountType: 'GENERAL' })
}
</script>

<template>
  <section class="page-view">
    <div class="page-intro"><div><p class="section-kicker">FUND ACCOUNT LIFECYCLE</p><h2>资金账户</h2><p>统一维护银行、支付宝与微信支付账户的台账、余额和运行状态。</p></div><button v-if="auth.isAdmin" class="button primary" @click="modalOpen = true"><Plus :size="14" />新增账户</button></div>
    <div class="summary-strip"><div><span>账户总数</span><strong>{{ accounts.length }}</strong></div><div><span>正常运行</span><strong>{{ accounts.filter(item => item.status === 'ACTIVE').length }}</strong></div><div><span>人民币余额</span><strong>¥ {{ format.wan(cnyBalance) }}</strong></div><div><span>余额预警</span><strong>{{ accounts.filter(item => item.lowBalance).length }}</strong></div></div>
    <div class="payment-tabs channel-tabs"><button :class="{ active: channel === '' }" @click="selectChannel('')">全部 <span>{{ accounts.length }}</span></button><button :class="{ active: channel === 'BANK' }" @click="selectChannel('BANK')">银行 <span>{{ accounts.filter(item => item.channel === 'BANK').length }}</span></button><button :class="{ active: channel === 'ALIPAY' }" @click="selectChannel('ALIPAY')">支付宝 <span>{{ accounts.filter(item => item.channel === 'ALIPAY').length }}</span></button><button :class="{ active: channel === 'WECHAT' }" @click="selectChannel('WECHAT')">微信支付 <span>{{ accounts.filter(item => item.channel === 'WECHAT').length }}</span></button></div>
    <article class="panel list-panel">
      <div class="toolbar"><label class="search-box"><Search :size="15" /><input v-model="search" placeholder="搜索组织、银行或账户名称" /></label><select v-model="status"><option value="">全部状态</option><option value="ACTIVE">正常</option><option value="RESTRICTED">受限</option><option value="FROZEN">冻结</option><option value="CLOSED">已销户</option></select></div>
      <div class="table-wrap"><table class="data-table"><thead><tr><th>账户名称</th><th>开户银行</th><th>账号</th><th>可用余额</th><th>用途</th><th>状态</th><th>最近同步</th></tr></thead><tbody>
        <tr v-if="loading"><td colspan="7"><div class="empty-state">正在加载账户…</div></td></tr>
        <tr v-for="account in filtered" v-else :key="account.id"><td><div class="account-name"><span>{{ account.bankCode.slice(0, 3) }}</span><div><b>{{ account.accountName }}</b><small>{{ account.organizationName }}</small></div></div></td><td><span class="channel-badge" :class="`channel-${account.channel}`">{{ channelNames[account.channel] }}</span><small>{{ account.bankName }}</small></td><td><b>{{ account.accountNoMasked }}</b><small>{{ account.currency }}</small></td><td><span class="amount">{{ format.symbol(account.currency) }} {{ format.number(account.availableBalance) }}</span><small v-if="account.lowBalance" class="low-balance"><TriangleAlert :size="11" />低于预警线</small></td><td>{{ typeNames[account.accountType] }}</td><td><StatusBadge :status="account.status" /></td><td>{{ format.dateTime(account.lastSyncTime) }}</td></tr>
        <tr v-if="!loading && !filtered.length"><td colspan="7"><div class="empty-state">没有符合条件的资金账户</div></td></tr>
      </tbody></table></div>
    </article>

    <BaseModal :open="modalOpen" kicker="NEW FUND ACCOUNT" title="新增资金账户" @close="modalOpen = false">
      <form id="account-form" class="form-grid" @submit.prevent="create">
        <label class="field full"><span>账户渠道 *</span><select v-model="form.channel" @change="configureChannel"><option value="BANK">银行</option><option value="ALIPAY">支付宝</option><option value="WECHAT">微信支付</option></select></label>
        <label class="field"><span>所属组织 *</span><input v-model="form.organizationName" required /></label><label class="field"><span>账户名称 *</span><input v-model="form.accountName" required placeholder="例如：集团一般结算户" /></label>
        <label class="field"><span>开户机构 / 平台 *</span><input v-model="form.bankName" required :placeholder="form.channel === 'BANK' ? '银行全称' : '支付平台名称'" /></label><label class="field"><span>机构代码 *</span><input v-model="form.bankCode" required placeholder="例如：ICBC" /></label>
        <label class="field full"><span>{{ form.channel === 'BANK' ? '银行账号' : '商户号 / 账户标识' }} *</span><input v-model="form.accountNo" pattern="[A-Za-z0-9@._-]{6,64}" required :placeholder="form.channel === 'BANK' ? '8 至 32 位银行账号' : '请输入商户号或企业账户标识'" /></label>
        <label class="field"><span>币种 *</span><select v-model="form.currency"><option>CNY</option><option>USD</option><option>EUR</option></select></label><label class="field"><span>账户用途 *</span><select v-model="form.accountType"><option value="GENERAL">一般账户</option><option value="BASIC">基本账户</option><option value="SPECIAL">专用账户</option><option value="SETTLEMENT">结算账户</option><option value="CASH_POOL">资金池账户</option><option value="PAYMENT_PLATFORM">支付平台账户</option></select></label>
        <label class="field"><span>账面余额 *</span><input v-model.number="form.balance" type="number" min="0" step="0.01" required /></label><label class="field"><span>可用余额 *</span><input v-model.number="form.availableBalance" type="number" min="0" step="0.01" required /></label>
        <label class="field"><span>余额预警线 *</span><input v-model.number="form.lowBalanceThreshold" type="number" min="0" step="0.01" required /></label><label class="field"><span>账户状态 *</span><select v-model="form.status"><option value="ACTIVE">正常</option><option value="RESTRICTED">受限</option><option value="FROZEN">冻结</option></select></label>
      </form>
      <template #footer><button class="button secondary" @click="modalOpen = false">取消</button><button class="button primary" type="submit" form="account-form" :disabled="saving">{{ saving ? '保存中…' : '保存账户' }}</button></template>
    </BaseModal>
  </section>
</template>
