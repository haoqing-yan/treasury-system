<script setup lang="ts">
import { X } from 'lucide-vue-next'

withDefaults(defineProps<{ open: boolean; kicker?: string; title: string; width?: 'normal' | 'small' }>(), {
  kicker: '', width: 'normal',
})
const emit = defineEmits<{ close: [] }>()
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="open" class="modal-layer" @mousedown.self="emit('close')">
        <section class="modal-card" :class="{ small: width === 'small' }" role="dialog" aria-modal="true" :aria-label="title">
          <header class="modal-header">
            <div><span class="panel-kicker">{{ kicker }}</span><h2>{{ title }}</h2></div>
            <button class="modal-close" aria-label="关闭" @click="emit('close')"><X :size="18" /></button>
          </header>
          <div class="modal-body"><slot /></div>
          <footer v-if="$slots.footer" class="modal-footer"><slot name="footer" /></footer>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>
