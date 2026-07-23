<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { CheckCircle2, ClipboardCheck, RefreshCw, RotateCcw, Search, ShieldAlert, UserCheck } from 'lucide-vue-next'
import BaseModal from '@/components/BaseModal.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { jsonBody, request } from '@/services/http'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { useFormat } from '@/composables/useFormat'
import type { ExceptionCase, ExceptionCaseType, ExceptionCategory, ExceptionStatus, ExceptionSummary } from '@/types/api'

const auth = useAuthStore()
const toast = useToastStore()
const format = useFormat()
const cases = ref<ExceptionCase[]>([])
const summary = ref<ExceptionSummary>({ totalCount: 0, businessCount: 0, systemCount: 0, openCount: 0, processingCount: 0, resolvedCount: 0, highPriorityCount: 0 })
const loading = ref(true)
const processing = ref(false)
const search = ref('')
const category = ref<ExceptionCategory | ''>('')
const status = ref<ExceptionStatus | ''>('')
const type = ref<ExceptionCaseType | ''>('')
const modalOpen = ref(false)
const modalMode = ref<'resolve' | 'reopen'>('resolve')
const selectedCase = ref<ExceptionCase | null>(null)
const note = ref('')

const typeNames: Record<ExceptionCaseType, string> = {
  PAYMENT_RISK: '付款风险', RECONCILIATION: '银行对账', ACCOUNT_BALANCE: '账户预警',
  SYSTEM_INTEGRATION: '外部接口', SYSTEM_JOB: '定时任务', SYSTEM_RUNTIME: '运行故障',
}
const categoryNames: Record<ExceptionCategory, string> = { BUSINESS: '业务异常', SYSTEM: '系统异常' }
const typeOptions: Array<{ value: ExceptionCaseType; label: string; category: ExceptionCategory }> = [
  { value: 'PAYMENT_RISK', label: '付款风险', category: 'BUSINESS' },
  { value: 'RECONCILIATION', label: '银行对账', category: 'BUSINESS' },
  { value: 'ACCOUNT_BALANCE', label: '账户预警', category: 'BUSINESS' },
  { value: 'SYSTEM_INTEGRATION', label: '外部接口', category: 'SYSTEM' },
  { value: 'SYSTEM_JOB', label: '定时任务', category: 'SYSTEM' },
  { value: 'SYSTEM_RUNTIME', label: '运行故障', category: 'SYSTEM' },
]
const availableTypes = computed(() => typeOptions.filter(item => !category.value || item.category === category.value))
const severityNames = { LOW: '低', MEDIUM: '中', HIGH: '高', CRITICAL: '紧急' }
const filtered = computed(() => cases.value.filter(item => {
  const text = `${item.caseNo} ${item.title} ${item.description} ${item.sourceReference}`.toLowerCase()
  return (!search.value || text.includes(search.value.toLowerCase()))
    && (!category.value || item.category === category.value)
    && (!status.value || item.status === status.value)
    && (!type.value || item.type === type.value)
}))

function selectCategory(value: ExceptionCategory | '') {
  category.value = value
  if (type.value && !availableTypes.value.some(item => item.value === type.value)) type.value = ''
}

onMounted(load)

async function load() {
  loading.value = true
  try {
    ;[cases.value, summary.value] = await Promise.all([
      request<ExceptionCase[]>('/api/exceptions'),
      request<ExceptionSummary>('/api/exceptions/summary'),
    ])
  } catch (error) { toast.show('异常工单加载失败', error instanceof Error ? error.message : '请稍后重试', true) }
  finally { loading.value = false }
}

async function claim(item: ExceptionCase) {
  processing.value = true
  try {
    await request<ExceptionCase>(`/api/exceptions/${item.id}/claim`, { method: 'POST' })
    toast.show('异常已认领', `工单 ${item.caseNo} 已进入处理中。`)
    await load()
  } catch (error) { toast.show('认领失败', error instanceof Error ? error.message : '请稍后重试', true) }
  finally { processing.value = false }
}

function openModal(item: ExceptionCase, mode: 'resolve' | 'reopen') {
  selectedCase.value = item
  modalMode.value = mode
  note.value = ''
  modalOpen.value = true
}

function canResolve(item: ExceptionCase) {
  return auth.isAdmin || item.assignee === auth.user?.username
}

async function submitModal() {
  if (!selectedCase.value || note.value.trim().length < 5) {
    toast.show('请补充处理说明', '处理说明至少填写 5 个字符。', true)
    return
  }
  processing.value = true
  try {
    const path = `/api/exceptions/${selectedCase.value.id}/${modalMode.value}`
    const body = modalMode.value === 'resolve' ? { note: note.value.trim() } : { reason: note.value.trim() }
    await request<ExceptionCase>(path, { method: 'POST', ...jsonBody(body) })
    toast.show(modalMode.value === 'resolve' ? '异常已解决' : '异常已重新打开', `工单 ${selectedCase.value.caseNo} 状态已更新。`)
    modalOpen.value = false
    await load()
  } catch (error) { toast.show('处理失败', error instanceof Error ? error.message : '请稍后重试', true) }
  finally { processing.value = false }
}
</script>

