import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface ToastItem {
  id: number
  title: string
  message: string
  error: boolean
}

export const useToastStore = defineStore('toast', () => {
  const items = ref<ToastItem[]>([])
  let sequence = 0

  function show(title: string, message: string, error = false) {
    const item = { id: ++sequence, title, message, error }
    items.value.push(item)
    window.setTimeout(() => remove(item.id), 4200)
  }

  function remove(id: number) {
    items.value = items.value.filter(item => item.id !== id)
  }

  return { items, show, remove }
})
