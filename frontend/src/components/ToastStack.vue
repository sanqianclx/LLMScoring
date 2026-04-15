<script setup>
import { useToast } from '../composables/useToast'

const { toasts } = useToast()
</script>

<template>
  <div class="toast-stack">
    <TransitionGroup name="fade">
      <div v-for="toast in toasts" :key="toast.id" class="toast-item">
        <span class="toast-dot" :class="toast.type"></span>
        <div>
          <strong>{{ toast.type === 'error' ? '操作提醒' : '系统提示' }}</strong>
          <div class="muted">{{ toast.message }}</div>
        </div>
      </div>
    </TransitionGroup>
  </div>
</template>