<script setup>
import { computed, onMounted } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import SectionCard from '../../components/SectionCard.vue'
import { pushToast } from '../../composables/useToast'
import { usePlatform } from '../../services/platform'

const route = useRoute()
const { findPaper, state, refreshDashboard } = usePlatform()

const paper = computed(() => findPaper(route.params.paperId) || state.papers[0] || null)
const submittedStudents = computed(() => state.submissions.filter((item) => item.paperId === paper.value?.id))
const pendingStudents = computed(() => submittedStudents.value.filter((item) => item.status === 'PENDING_REVIEW'))
const studentLink = computed(() => paper.value ? `${window.location.origin}/student?shareCode=${encodeURIComponent(paper.value.shareCode)}` : '')

onMounted(() => {
  if (state.teacher.id) {
    refreshDashboard(state.teacher.id).catch(() => {})
  }
})

async function copyText(text, successMessage) {
  try {
    await navigator.clipboard.writeText(text)
    pushToast(successMessage, 'success')
  } catch {
    pushToast('当前浏览器不支持复制到剪贴板。', 'error')
  }
}
</script>

<template>
  <section v-if="paper" class="section-grid">
    <SectionCard title="分享试卷" subtitle="将分享码发送给学生，并查看提交与评阅进度。">
      <div class="split-view">
        <div class="section-grid">
          <article class="result-card code-card">
            <p class="kicker">分享码</p>
            <div class="big-code">{{ paper.shareCode }}</div>
            <p class="muted">试卷：{{ paper.title }}</p>
            <p class="muted">学生入口：{{ studentLink }}</p>
            <div class="toolbar">
              <button class="primary-btn" @click="copyText(paper.shareCode, '分享码已复制。')">复制分享码</button>
              <button class="ghost-btn" @click="copyText(studentLink, '学生入口链接已复制。')">复制入口链接</button>
            </div>
          </article>

          <article class="list-card">
            <strong>学生流程</strong>
            <p class="muted">学生打开入口页，输入分享码，完成作答后使用学号提交。</p>
            <p class="muted">后端先自动评分，随后教师复核并发布最终成绩。</p>
            <div class="inline-actions" style="margin-top: 14px;">
              <RouterLink class="secondary-btn" to="/student">打开学生入口</RouterLink>
              <RouterLink class="text-btn" :to="`/teacher/papers/${paper.id}/edit`">返回编辑页</RouterLink>
            </div>
          </article>
        </div>

        <div class="section-grid">
          <article class="list-card">
            <div class="row-between">
              <strong>提交概览</strong>
              <span class="badge">共 {{ submittedStudents.length }} 份</span>
            </div>
            <p class="muted">待复核：{{ pendingStudents.length }}</p>
            <p class="muted">已发布：{{ submittedStudents.length - pendingStudents.length }}</p>
          </article>

          <article class="list-card">
            <div class="row-between">
              <strong>已提交学生</strong>
              <span class="badge">{{ submittedStudents.length }}</span>
            </div>
            <div v-if="submittedStudents.length" class="data-grid" style="margin-top: 14px;">
              <div v-for="student in submittedStudents" :key="student.id" class="row-between muted student-row">
                <span>{{ student.studentName }} ({{ student.studentId }})</span>
                <span>{{ student.statusLabel }} / {{ student.submittedAtLabel }}</span>
              </div>
            </div>
            <div v-else class="empty-state">暂时还没有学生提交该试卷。</div>
          </article>
        </div>
      </div>
    </SectionCard>
  </section>

  <section v-else class="empty-state">
    未找到试卷。
  </section>
</template>

<style scoped>
.code-card {
  padding: 24px;
}

.big-code {
  font-size: clamp(2rem, 4vw, 3.6rem);
  font-weight: 800;
  letter-spacing: 0.12em;
  color: var(--primary-deep);
  margin: 12px 0 16px;
}

.student-row {
  gap: 12px;
}
</style>