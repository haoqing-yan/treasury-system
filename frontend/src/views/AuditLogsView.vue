<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RefreshCw, ShieldCheck } from 'lucide-vue-next'
import { request } from '@/services/http'
import { useToastStore } from '@/stores/toast'
import { useFormat } from '@/composables/useFormat'
import type { AuditLog } from '@/types/api'

const toast = useToastStore()
const format = useFormat()
const logs = ref<AuditLog[]>([])
const loading = ref(true)
const actionNames: Record<string, string> = { CREATE: '新建', UPDATE: '更新', SYNC: '同步', BANK_SYNC: '流水同步', SUBMIT: '提交审批', APPROVE: '审批通过', REJECT: '驳回', BANK_EXECUTE: '银行执行', AUTO_MATCH: '自动对账', MANUAL_MATCH: '手工匹配', MARK_EXCEPTION: '异常登记', CLAIM_EXCEPTION: '认领异常', RESOLVE_EXCEPTION: '解决异常', REOPEN_EXCEPTION: '重开异常' }

onMounted(load)

async function load() {
  loading.value = true
  try { logs.value = await request<AuditLog[]>('/api/audits') }
  catch (error) { toast.show('日志加载失败', error instanceof Error ? error.message : '请稍后重试', true) }
  finally { loading.value = false }
}
</script>

<template>
  <section class="page-view">
    <div class="page-intro"><div><p class="section-kicker">AUDIT TRAIL</p><h2>审计日志</h2><p>关键资金操作全量留痕，支持责任追溯。</p></div><button class="button secondary" @click="load"><RefreshCw :size="14" />刷新日志</button></div>
    <article class="panel list-panel">
      <div class="audit-banner"><span><ShieldCheck :size="17" /></span><div><strong>审计链路正常</strong><p>系统记录操作人、业务对象、动作、时间与来源地址。</p></div></div>
      <div class="table-wrap"><table class="data-table"><thead><tr><th>时间</th><th>操作人</th><th>动作</th><th>业务对象</th><th>操作详情</th><th>来源</th></tr></thead><tbody>
        <tr v-if="loading"><td colspan="6"><div class="empty-state">正在加载审计日志…</div></td></tr>
        <tr v-for="log in logs" v-else :key="log.id"><td>{{ format.dateTime(log.operatedAt) }}</td><td><b>{{ log.username }}</b></td><td><span class="action-tag">{{ actionNames[log.action] ?? log.action }}</span></td><td>{{ log.resourceType }}<template v-if="log.resourceId"> #{{ log.resourceId }}</template></td><td>{{ log.detail }}</td><td>{{ log.ipAddress }}</td></tr>
        <tr v-if="!loading && !logs.length"><td colspan="6"><div class="empty-state">暂无审计日志</div></td></tr>
      </tbody></table></div>
    </article>
  </section>
</template>
