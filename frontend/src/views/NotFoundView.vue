<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <section class="form-card not-found">
    <p class="kicker">404</p>
    <h2 class="section-title">页面不存在</h2>
    <p class="muted">你访问的页面不存在。你可以返回教师登录页，或进入学生入口。</p>
    <div class="inline-actions">
      <RouterLink class="primary-btn" to="/login">教师登录</RouterLink>
      <RouterLink class="ghost-btn" to="/student">学生入口</RouterLink>
    </div>
  </section>
</template>