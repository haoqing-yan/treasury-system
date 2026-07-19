<script setup lang="ts">
import { CircleAlert, CircleCheck, X } from 'lucide-vue-next'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()
</script>

<template>
  <div class="toast-stack" aria-live="polite">
    <TransitionGroup name="toast">
      <div v-for="item in toast.items" :key="item.id" class="toast" :class="{ error: item.error }">
        <span class="toast-symbol"><CircleAlert v-if="item.error" :size="15" /><CircleCheck v-else :size="15" /></span>
        <div><strong>{{ item.title }}</strong><small>{{ item.message }}</small></div>
        <button aria-label="关闭提示" @click="toast.remove(item.id)"><X :size="13" /></button>
      </div>
    </TransitionGroup>
  </div>
</template>
