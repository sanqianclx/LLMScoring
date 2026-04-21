<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import SectionCard from '../../components/SectionCard.vue'
import { pushToast } from '../../composables/useToast'
import { usePlatform } from '../../services/platform'

const { state, reviewSubmission, findPaper, findCourse, refreshDashboard } = usePlatform()

const filters = reactive({
  status: 'ALL',
  keyword: ''
})
const activeSubmissionId = ref('')
const activeSubmission = ref(null)

const filtered = computed(() => state.submissions.filter((item) => {
  const statusMatch = filters.status === 'ALL' || item.status === filters.status
  const keyword = filters.keyword.trim().toLowerCase()
  const keywordMatch = !keyword
    || item.studentId.toLowerCase().includes(keyword)
    || item.studentName.toLowerCase().includes(keyword)
    || item.shareCode.toLowerCase().includes(keyword)
  return statusMatch && keywordMatch
}))

watch(filtered, (value) => {
  if (!value.length) {
    activeSubmissionId.value = ''
    activeSubmission.value = null
    return
  }

  if (!value.find((item) => item.id === activeSubmissionId.value)) {
    activeSubmissionId.value = value[0].id
  }

  const current = value.find((item) => item.id === activeSubmissionId.value)
  activeSubmission.value = current ? structuredClone(current) : null
}, { immediate: true })

const totalDraft = computed(() => activeSubmission.value?.answers.reduce((sum, answer) => sum + Number(answer.finalScore || 0), 0) || 0)

onMounted(() => {
  if (state.teacher.id) {
    refreshDashboard(state.teacher.id).catch(() => {})
  }
})

function setActive(submission) {
  activeSubmissionId.value = submission.id
  activeSubmission.value = structuredClone(submission)
}

async function reloadDashboard() {
  if (!state.teacher.id) {
    return
  }
  try {
    await refreshDashboard(state.teacher.id, true)
  } catch (error) {
    pushToast(error.message, 'error')
  }
}

async function publishReview() {
  if (!activeSubmission.value) {
    return
  }
  try {
    await reviewSubmission(activeSubmission.value)
    const latest = state.submissions.find((item) => item.id === activeSubmissionId.value)
    activeSubmission.value = latest ? structuredClone(latest) : null
  } catch (error) {
    pushToast(error.message, 'error')
  }
}
</script>

<template>
  <section class="section-grid">
    <SectionCard title="教师评阅" subtitle="查看 AI 评分结果，按题调整分数，并发布最终成绩。">
      <template #actions>
        <button class="ghost-btn" @click="reloadDashboard">刷新队列</button>
      </template>

      <div class="split-view">
        <aside class="side-card" style="padding: 18px;">
          <div class="section-grid">
            <label class="label">
              <span>状态</span>
              <select v-model="filters.status" class="select">
                <option value="ALL">全部</option>
                <option value="PENDING_REVIEW">待复核</option>
                <option value="REVIEWED">已发布</option>
              </select>
            </label>
            <label class="label">
              <span>搜索</span>
              <input v-model="filters.keyword" class="input" placeholder="学号 / 姓名 / 分享码" />
            </label>
          </div>

          <div v-if="filtered.length" class="data-grid" style="margin-top: 18px;">
            <button
              v-for="submission in filtered"
              :key="submission.id"
              class="list-card review-list-item"
              :class="{ active: activeSubmission?.id === submission.id }"
              @click="setActive(submission)"
            >
              <div class="row-between">
                <strong>{{ submission.studentName }}</strong>
                <span class="status-pill" :class="submission.status === 'PENDING_REVIEW' ? 'warning' : 'success'">{{ submission.statusLabel }}</span>
              </div>
              <p class="muted">{{ submission.studentId }} / {{ submission.submittedAtLabel }}</p>
              <p class="muted">{{ findPaper(submission.paperId)?.title }}</p>
            </button>
          </div>
          <div v-else class="empty-state">没有符合当前筛选条件的提交记录。</div>
        </aside>

        <div v-if="activeSubmission" class="section-grid">
          <article class="review-card">
            <div class="row-between">
              <div>
                <p class="kicker">提交详情</p>
                <h3 class="section-title">{{ activeSubmission.studentName }} ({{ activeSubmission.studentId }})</h3>
                <p class="muted">{{ findCourse(activeSubmission.courseId)?.name }} / {{ findPaper(activeSubmission.paperId)?.title }}</p>
              </div>
              <div class="toolbar">
                <span class="badge">AI 总分 {{ activeSubmission.autoTotal }}</span>
                <span class="badge">最终总分 {{ totalDraft }}</span>
              </div>
            </div>
          </article>

          <article v-for="answer in activeSubmission.answers" :key="answer.questionId" class="review-card">
            <div class="row-between">
              <div>
                <h4 class="section-title" style="font-size: 1.08rem;">{{ answer.stem }}</h4>
                <p class="muted">题型：{{ answer.typeLabel }}</p>
              </div>
              <span class="status-pill info">AI 已评分</span>
            </div>

            <div class="two-col section-grid" style="margin-top: 16px;">
              <div class="list-card">
                <strong>学生答案</strong>
                <p class="muted">{{ answer.answerText || '未提交作答。' }}</p>
              </div>
              <div class="list-card">
                <strong>AI 评分依据</strong>
                <p class="muted">{{ answer.rationale || '未返回评分依据。' }}</p>
                <div class="toolbar" style="margin-top: 12px;">
                  <span v-for="item in answer.matchedPoints" :key="`m-${item}`" class="status-pill success">{{ item }}</span>
                  <span v-for="item in answer.missingPoints" :key="`x-${item}`" class="status-pill danger">{{ item }}</span>
                </div>
              </div>
            </div>

            <div class="two-col section-grid" style="margin-top: 16px;">
              <label class="label">
                <span>最终得分</span>
                <div class="toolbar">
                  <input v-model="answer.finalScore" type="range" min="0" :max="answer.maxScore" class="input-range" />
                  <input v-model="answer.finalScore" class="input" type="number" min="0" :max="answer.maxScore" style="width: 120px;" />
                </div>
              </label>
              <label class="label">
                <span>教师评语</span>
                <textarea v-model="answer.comment" class="textarea" placeholder="可选：填写本题评语"></textarea>
              </label>
            </div>
          </article>

          <label class="label">
            <span>总体评语</span>
            <textarea v-model="activeSubmission.overallFeedback" class="textarea" placeholder="填写将展示给学生的最终评语"></textarea>
          </label>

          <div class="toolbar">
            <button class="secondary-btn" @click="publishReview">发布最终成绩</button>
          </div>
        </div>

        <div v-else class="empty-state">请选择一份提交记录进行评阅。</div>
      </div>
    </SectionCard>
  </section>
</template>

<style scoped>
.review-list-item {
  border: none;
  text-align: left;
  background: rgba(255, 255, 255, 0.76);
}

.review-list-item.active {
  background: linear-gradient(135deg, rgba(31, 94, 255, 0.12), rgba(83, 180, 255, 0.14));
}

.input-range {
  width: min(100%, 320px);
}
</style>