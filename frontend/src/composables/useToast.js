import { reactive } from 'vue'

const toasts = reactive([])
let seed = 1

export function pushToast(message, type = 'info') {
  const id = seed++
  toasts.push({ id, message, type })
  window.setTimeout(() => {
    const index = toasts.findIndex((item) => item.id === id)
    if (index >= 0) {
      toasts.splice(index, 1)
    }
  }, 3000)
}

export function useToast() {
  return { toasts, pushToast }
}