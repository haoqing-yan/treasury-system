<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ArrowDownLeft, ArrowUpRight, Plus } from 'lucide-vue-next'
import BaseModal from '@/components/BaseModal.vue'
import { jsonBody, request } from '@/services/http'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { useFormat } from '@/composables/useFormat'
import type { CashPlan, CashPlanType } from '@/types/api'

const auth = useAuthStore()
const toast = useToastStore()
const format = useFormat()
const now = new Date()
const end = new Date(); end.setDate(end.getDate() + 30)
const from = ref(format.dateInput(now))
const to = ref(format.dateInput(end))
const plans = ref<CashPlan[]>([])
const loading = ref(true)
const modalOpen = ref(false)
const saving = ref(false)
const form = reactive({ planDate: format.dateInput(now), type: 'INFLOW' as CashPlanType, category: '', amount: 0, organizationName: '华辰控股集团', description: '' })
const inflow = computed(() => plans.value.filter(item => item.type === 'INFLOW').reduce((sum, item) => sum + item.amount, 0))
const outflow = computed(() => plans.value.filter(item => item.type === 'OUTFLOW').reduce((sum, item) => sum + item.amount, 0))

onMounted(load)

async function load() {
  loading.value = true
  try { plans.value = await request<CashPlan[]>(`/api/cash-plans?from=${encodeURIComponent(from.value)}&to=${encodeURIComponent(to.value)}`) }
  catch (error) { toast.show('资金计划加载失败', error instanceof Error ? error.message : '请稍后重试', true) }
  finally { loading.value = false }
}

async function create() {
  saving.value = true
  try {
    await request<CashPlan>('/api/cash-plans', { method: 'POST', ...jsonBody(form) })
    toast.show('资金计划已保存', '现金流预测已同步更新。')
    modalOpen.value = false
    Object.assign(form, { planDate: format.dateInput(new Date()), type: 'INFLOW', category: '', amount: 0, organizationName: '华辰控股集团', description: '' })
    await load()
  } catch (error) { toast.show('保存失败', error instanceof Error ? error.message : '请稍后重试', true) }
  finally { saving.value = false }
}
</script>

<template>
  <section class="page-view">
    <div class="page-intro"><div><p class="section-kicker">CASH FORECAST</p><h2>资金计划</h2><p>维护未来资金流入流出，提前识别缺口与闲置。</p></div><button v-if="auth.hasPermission('cash-plan:create')" class="button primary" @click="modalOpen = true"><Plus :size="14" />新增计划</button></div>
    <div class="plan-overview"><article class="in"><span>计划流入</span><strong>¥ {{ format.wan(inflow) }}</strong><small>{{ plans.filter(item => item.type === 'INFLOW').length }} 笔计划</small></article><article class="out"><span>计划流出</span><strong>¥ {{ format.wan(outflow) }}</strong><small>{{ plans.filter(item => item.type === 'OUTFLOW').length }} 笔计划</small></article><article><span>计划净流量</span><strong>{{ inflow - outflow >= 0 ? '+' : '-' }} ¥ {{ format.wan(Math.abs(inflow - outflow)) }}</strong><small>{{ inflow - outflow >= 0 ? '预计资金净流入' : '需关注资金缺口' }}</small></article></div>
    <article class="panel list-panel">
      <div class="toolbar"><div class="date-range"><label>从 <input v-model="from" type="date" /></label><label>至 <input v-model="to" type="date" /></label><button class="button secondary small" @click="load">查询</button></div></div>
      <div class="table-wrap"><table class="data-table"><thead><tr><th>计划日期</th><th>收支方向</th><th>类别</th><th>组织</th><th>金额</th><th>说明</th><th>创建人</th></tr></thead><tbody>
        <tr v-if="loading"><td colspan="7"><div class="empty-state">正在加载资金计划…</div></td></tr>
        <tr v-for="plan in plans" v-else :key="plan.id"><td><b>{{ format.longDate(plan.planDate) }}</b></td><td><span class="direction" :class="plan.type === 'INFLOW' ? 'in' : 'out'"><ArrowDownLeft v-if="plan.type === 'INFLOW'" :size="13" /><ArrowUpRight v-else :size="13" />{{ plan.type === 'INFLOW' ? '资金流入' : '资金流出' }}</span></td><td>{{ plan.category }}</td><td>{{ plan.organizationName }}</td><td class="amount">{{ plan.type === 'INFLOW' ? '+' : '-' }} ¥ {{ format.number(plan.amount) }}</td><td>{{ plan.description || '—' }}</td><td>{{ plan.createdBy }}</td></tr>
        <tr v-if="!loading && !plans.length"><td colspan="7"><div class="empty-state">当前日期范围内没有资金计划</div></td></tr>
      </tbody></table></div>
    </article>

    <BaseModal :open="modalOpen" kicker="NEW CASH PLAN" title="新增资金计划" width="small" @close="modalOpen = false">
      <form id="plan-form" class="form-grid" @submit.prevent="create">
        <label class="field"><span>计划日期 *</span><input v-model="form.planDate" type="date" required /></label><label class="field"><span>收支方向 *</span><select v-model="form.type"><option value="INFLOW">资金流入</option><option value="OUTFLOW">资金流出</option></select></label>
        <label class="field"><span>计划类别 *</span><input v-model="form.category" required placeholder="例如：销售回款" /></label><label class="field"><span>计划金额 *</span><input v-model.number="form.amount" type="number" min="0.01" step="0.01" required /></label>
        <label class="field full"><span>所属组织 *</span><input v-model="form.organizationName" required /></label><label class="field full"><span>计划说明</span><textarea v-model="form.description" maxlength="240" placeholder="补充合同、客户或付款批次信息" /></label>
      </form>
      <template #footer><button class="button secondary" @click="modalOpen = false">取消</button><button class="button primary" type="submit" form="plan-form" :disabled="saving">{{ saving ? '保存中…' : '保存计划' }}</button></template>
    </BaseModal>
  </section>
</template>
