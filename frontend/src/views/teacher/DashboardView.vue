<script setup>
import { computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import MetricCard from '../../components/MetricCard.vue'
import SectionCard from '../../components/SectionCard.vue'
import { usePlatform } from '../../services/platform'

const { state, dashboardCards, pendingSubmissions, findCourse, findPaper, refreshDashboard } = usePlatform()

const latestPapers = computed(() => state.papers.slice(0, 3))
const quickActions = [
  { label: '创建课程', to: '/teacher/courses', style: 'primary-btn' },
  { label: '创建试卷', to: '/teacher/papers/new', style: 'secondary-btn' },
  { label: '进入审核', to: '/teacher/review', style: 'ghost-btn' }
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
      <p class="kicker">工作台概览</p>
      <div class="row-between">
        <div>
          <h2 class="hero-title" style="font-size: 2.5rem;">{{ state.teacher.name || '教师' }}的教学工作区</h2>
          <p class="hero-copy">这里汇总了课程、试卷、学生提交、待审核记录和最近动态，数据均来自当前后端服务。</p>
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
      <SectionCard title="最近动态" subtitle="根据最新试卷与学生提交自动汇总。">
        <div class="data-grid" v-if="state.activities.length">
          <article v-for="activity in state.activities" :key="activity.id" class="list-card">
            <div class="row-between">
              <strong>{{ activity.title }}</strong>
              <span class="status-pill info">{{ activity.kind }}</span>
            </div>
            <p class="muted">{{ activity.time }}</p>
          </article>
        </div>
        <div v-else class="empty-state">当前还没有动态记录，创建课程或试卷后会显示在这里。</div>
      </SectionCard>

      <SectionCard title="待审核评分" subtitle="展示最新待审核的学生提交记录。">
        <div class="data-grid" v-if="pendingSubmissions.length">
          <article v-for="submission in pendingSubmissions" :key="submission.id" class="list-card">
            <div class="row-between">
              <strong>{{ submission.studentName }}（{{ submission.studentId }}）</strong>
              <span class="status-pill warning">{{ submission.statusLabel }}</span>
            </div>
            <p class="muted">{{ findPaper(submission.paperId)?.title }} / {{ findCourse(submission.courseId)?.name }}</p>
            <p class="muted">提交时间：{{ submission.submittedAtLabel }}</p>
          </article>
        </div>
        <div v-else class="empty-state">当前没有待审核记录。</div>
      </SectionCard>
    </div>

    <SectionCard title="试卷概览" subtitle="可直接进入编辑页和分享页。">
      <div class="three-col section-grid">
        <article v-for="paper in latestPapers" :key="paper.id" class="list-card">
          <div class="row-between">
            <strong>{{ paper.title }}</strong>
            <span class="status-pill" :class="paper.active ? 'success' : 'info'">{{ paper.status }}</span>
          </div>
          <p class="muted">{{ paper.description }}</p>
          <p class="muted">分享码：{{ paper.shareCode }} / 提交人数：{{ paper.submittedCount }}</p>
          <div class="inline-actions">
            <RouterLink class="text-btn" :to="`/teacher/papers/${paper.id}/edit`">编辑</RouterLink>
            <RouterLink class="text-btn" :to="`/teacher/share/${paper.id}`">分享</RouterLink>
          </div>
        </article>
      </div>
    </SectionCard>
  </section>
</template>