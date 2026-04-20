<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { pushToast } from '../../composables/useToast'
import { usePlatform } from '../../services/platform'

const route = useRoute()
const { state, loadStudentResult } = usePlatform()

const studentId = computed(() => route.query.studentId || '')
const result = computed(() => state.studentResult)

onMounted(async () => {
  try {
    await loadStudentResult(route.params.shareCode, studentId.value)
  } catch (error) {
    pushToast(error.message, 'error')
  }
})
</script>

<template>
  <section class="section-grid" v-if="result">
    <article class="result-card" style="padding: 28px;">
      <p class="kicker">结果中心</p>
      <div class="row-between">
        <div>
          <h2 class="hero-title" style="font-size: 2.4rem;">测评结果</h2>
          <p class="hero-copy">{{ result.message }}</p>
        </div>
        <div class="score-hero">
          <span>总分</span>
          <strong>{{ result.totalScore }}</strong>
        </div>
      </div>
    </article>

    <article class="list-card">
      <div class="row-between">
        <strong>{{ result.paper.title }}</strong>
        <span class="badge">{{ result.statusLabel }}</span>
      </div>
      <p class="muted">分享码：{{ result.paper.shareCode }} / 学号：{{ studentId }}</p>
      <p class="muted">总体评语：{{ result.overallFeedback }}</p>
    </article>

    <article v-if="!result.scores.length" class="empty-state">
      教师暂未发布逐题得分结果。
    </article>

    <article v-for="score in result.scores" :key="score.questionId" class="result-card">
      <div class="row-between">
        <div>
          <h3 class="section-title" style="font-size: 1.08rem;">{{ score.stem }}</h3>
          <p class="muted">题型：{{ score.typeLabel }}</p>
          <p class="muted">教师评语：{{ score.comment }}</p>
        </div>
        <span class="status-pill success">{{ score.score }}/{{ score.maxScore }}</span>
      </div>
      <div class="list-card" style="margin-top: 16px;">
        <strong>评分依据</strong>
        <p class="muted">{{ score.rationale }}</p>
      </div>
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