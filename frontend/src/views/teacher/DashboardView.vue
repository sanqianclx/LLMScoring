<script setup>
import { computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import MetricCard from '../../components/MetricCard.vue'
import SectionCard from '../../components/SectionCard.vue'
import { usePlatform } from '../../services/platform'

const { state, dashboardCards, pendingSubmissions, findCourse, findPaper, refreshDashboard } = usePlatform()

const latestPapers = computed(() => state.papers.slice(0, 4))
const quickActions = [
  { label: '创建课程', to: '/teacher/courses', style: 'primary-btn' },
  { label: '试卷管理', to: '/teacher/papers', style: 'secondary-btn' },
  { label: '评阅队列', to: '/teacher/review', style: 'ghost-btn' }
]

onMounted(() => {
  if (state.teacher.id) {
    refreshDashboard(state.teacher.id).catch(() => {})
  }
})
</script>

<template>
  <section class="section-grid">
    <section class="panel" style="padding: 28px; border-radius: 28px;">
      <p class="kicker">仪表盘</p>
      <div class="row-between">
        <div>
          <h2 class="hero-title" style="font-size: 2.4rem;">{{ state.teacher.name || '教师' }} 工作台</h2>
          <p class="hero-copy">在一个页面里查看课程、历史试卷、学生提交、AI 评分进度，以及待教师复核的任务。</p>
        </div>
        <div class="toolbar">
          <RouterLink v-for="action in quickActions" :key="action.to" :to="action.to" :class="action.style">{{ action.label }}</RouterLink>
        </div>
      </div>
    </section>

    <div class="four-col section-grid">
      <MetricCard v-for="card in dashboardCards" :key="card.label" :label="card.label" :value="card.value" :foot="card.foot" :tone="card.tone" />
    </div>

    <div class="two-col section-grid">
      <SectionCard title="最近动态" subtitle="来自后端的最新试卷与提交更新。">
        <div v-if="state.activities.length" class="data-grid">
          <article v-for="activity in state.activities" :key="activity.id" class="list-card">
            <div class="row-between">
              <strong>{{ activity.title }}</strong>
              <span class="status-pill info">{{ activity.kind }}</span>
            </div>
            <p class="muted">{{ activity.time }}</p>
          </article>
        </div>
        <div v-else class="empty-state">暂无动态。你可以先创建课程或试卷开始使用。</div>
      </SectionCard>

      <SectionCard title="待复核" subtitle="等待教师确认的学生提交。">
        <div v-if="pendingSubmissions.length" class="data-grid">
          <article v-for="submission in pendingSubmissions" :key="submission.id" class="list-card">
            <div class="row-between">
              <strong>{{ submission.studentName }} ({{ submission.studentId }})</strong>
              <span class="status-pill warning">{{ submission.statusLabel }}</span>
            </div>
            <p class="muted">{{ findPaper(submission.paperId)?.title }} / {{ findCourse(submission.courseId)?.name }}</p>
            <p class="muted">提交时间：{{ submission.submittedAtLabel }}</p>
          </article>
        </div>
        <div v-else class="empty-state">当前没有待复核的提交。</div>
      </SectionCard>
    </div>

    <SectionCard title="最近试卷" subtitle="打开历史试卷进行再次编辑或重新分享。">
      <div v-if="latestPapers.length" class="three-col section-grid">
        <article v-for="paper in latestPapers" :key="paper.id" class="list-card">
          <div class="row-between">
            <strong>{{ paper.title }}</strong>
            <span class="status-pill" :class="paper.active ? 'success' : 'info'">{{ paper.status }}</span>
          </div>
          <p class="muted">{{ paper.description || '暂无说明。' }}</p>
          <p class="muted">分享码：{{ paper.shareCode }} / 提交数：{{ paper.submittedCount }}</p>
          <div class="inline-actions">
            <RouterLink class="text-btn" :to="`/teacher/papers/${paper.id}/edit`">编辑</RouterLink>
            <RouterLink class="text-btn" :to="`/teacher/share/${paper.id}`">分享</RouterLink>
          </div>
        </article>
      </div>
      <div v-else class="empty-state">暂未找到试卷。请在试卷管理页面创建第一份试卷。</div>
    </SectionCard>
  </section>
</template>