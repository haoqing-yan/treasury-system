<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Clock3, Layers3, Play, Plus, RefreshCw } from 'lucide-vue-next'
import BaseModal from '@/components/BaseModal.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { jsonBody, request } from '@/services/http'
import { useToastStore } from '@/stores/toast'
import { useFormat } from '@/composables/useFormat'
import type { Payment, PaymentBatch } from '@/types/api'

const toast = useToastStore()
const format = useFormat()
const batches = ref<PaymentBatch[]>([])
const approvedPayments = ref<Payment[]>([])
const selectedIds = ref<number[]>([])
const modalOpen = ref(false)
const loading = ref(false)
const saving = ref(false)
const expandedId = ref<number | null>(null)

function defaultSchedule() {
  const date = new Date(Date.now() + 5 * 60_000)
  date.setSeconds(0, 0)
  return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 16)
}

const scheduledAt = ref(defaultSchedule())
const selectedTotal = computed(() => approvedPayments.value
  .filter(payment => selectedIds.value.includes(payment.id))
  .reduce((sum, payment) => sum + payment.amount, 0))

async function load() {
  loading.value = true
  try {
    const [batchData, paymentData] = await Promise.all([
      request<PaymentBatch[]>('/api/payment-batches'),
      request<Payment[]>('/api/payments?status=APPROVED'),
    ])
    batches.value = batchData
    approvedPayments.value = paymentData
    selectedIds.value = selectedIds.value.filter(id => paymentData.some(payment => payment.id === id))
  } catch (error) {
    toast.show('批量付款加载失败', error instanceof Error ? error.message : '请稍后重试', true)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  selectedIds.value = []
  scheduledAt.value = defaultSchedule()
  modalOpen.value = true
}

async function createBatch() {
  if (!selectedIds.value.length) {
    toast.show('请选择付款单', '至少选择一笔已审批付款', true)
    return
  }
  saving.value = true
  try {
    await request<PaymentBatch>('/api/payment-batches', {
      method: 'POST',
      ...jsonBody({ paymentIds: selectedIds.value, scheduledAt: new Date(scheduledAt.value).toISOString() }),
    })
    modalOpen.value = false
    toast.show('批次创建成功', '系统将在计划时间自动执行，也可以立即执行。')
    await load()
  } catch (error) {
    toast.show('批次创建失败', error instanceof Error ? error.message : '请稍后重试', true)
  } finally {
    saving.value = false
  }
}

async function executeBatch(batch: PaymentBatch) {
  saving.value = true
  try {
    const result = await request<PaymentBatch>(`/api/payment-batches/${batch.id}/execute`, { method: 'POST' })
    toast.show('批次执行完成', `成功 ${result.successCount} 笔，失败 ${result.failedCount} 笔`)
    await load()
    expandedId.value = batch.id
  } catch (error) {
    toast.show('批次执行失败', error instanceof Error ? error.message : '请稍后重试', true)
  } finally {
    saving.value = false
  }
}

function toggleAll() {
  selectedIds.value = selectedIds.value.length === approvedPayments.value.length
    ? []
    : approvedPayments.value.map(payment => payment.id)
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-intro">
      <div>
        <p class="section-kicker">PAYMENT BATCH</p>
        <h2>批量付款</h2>
        <p>将已审批付款统一组批，到达计划时间后通过受控线程池并发执行。</p>
      </div>
      <button class="button primary" @click="openCreate"><Plus :size="14" />新建付款批次</button>
    </div>

    <div class="toolbar">
      <span class="toolbar-note"><Clock3 :size="14" />系统每 30 秒扫描到期批次</span>
      <button class="button secondary" :disabled="loading" @click="load"><RefreshCw :size="13" />刷新</button>
    </div>

    <div class="table-wrap">
      <table class="data-table">
        <thead><tr><th>批次号</th><th>计划执行时间</th><th>笔数</th><th>总金额</th><th>执行结果</th><th>状态</th><th class="align-right">操作</th></tr></thead>
        <tbody>
          <tr v-if="loading"><td colspan="7"><div class="empty-state">正在加载付款批次…</div></td></tr>
          <template v-for="batch in batches" :key="batch.id">
            <tr>
              <td><strong>{{ batch.batchNo }}</strong><small class="cell-subtitle">创建人：{{ batch.createdBy }}</small></td>
              <td>{{ format.dateTime(batch.scheduledAt) }}</td>
              <td>{{ batch.totalCount }} 笔</td>
              <td><strong>{{ format.symbol(batch.items[0]?.currency ?? 'CNY') }} {{ format.number(batch.totalAmount) }}</strong></td>
              <td><span class="text-success">成功 {{ batch.successCount }}</span> / <span>失败 {{ batch.failedCount }}</span></td>
              <td><StatusBadge :status="batch.status" /></td>
              <td class="align-right">
                <button class="text-button" @click="expandedId = expandedId === batch.id ? null : batch.id">{{ expandedId === batch.id ? '收起' : '明细' }}</button>
                <button v-if="batch.status === 'READY'" class="text-button" :disabled="saving" @click="executeBatch(batch)"><Play :size="12" />立即执行</button>
              </td>
            </tr>
            <tr v-if="expandedId === batch.id" class="detail-row">
              <td colspan="7">
                <div class="table-wrap">
                  <table class="data-table compact">
                    <thead><tr><th>付款单</th><th>付款账户</th><th>收款方</th><th>金额</th><th>请求号</th><th>结果</th></tr></thead>
                    <tbody><tr v-for="item in batch.items" :key="item.id">
                      <td>{{ item.paymentNo }}</td><td>{{ item.payerAccountName }}</td><td>{{ item.payeeName }}</td>
                      <td>{{ format.symbol(item.currency) }} {{ format.number(item.amount) }}</td>
                      <td>{{ item.requestId }}</td>
                      <td><StatusBadge :status="item.status" /><small v-if="item.failureReason" class="cell-subtitle">{{ item.failureReason }}</small></td>
                    </tr></tbody>
                  </table>
                </div>
              </td>
            </tr>
          </template>
          <tr v-if="!loading && !batches.length"><td colspan="7"><div class="empty-state"><Layers3 :size="28" /><p>暂无付款批次</p></div></td></tr>
        </tbody>
      </table>
    </div>

    <BaseModal :open="modalOpen" kicker="NEW BATCH" title="新建付款批次" @close="modalOpen = false">
      <div class="form-section-title"><span>01</span><div><strong>统一执行时间</strong><small>到达该时间后系统自动启动批次</small></div></div>
      <label class="field full"><span>计划执行时间 *</span><input v-model="scheduledAt" type="datetime-local" required /></label>

      <div class="form-section-title"><span>02</span><div><strong>选择已审批付款</strong><small>本批次将并发执行不同付款账户，同一账户自动串行</small></div></div>
      <div class="toolbar">
        <button class="text-button" type="button" @click="toggleAll">{{ selectedIds.length === approvedPayments.length ? '取消全选' : '全选' }}</button>
        <span class="toolbar-note">已选 {{ selectedIds.length }} 笔，合计 ¥ {{ format.number(selectedTotal) }}</span>
      </div>
      <div class="table-wrap">
        <table class="data-table compact">
          <thead><tr><th></th><th>付款单</th><th>付款账户</th><th>收款方</th><th>金额</th></tr></thead>
          <tbody>
            <tr v-for="payment in approvedPayments" :key="payment.id">
              <td><input v-model="selectedIds" type="checkbox" :value="payment.id" /></td>
              <td>{{ payment.paymentNo }}</td><td>{{ payment.payerAccountName }}</td><td>{{ payment.payeeName }}</td>
              <td>{{ format.symbol(payment.currency) }} {{ format.number(payment.amount) }}</td>
            </tr>
            <tr v-if="!approvedPayments.length"><td colspan="5"><div class="empty-state">暂无可组批的已审批付款</div></td></tr>
          </tbody>
        </table>
      </div>
      <template #footer>
        <button class="button secondary" @click="modalOpen = false">取消</button>
        <button class="button primary" :disabled="saving || !selectedIds.length" @click="createBatch">{{ saving ? '创建中…' : '创建批次' }}</button>
      </template>
    </BaseModal>
  </section>
</template>
