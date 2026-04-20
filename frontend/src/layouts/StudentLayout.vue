<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <div class="app-page student-wrap">
    <header class="student-shell panel">
      <div>
        <p class="kicker">学生端</p>
        <h1 class="section-title">在线答题与结果查询</h1>
        <p class="muted">通过分享码进入答题页面，无需登录，教师审核完成后可查看最终成绩与评语。</p>
      </div>
      <div class="inline-actions">
        <RouterLink class="ghost-btn" to="/login">教师登录</RouterLink>
        <RouterLink class="secondary-btn" to="/student/exam/BIO-2026">进入示例试卷</RouterLink>
      </div>
    </header>
    <slot />
  </div>
</template>

<style scoped>
.student-wrap {
  display: grid;
  gap: 22px;
}
.student-shell {
  padding: 24px 26px;
  border-radius: 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
@media (max-width: 767px) {
  .student-shell {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>