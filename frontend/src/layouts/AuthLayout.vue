<script setup>
import { RouterLink, useRoute } from 'vue-router'
import { usePlatform } from '../services/platform'

const route = useRoute()
const { state } = usePlatform()
</script>

<template>
  <div class="app-page">
    <div class="auth-shell formal-auth-shell">
      <section class="auth-brand panel formal-brand">
        <p class="kicker">教学评分平台</p>
        <h1 class="hero-title">LLM 自动评分系统</h1>
        <p class="hero-copy">
          以课程管理、试卷创建、自动评分、人工审核和结果反馈为核心，提供适合正式教学场景的完整业务界面。
        </p>

        <div class="formal-note list-card">
          <strong>本地演示入口</strong>
          <p class="muted">教师账号：<code>teacher</code> / <code>teacher123</code></p>
          <p class="muted">学生分享码：<code>{{ state.bootstrap?.demoShareCode || 'BIO-2026' }}</code></p>
        </div>

        <div class="hero-checklist">
          <div class="list-card">
            <strong>教师端流程</strong>
            <p class="muted">工作台、课程、试卷、分享、审核与个人中心均为独立页面，便于日常教学管理。</p>
          </div>
          <div class="list-card">
            <strong>学生端流程</strong>
            <p class="muted">学生通过分享码独立进入答题页，提交后在结果页查看最终成绩与评语。</p>
          </div>
        </div>
      </section>

      <section class="auth-content panel">
        <header class="row-between auth-header">
          <div>
            <p class="kicker">{{ route.meta.title }}</p>
            <h2 class="section-title">教师登录与账号入口</h2>
          </div>
          <RouterLink class="ghost-btn" to="/student/exam/BIO-2026">学生入口</RouterLink>
        </header>
        <slot />
      </section>
    </div>
  </div>
</template>

<style scoped>
.formal-auth-shell {
  display: grid;
  grid-template-columns: 1fr 0.9fr;
  gap: 22px;
}
.formal-brand,
.auth-content {
  padding: 28px;
  border-radius: 30px;
}
.formal-note {
  margin-top: 22px;
}
.hero-checklist {
  display: grid;
  gap: 12px;
  margin-top: 22px;
}
.auth-header {
  margin-bottom: 18px;
}
@media (max-width: 960px) {
  .formal-auth-shell {
    grid-template-columns: 1fr;
  }
}
</style>