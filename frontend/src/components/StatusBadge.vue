<script setup lang="ts">
const props = defineProps<{ status: string }>()

const names: Record<string, string> = {
  DRAFT: '草稿', PENDING: '待审批', APPROVED: '待组批', QUEUED: '待批次执行',
  READY: '待执行', PROCESSING: '处理中', SUCCESS: '成功', COMPLETED: '已完成',
  PARTIAL_FAILED: '部分失败', FAILED: '失败', PAID: '已支付', REJECTED: '已驳回',
  ACTIVE: '正常', RESTRICTED: '受限', FROZEN: '冻结', CLOSED: '已销户',
  UNMATCHED: '待匹配', MATCHED: '已匹配', EXCEPTION: '异常',
  OPEN: '待处理', RESOLVED: '已解决',
}
</script>

<template>
  <span class="status" :class="`status-${props.status}`">{{ names[props.status] ?? props.status }}</span>
</template>
