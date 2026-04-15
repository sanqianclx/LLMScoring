<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { usePlatform } from '../services/platform'

const route = useRoute()
const { state } = usePlatform()

const navItems = [
  { label: '首页', to: '/teacher/dashboard', section: 'dashboard', icon: '首' },
  { label: '课程管理', to: '/teacher/courses', section: 'courses', icon: '课' },
  { label: '试卷管理', to: '/teacher/papers/new', section: 'papers', icon: '卷' },
  { label: '评分审核', to: '/teacher/review', section: 'review', icon: '审' },
  { label: '个人中心', to: '/teacher/profile', section: 'profile', icon: '我' }
]

const activeSection = computed(() => route.meta.section)
</script>

<template>
  <aside class="side-card sidebar-shell">
    <div>
      <p class="kicker">LLM 自动评分</p>
      <h2 class="section-title brand-title">教师控制台</h2>
      <p class="muted">{{ state.teacher.school || '本地教学工作区' }}</p>
    </div>

    <nav class="nav-list">
      <RouterLink
        v-for="item in navItems"
        :key="item.to"
        :to="item.to"
        class="nav-item"
        :class="{ active: activeSection === item.section }"
      >
        <span class="nav-icon">{{ item.icon }}</span>
        <span>{{ item.label }}</span>
      </RouterLink>
    </nav>

    <div class="sidebar-summary list-card">
      <strong>{{ state.teacher.name || '教师' }}</strong>
      <p class="muted">{{ state.teacher.role }}</p>
      <div class="toolbar">
        <span class="badge">{{ state.teacher.taughtCourses[0] || '任教课程' }}</span>
        <span class="badge">{{ state.teacher.lastLogin || '--' }}</span>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.sidebar-shell {
  padding: 24px 20px;
  display: grid;
  gap: 22px;
  align-content: start;
}
.brand-title {
  font-size: 1.6rem;
}
.nav-list {
  display: grid;
  gap: 10px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 18px;
  color: var(--text-soft);
  transition: background 0.2s ease, transform 0.2s ease, color 0.2s ease;
}
.nav-item:hover {
  transform: translateX(2px);
  background: rgba(31, 94, 255, 0.06);
}
.nav-item.active {
  background: linear-gradient(135deg, rgba(31, 94, 255, 0.14), rgba(23, 63, 158, 0.16));
  color: var(--primary-deep);
  font-weight: 600;
}
.nav-icon {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: rgba(31, 94, 255, 0.08);
}
</style>