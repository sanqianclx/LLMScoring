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
        <h1 class="hero-title">LLM 智能评分系统</h1>
        <p class="hero-copy">
          覆盖课程管理、试卷创建、学生提交、自动评分、教师复核与成绩查询等功能。
        </p>

        <div class="formal-note list-card">
          <strong>本地演示</strong>
          <p class="muted">教师账号：<code>teacher</code> / <code>teacher123</code></p>
          <p class="muted">演示分享码：<code>{{ state.bootstrap?.demoShareCode || 'BIO-2026' }}</code></p>
        </div>

        <div class="hero-checklist">
          <div class="list-card">
            <strong>教师端流程</strong>
            <p class="muted">创建课程与试卷、评阅学生提交，并发布最终成绩。</p>
          </div>
          <div class="list-card">
            <strong>学生端流程</strong>
            <p class="muted">输入分享码进入答题；随后可用分享码与学号查询成绩。</p>
          </div>
        </div>
      </section>

      <section class="auth-content panel">
        <header class="row-between auth-header">
          <div>
            <p class="kicker">{{ route.meta.title }}</p>
            <h2 class="section-title">教师入口</h2>
          </div>
          <RouterLink class="ghost-btn" to="/student">学生入口</RouterLink>
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