<template>
  <section class="page-view">
    <div class="page-intro">
      <div><p class="section-kicker">EXCEPTION CONTROL</p><h2>异常中心</h2><p>分类管理业务异常与系统异常，统一完成认领、处理、关闭和审计留痕。</p></div>
      <button class="button secondary" @click="load"><RefreshCw :size="14" />刷新工单</button>
    </div>

    <div class="exception-summary">
      <article><span>异常工单</span><strong>{{ summary.totalCount }}</strong><small>全部已发现事项</small></article>
      <article class="business"><span>业务异常</span><strong>{{ summary.businessCount }}</strong><small>付款、对账与账户事项</small></article>
      <article class="system"><span>系统异常</span><strong>{{ summary.systemCount }}</strong><small>接口、任务与运行故障</small></article>
      <article class="high"><span>高优先级</span><strong>{{ summary.highPriorityCount }}</strong><small>需优先关注</small></article>
    </div>

    <div class="exception-category-tabs" role="tablist" aria-label="异常分类">
      <button :class="{ active: category === '' }" @click="selectCategory('')">全部异常 <span>{{ summary.totalCount }}</span></button>
      <button :class="{ active: category === 'BUSINESS' }" @click="selectCategory('BUSINESS')">业务异常 <span>{{ summary.businessCount }}</span></button>
      <button :class="{ active: category === 'SYSTEM' }" @click="selectCategory('SYSTEM')">系统异常 <span>{{ summary.systemCount }}</span></button>
    </div>

    <article class="panel list-panel">
      <div class="toolbar">
        <label class="search-box wide"><Search :size="15" /><input v-model="search" placeholder="搜索工单号、异常标题或来源单号" /></label>
        <div class="exception-filters"><select v-model="type"><option value="">全部类型</option><option v-for="option in availableTypes" :key="option.value" :value="option.value">{{ option.label }}</option></select><select v-model="status"><option value="">全部状态</option><option value="OPEN">待处理</option><option value="PROCESSING">处理中</option><option value="RESOLVED">已解决</option></select></div>
      </div>
      <div class="table-wrap"><table class="data-table exception-table"><thead><tr><th>工单 / 发现时间</th><th>异常来源</th><th>异常内容</th><th>优先级</th><th>处理状态</th><th>处理人 / 结论</th><th class="align-right">操作</th></tr></thead><tbody>
        <tr v-if="loading"><td colspan="7"><div class="empty-state">正在加载异常工单…</div></td></tr>
        <tr v-for="item in filtered" v-else :key="item.id">
          <td><b>{{ item.caseNo }}</b><small>{{ format.dateTime(item.detectedAt) }}</small></td>
          <td><span class="case-category" :class="`case-category-${item.category}`">{{ categoryNames[item.category] }}</span><span class="case-source">{{ typeNames[item.type] }}</span><small>{{ item.sourceReference }}</small></td>
          <td class="case-detail"><b>{{ item.title }}</b><small>{{ item.description }}</small></td>
          <td><span class="severity" :class="`severity-${item.severity}`">{{ severityNames[item.severity] }}</span></td>
          <td><StatusBadge :status="item.status" /></td>
          <td><b>{{ item.assignee ?? '尚未认领' }}</b><small v-if="item.resolution" class="resolution-note">{{ item.resolution }}</small><small v-else>{{ item.status === 'PROCESSING' ? '等待提交处理结论' : '—' }}</small></td>
          <td><div v-if="auth.hasPermission('exception:handle')" class="row-actions"><button v-if="item.status === 'OPEN'" class="row-button primary" :disabled="processing" @click="claim(item)"><UserCheck :size="11" />认领</button><button v-else-if="item.status === 'PROCESSING' && canResolve(item)" class="row-button primary" @click="openModal(item, 'resolve')"><CheckCircle2 :size="11" />解决</button><button v-else-if="item.status === 'RESOLVED'" class="row-button" @click="openModal(item, 'reopen')"><RotateCcw :size="11" />重新打开</button><span v-else>—</span></div><span v-else>—</span></td>
        </tr>
        <tr v-if="!loading && !filtered.length"><td colspan="7"><div class="empty-state">没有符合条件的异常工单</div></td></tr>
      </tbody></table></div>
    </article>

    <BaseModal :open="modalOpen" :kicker="modalMode === 'resolve' ? 'RESOLUTION' : 'REOPEN CASE'" :title="modalMode === 'resolve' ? '提交异常处理结论' : '重新打开异常工单'" width="small" @close="modalOpen = false">
      <div v-if="selectedCase" class="exception-preview"><span><ShieldAlert :size="15" /></span><div><small>{{ selectedCase.caseNo }} · {{ categoryNames[selectedCase.category] }} · {{ typeNames[selectedCase.type] }}</small><strong>{{ selectedCase.title }}</strong><p>{{ selectedCase.description }}</p></div></div>
      <label class="field"><span>{{ modalMode === 'resolve' ? '处理结论与依据 *' : '重新打开原因 *' }}</span><textarea v-model="note" maxlength="500" :placeholder="modalMode === 'resolve' ? '填写核查过程、处理结果及相关依据' : '说明为什么需要继续跟进该异常'" /></label>
      <div class="control-tip"><ClipboardCheck :size="15" /><p>处理动作、操作人和结论将同步写入审计日志，便于后续责任追溯。</p></div>
      <template #footer><button class="button secondary" @click="modalOpen = false">取消</button><button class="button primary" :disabled="processing" @click="submitModal">{{ processing ? '提交中…' : '确认提交' }}</button></template>
    </BaseModal>
  </section>
</template>
