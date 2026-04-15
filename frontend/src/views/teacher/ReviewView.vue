<script setup>
import { computed, ref, watch } from 'vue'
import SectionCard from '../../components/SectionCard.vue'
import { pushToast } from '../../composables/useToast'
import { usePlatform } from '../../services/platform'

const { state, reviewSubmission, findPaper, findCourse } = usePlatform()

const filters = ref({ status: 'ALL', keyword: '' })

const filtered = computed(() => state.submissions.filter((item) => {
  const statusOk = filters.value.status === 'ALL' || item.status === filters.value.status
  const keyword = filters.value.keyword.trim().toLowerCase()
  const keywordOk = !keyword || item.studentId.toLowerCase().includes(keyword) || item.studentName.toLowerCase().includes(keyword)
  return statusOk && keywordOk
}))

const activeSubmission = ref(filtered.value[0] ? structuredClone(filtered.value[0]) : null)

watch(filtered, (value) => {
  if (!value.length) {
    activeSubmission.value = null
    return
  }
  if (!value.find((item) => item.id === activeSubmission.value?.id)) {
    activeSubmission.value = structuredClone(value[0])
  }
})

const totalDraft = computed(() => activeSubmission.value?.answers.reduce((sum, answer) => sum + Number(answer.finalScore || 0), 0) || 0)

function setActive(submission) {
  activeSubmission.value = structuredClone(submission)
}

async function saveReview() {
  if (!activeSubmission.value) return
  try {
    await reviewSubmission({
      ...activeSubmission.value,
      summary: activeSubmission.value.overallFeedback || `最终总分 ${totalDraft.value} 分`
    })
    activeSubmission.value = structuredClone(state.submissions.find((item) => item.id === activeSubmission.value.id) || state.submissions[0])
  } catch (error) {
    pushToast(error.message, 'error')
  }
}
</script>

<template>
  <section class="section-grid">
    <SectionCard title="评分审核" subtitle="自动评分完成后，教师可在此逐题审核并修正最终得分。">
      <div class="split-view">
        <aside class="side-card" style="padding: 18px;">
          <div class="section-grid">
            <label class="label">
              <span>状态筛选</span>
              <select v-model="filters.status" class="select">
                <option value="ALL">全部</option>
                <option value="PENDING_REVIEW">待审核</option>
                <option value="REVIEWED">已审核</option>
              </select>
            </label>
            <label class="label">
              <span>搜索学生</span>
              <input v-model="filters.keyword" class="input" placeholder="按学号或姓名搜索" />
            </label>
          </div>

          <div class="data-grid" style="margin-top: 18px;">
            <button v-for="submission in filtered" :key="submission.id" class="list-card review-list-item" :class="{ active: activeSubmission?.id === submission.id }" @click="setActive(submission)">
              <div class="row-between">
                <strong>{{ submission.studentName }}</strong>
                <span class="status-pill" :class="submission.status === 'PENDING_REVIEW' ? 'warning' : 'success'">{{ submission.statusLabel }}</span>
              </div>
              <p class="muted">{{ submission.studentId }} / {{ submission.submittedAtLabel }}</p>
              <p class="muted">{{ findPaper(submission.paperId)?.title }}</p>
            </button>
          </div>
        </aside>

        <div v-if="activeSubmission" class="section-grid">
          <article class="review-card">
            <div class="row-between">
              <div>
                <p class="kicker">提交详情</p>
                <h3 class="section-title">{{ activeSubmission.studentName }}（{{ activeSubmission.studentId }}）</h3>
                <p class="muted">{{ findCourse(activeSubmission.courseId)?.name }} / {{ findPaper(activeSubmission.paperId)?.title }}</p>
              </div>
              <div class="toolbar">
                <span class="badge">自动评分 {{ activeSubmission.autoTotal }}</span>
                <span class="badge">最终评分 {{ totalDraft }}</span>
              </div>
            </div>
          </article>

          <article v-for="answer in activeSubmission.answers" :key="answer.questionId" class="review-card">
            <div class="row-between">
              <div>
                <h4 class="section-title" style="font-size: 1.08rem;">{{ answer.stem }}</h4>
                <p class="muted">题型：{{ answer.type }}</p>
              </div>
              <span class="status-pill info">自动评分完成</span>
            </div>

            <div class="two-col section-grid" style="margin-top: 16px;">
              <div class="list-card">
                <strong>学生答案</strong>
                <p class="muted">{{ answer.answerText || '学生未作答。' }}</p>
              </div>
              <div class="list-card">
                <strong>评分依据</strong>
                <p class="muted">{{ answer.rationale || '当前暂无评分依据。' }}</p>
                <div class="toolbar" style="margin-top: 12px;">
                  <span v-for="item in answer.matchedPoints" :key="item" class="status-pill success">{{ item }}</span>
                  <span v-for="item in answer.missingPoints" :key="item" class="status-pill danger">{{ item }}</span>
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
                <textarea v-model="answer.comment" class="textarea"></textarea>
              </label>
            </div>
          </article>

          <div class="toolbar">
            <button class="secondary-btn" @click="saveReview">保存审核结果</button>
          </div>
        </div>

        <div v-else class="empty-state">当前筛选条件下没有匹配的提交记录。</div>
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