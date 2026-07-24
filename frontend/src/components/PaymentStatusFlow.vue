<script setup lang="ts">
import { computed } from 'vue'
import {
  CheckCircle2,
  CircleDollarSign,
  FilePenLine,
  Layers3,
  LoaderCircle,
  RotateCcw,
  Send,
  XCircle,
} from 'lucide-vue-next'
import type { Payment, PaymentStatus } from '@/types/api'

const props = defineProps<{
  payments: Payment[]
  modelValue: PaymentStatus | ''
}>()

const emit = defineEmits<{
  'update:modelValue': [value: PaymentStatus | '']
}>()

const mainSteps: Array<{ status: PaymentStatus; label: string; hint: string; icon: typeof FilePenLine }> = [
  { status: 'DRAFT', label: '草稿', hint: '录入付款信息', icon: FilePenLine },
  { status: 'PENDING', label: '待审批', hint: '等待复核', icon: RotateCcw },
  { status: 'APPROVED', label: '待组批', hint: '审批已通过', icon: CheckCircle2 },
  { status: 'QUEUED', label: '待执行', hint: '已进入批次', icon: Layers3 },
  { status: 'PROCESSING', label: '执行中', hint: '渠道处理中', icon: LoaderCircle },
  { status: 'PAID', label: '已支付', hint: '付款已完成', icon: CircleDollarSign },
]

const exceptionSteps: Array<{ status: PaymentStatus; label: string; hint: string; icon: typeof XCircle }> = [
  { status: 'REJECTED', label: '已驳回', hint: '审批未通过', icon: XCircle },
  { status: 'FAILED', label: '执行失败', hint: '等待人工处理', icon: Send },
]

const total = computed(() => props.payments.length)

function count(status: PaymentStatus) {
  return props.payments.filter(payment => payment.status === status).length
}

function select(value: PaymentStatus | '') {
  emit('update:modelValue', props.modelValue === value ? '' : value)
}
</script>

<template>
  <section class="payment-flow" aria-label="付款状态流程筛选">
    <header class="payment-flow-header">
      <div><strong>付款处理流程</strong><span>点击状态节点筛选付款单</span></div>
      <button type="button" :class="{ active: modelValue === '' }" :aria-pressed="modelValue === ''" @click="select('')">
        全部付款 <em>{{ total }}</em>
      </button>
    </header>

    <div class="payment-flow-main">
      <template v-for="(step, index) in mainSteps" :key="step.status">
        <button
          type="button"
          class="payment-flow-node"
          :class="{ active: modelValue === step.status, complete: count(step.status) > 0 }"
          :aria-label="`${step.label}，${count(step.status)} 笔`"
          :aria-pressed="modelValue === step.status"
          @click="select(step.status)"
        >
          <span><component :is="step.icon" :size="16" /><em>{{ count(step.status) }}</em></span>
          <strong>{{ step.label }}</strong>
          <small>{{ step.hint }}</small>
        </button>
        <div v-if="index < mainSteps.length - 1" class="payment-flow-arrow" aria-hidden="true"><i /></div>
      </template>
    </div>

    <div class="payment-flow-exceptions">
      <span>异常分支</span>
      <button
        v-for="step in exceptionSteps"
        :key="step.status"
        type="button"
        :class="{ active: modelValue === step.status }"
        :aria-pressed="modelValue === step.status"
        @click="select(step.status)"
      >
        <component :is="step.icon" :size="14" />
        <span><strong>{{ step.label }}</strong><small>{{ step.hint }}</small></span>
        <em>{{ count(step.status) }}</em>
      </button>
    </div>
  </section>
</template>
