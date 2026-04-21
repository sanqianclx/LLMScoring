<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { pushToast } from '../../composables/useToast'
import { usePlatform } from '../../services/platform'

const route = useRoute()
const router = useRouter()
const { state, loadStudentResult } = usePlatform()
const loading = ref(false)

const queryForm = reactive({
  shareCode: route.params.shareCode || '',
  studentId: route.query.studentId || ''
})

const result = computed(() => state.studentResult)

async function fetchResult() {
  if (!queryForm.shareCode.trim() || !queryForm.studentId.trim()) {
    return
  }
  loading.value = true
  try {
    await loadStudentResult(queryForm.shareCode.trim(), queryForm.studentId.trim())
  } catch (error) {
    pushToast(error.message, 'error')
  } finally {
    loading.value = false
  }
}

function lookupResult() {
  if (!queryForm.shareCode.trim() || !queryForm.studentId.trim()) {
    pushToast('请同时填写分享码与学号。', 'error')
    return
  }
  router.replace(`/student/result/${encodeURIComponent(queryForm.shareCode.trim())}?studentId=${encodeURIComponent(queryForm.studentId.trim())}`)
  fetchResult()
}

onMounted(fetchResult)
watch(() => route.params.shareCode, (value) => {
  queryForm.shareCode = value || ''
  fetchResult()
})
watch(() => route.query.studentId, (value) => {
  queryForm.studentId = value || ''
  fetchResult()
})
</script>

<template>
  <section class="section-grid">
    <article class="form-card">
      <header class="card-header">
        <div>
          <h3 class="section-title">成绩查询</h3>
          <p class="muted">输入分享码与学号，查看当前是否仍在等待复核，或已发布最终成绩。</p>
        </div>
      </header>
      <div class="two-col section-grid">
        <label class="label">
          <span>分享码</span>
          <input v-model="queryForm.shareCode" class="input" placeholder="请输入分享码" />
        </label>
        <label class="label">
          <span>学号</span>
          <input v-model="queryForm.studentId" class="input" placeholder="请输入学号" />
        </label>
      </div>
      <div class="toolbar" style="margin-top: 16px;">
        <button class="primary-btn" @click="lookupResult">查询成绩</button>
        <RouterLink class="ghost-btn" to="/student">返回入口</RouterLink>
      </div>
    </article>

    <article v-if="loading" class="empty-state">
      正在加载提交状态...
    </article>

    <section v-else-if="result" class="section-grid">
      <article class="result-card" style="padding: 28px;">
        <p class="kicker">提交状态</p>
        <div class="row-between">
          <div>
            <h2 class="hero-title" style="font-size: 2.1rem;">{{ result.isReviewed ? '最终成绩已发布' : '提交成功，等待教师复核' }}</h2>
            <p class="hero-copy">{{ result.message }}</p>
          </div>
          <div class="score-hero">
            <span>{{ result.isReviewed ? '最终得分' : '当前得分' }}</span>
            <strong>{{ result.totalScore }}</strong>
          </div>
        </div>
      </article>

      <article class="list-card">
        <div class="row-between">
          <strong>{{ result.paper.title }}</strong>
          <span class="badge">{{ result.statusLabel }}</span>
        </div>
        <p class="muted">分享码：{{ result.paper.shareCode }} / 学号：{{ queryForm.studentId }}</p>
        <p class="muted">总体评语：{{ result.overallFeedback || '教师尚未发布总体评语。' }}</p>
      </article>

      <article v-if="result.isPending" class="empty-state">
        你的提交已进入教师复核队列，请稍后使用相同分享码与学号再次查询。
      </article>

      <article v-for="score in result.scores" :key="score.questionId" class="result-card">
        <div class="row-between">
          <div>
            <h3 class="section-title" style="font-size: 1.08rem;">{{ score.stem }}</h3>
            <p class="muted">题型：{{ score.typeLabel }}</p>
            <p class="muted">教师评语：{{ score.comment || '暂无教师评语。' }}</p>
          </div>
          <span class="status-pill success">{{ score.score }}/{{ score.maxScore }}</span>
        </div>
        <div class="list-card" style="margin-top: 16px;">
          <strong>评分依据</strong>
          <p class="muted">{{ score.rationale || '未返回评分依据。' }}</p>
        </div>
      </article>
    </section>

    <article v-else class="empty-state">
      请输入分享码与学号以加载提交记录。
    </article>
  </section>
</template>

<style scoped>
.score-hero {
  min-width: 180px;
  padding: 18px 24px;
  border-radius: 24px;
  color: white;
  background: linear-gradient(135deg, var(--primary), var(--student));
  display: grid;
  justify-items: center;
}

.score-hero strong {
  font-size: 3rem;
  line-height: 1;
}
</style>