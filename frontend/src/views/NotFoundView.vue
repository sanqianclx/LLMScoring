<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <section class="form-card not-found">
    <p class="kicker">404</p>
    <h2 class="section-title">页面不存在</h2>
    <p class="muted">当前访问的路由在本项目中不存在。</p>
    <div class="inline-actions">
      <RouterLink class="primary-btn" to="/login">返回登录页</RouterLink>
      <RouterLink class="ghost-btn" to="/student/exam/BIO-2026">进入示例试卷</RouterLink>
    </div>
  </section>
</template>

<style scoped>
.not-found {
  padding: 30px;
}
</style>