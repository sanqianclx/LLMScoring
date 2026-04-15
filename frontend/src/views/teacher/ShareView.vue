<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SectionCard from '../../components/SectionCard.vue'
import { pushToast } from '../../composables/useToast'
import { usePlatform } from '../../services/platform'

const route = useRoute()
const { findPaper, state } = usePlatform()

const paper = computed(() => findPaper(route.params.paperId) || state.papers[0])
const submittedStudents = computed(() => state.submissions.filter((item) => item.paperId === paper.value?.id))

const qrCells = computed(() => {
  const seed = paper.value?.shareCode || 'BIO2026'
  return Array.from({ length: 169 }, (_, index) => {
    const code = seed.charCodeAt(index % seed.length)
    return ((code + index * 7) % 5) < 2
  })
})

async function copyCode() {
  try {
    await navigator.clipboard.writeText(paper.value.shareCode)
    pushToast('分享码已复制', 'success')
  } catch (error) {
    pushToast('当前浏览器不支持剪贴板复制', 'error')
  }
}
</script>

<template>
  <section class="section-grid" v-if="paper">
    <SectionCard title="试卷分享" subtitle="展示分享码、课堂展示码和已提交学生列表。">
      <div class="split-view">
        <div class="section-grid">
          <article class="result-card code-card">
            <p class="kicker">分享码</p>
            <div class="big-code">{{ paper.shareCode }}</div>
            <p class="muted">试卷标题：{{ paper.title }}</p>
            <div class="toolbar">
              <button class="primary-btn" @click="copyCode">复制分享码</button>
              <button class="ghost-btn">刷新展示码</button>
            </div>
          </article>

          <article class="list-card">
            <strong>使用说明</strong>
            <p class="muted">可直接将学生入口链接与分享码发给学生，也可在课堂中投屏展示本页。</p>
          </article>
        </div>

        <div class="section-grid">
          <article class="result-card qr-card">
            <div class="qr-grid">
              <span v-for="(cell, index) in qrCells" :key="index" :class="['qr-cell', { on: cell }]"></span>
            </div>
            <p class="muted">当前为课堂展示用的简化视觉码，后续可以替换为正式二维码。</p>
          </article>

          <article class="list-card">
            <div class="row-between">
              <strong>已提交学生</strong>
              <span class="badge">{{ submittedStudents.length }}</span>
            </div>
            <div class="data-grid" style="margin-top: 14px;">
              <div v-for="student in submittedStudents" :key="student.id" class="row-between muted">
                <span>{{ student.studentName }}（{{ student.studentId }}）</span>
                <span>{{ student.submittedAtLabel }}</span>
              </div>
            </div>
          </article>
        </div>
      </div>
    </SectionCard>
  </section>
</template>

<style scoped>
.code-card,
.qr-card {
  padding: 24px;
}
.big-code {
  font-size: clamp(2rem, 4vw, 3.6rem);
  font-weight: 800;
  letter-spacing: 0.12em;
  color: var(--primary-deep);
  margin: 12px 0 16px;
}
.qr-grid {
  width: min(100%, 320px);
  aspect-ratio: 1;
  display: grid;
  grid-template-columns: repeat(13, 1fr);
  gap: 4px;
  margin: 0 auto 18px;
  padding: 18px;
  border-radius: 24px;
  background: white;
}
.qr-cell {
  border-radius: 4px;
  background: rgba(31, 94, 255, 0.08);
}
.qr-cell.on {
  background: var(--primary-deep);
}
</style>