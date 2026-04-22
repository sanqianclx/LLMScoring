<script setup>
import { useRouter } from 'vue-router'
import { usePlatform } from '../services/platform'

defineProps({
  title: {
    type: String,
    required: true
  }
})

const router = useRouter()
const { state, clearAuth } = usePlatform()

function signOut() {
  clearAuth()
  router.push('/login')
}
</script>

<template>
  <header class="panel topbar-shell">
    <div>
      <p class="kicker">教师工作台</p>
      <h1 class="section-title">{{ title }}</h1>
    </div>
    <div class="inline-actions">
      <div class="search-chip">后端已连接</div>
      <div class="avatar-card">
        <div class="avatar-circle">{{ (state.teacher.name || '教').slice(0, 1) }}</div>
        <div>
          <strong>{{ state.teacher.name || '教师' }}</strong>
          <div class="muted">{{ state.teacher.email || '--' }}</div>
        </div>
      </div>
      <button class="ghost-btn" @click="signOut">退出登录</button>
    </div>
  </header>
</template>

<style scoped>
.topbar-shell {
  padding: 18px 22px;
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.search-chip {
  padding: 12px 18px;
  border-radius: 999px;
  background: rgba(31, 94, 255, 0.08);
  color: var(--primary-deep);
}
.avatar-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
}
.avatar-circle {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: white;
  background: linear-gradient(135deg, var(--primary), var(--student));
  font-weight: 700;
}
@media (max-width: 767px) {
  .topbar-shell {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>