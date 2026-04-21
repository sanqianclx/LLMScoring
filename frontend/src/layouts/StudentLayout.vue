<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <div class="app-page student-wrap">
    <header class="student-shell panel">
      <div>
        <p class="kicker">学生端</p>
        <h1 class="section-title">答题与成绩查询</h1>
        <p class="muted">输入分享码打开对应试卷；提交后可使用相同分享码与学号查询最终成绩。</p>
      </div>
      <div class="inline-actions">
        <RouterLink class="ghost-btn" to="/login">教师登录</RouterLink>
        <RouterLink class="secondary-btn" to="/student">学生入口</RouterLink>
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