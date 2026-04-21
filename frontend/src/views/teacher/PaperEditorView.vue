<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import SectionCard from '../../components/SectionCard.vue'
import { pushToast } from '../../composables/useToast'
import { usePlatform } from '../../services/platform'

const route = useRoute()
const router = useRouter()
const { state, findPaper, findCourse, savePaper, deletePaper, refreshDashboard } = usePlatform()

function createEmptyDraft() {
  const seed = Date.now()
  return {
    id: '',
    title: '',
    courseId: state.courses[0]?.id || '',
    description: '',
    active: true,
    questions: [
      {
        id: `question-${seed}`,
        type: 'FILL_BLANK',
        stem: '',
        referenceAnswer: '',
        maxScore: 10,
        points: [
          {
            id: `point-${seed}`,
            keyword: '',
            description: '',
            score: 10
          }
        ]
      }
    ]
  }
}

const draft = ref(createEmptyDraft())

const selectedPaper = computed(() => {
  if (route.name === 'paper-create') {
    return null
  }
  if (route.params.paperId) {
    return findPaper(route.params.paperId)
  }
  return state.papers[0] || null
})

const totalScore = computed(() => (draft.value.questions || []).reduce((sum, question) => sum + Number(question.maxScore || 0), 0))

function syncDraft() {
  if (selectedPaper.value) {
    draft.value = structuredClone(selectedPaper.value)
  } else {
    draft.value = createEmptyDraft()
  }

  if (!draft.value.courseId && state.courses[0]?.id) {
    draft.value.courseId = state.courses[0].id
  }
}

watch([selectedPaper, () => route.name, () => state.courses.length], syncDraft, { immediate: true })

onMounted(() => {
  if (state.teacher.id) {
    refreshDashboard(state.teacher.id).catch(() => {})
  }
})

function openPaper(paperId) {
  router.push(`/teacher/papers/${paperId}/edit`)
}

function openNewPaper() {
  router.push('/teacher/papers/new')
}

function addQuestion(type = 'SHORT_ANSWER') {
  const seed = Date.now()
  draft.value.questions.push({
    id: `question-${seed}`,
    type,
    stem: '',
    referenceAnswer: '',
    maxScore: 10,
    points: [
      {
        id: `point-${seed}`,
        keyword: '',
        description: '',
        score: type === 'FILL_BLANK' ? 10 : 2
      }
    ]
  })
}

function removeQuestion(questionId) {
  if (draft.value.questions.length === 1) {
    pushToast('试卷中至少需要保留一道题目。', 'error')
    return
  }
  draft.value.questions = draft.value.questions.filter((question) => question.id !== questionId)
}

function addPoint(question) {
  question.points.push({
    id: `point-${Math.random().toString(36).slice(2, 10)}`,
    keyword: '',
    description: '',
    score: 2
  })
}

function removePoint(question, pointId) {
  if (question.points.length === 1) {
    pushToast('每道题至少需要保留一个得分点。', 'error')
    return
  }
  question.points = question.points.filter((point) => point.id !== pointId)
}

async function persistPaper() {
  try {
    const saved = await savePaper(draft.value)
    router.replace(`/teacher/papers/${saved.id}/edit`)
  } catch (error) {
    pushToast(error.message, 'error')
  }
}

async function removeCurrentPaper() {
  if (!draft.value.id) {
    draft.value = createEmptyDraft()
    return
  }

  const confirmed = window.confirm('确定删除该试卷吗？相关学生提交也会一并删除。')
  if (!confirmed) {
    return
  }

  try {
    await deletePaper(draft.value.id)
    router.push('/teacher/papers')
  } catch (error) {
    pushToast(error.message, 'error')
  }
}
</script>

<template>
  <section class="section-grid">
    <div class="row-between panel" style="padding: 24px 26px; border-radius: 28px;">
      <div>
        <p class="kicker">试卷管理</p>
        <h2 class="section-title">查看历史试卷、编辑内容并发布分享码</h2>
        <p class="muted">左侧为已保存试卷列表；右侧用于编辑当前试卷，便于教师集中完成出卷流程。</p>
      </div>
      <div class="toolbar">
        <span class="badge">总分 {{ totalScore }}</span>
        <button class="ghost-btn" @click="addQuestion('FILL_BLANK')">添加填空题</button>
        <button class="ghost-btn" @click="addQuestion('SHORT_ANSWER')">添加简答题</button>
      </div>
    </div>

    <div class="split-view paper-manage-layout">
      <aside class="side-card paper-sidebar">
        <div class="row-between">
          <div>
            <strong>已保存试卷</strong>
            <p class="muted">共 {{ state.papers.length }} 份</p>
          </div>
          <button class="primary-btn" @click="openNewPaper">新建试卷</button>
        </div>

        <div v-if="state.papers.length" class="paper-list">
          <button
            v-for="paper in state.papers"
            :key="paper.id"
            class="list-card paper-item"
            :class="{ active: draft.id === paper.id }"
            @click="openPaper(paper.id)"
          >
            <div class="row-between">
              <strong>{{ paper.title }}</strong>
              <span class="status-pill" :class="paper.active ? 'success' : 'info'">{{ paper.status }}</span>
            </div>
            <p class="muted">{{ findCourse(paper.courseId)?.name || '未关联课程' }}</p>
            <p class="muted">分享码：{{ paper.shareCode }}</p>
            <p class="muted">提交数：{{ paper.submittedCount }}</p>
          </button>
        </div>

        <div v-else class="empty-state">还没有试卷，可以在这里新建第一份试卷。</div>
      </aside>

      <div class="section-grid">
        <SectionCard title="试卷设置" subtitle="维护标题、课程、说明以及学生可见状态。">
          <div v-if="!state.courses.length" class="empty-state">请先创建至少一个课程，再创建试卷。</div>
          <div v-else class="section-grid">
            <label class="label">
              <span>试卷标题</span>
              <input v-model="draft.title" class="input" placeholder="示例：第二单元小测" />
            </label>
            <label class="label">
              <span>课程</span>
              <select v-model="draft.courseId" class="select">
                <option v-for="course in state.courses" :key="course.id" :value="course.id">{{ course.name }}</option>
              </select>
            </label>
            <label class="label">
              <span>说明</span>
              <textarea v-model="draft.description" class="textarea" placeholder="填写作答要求、评分细则或注意事项"></textarea>
            </label>
            <label class="label">
              <span>学生可见</span>
              <select v-model="draft.active" class="select">
                <option :value="true">启用</option>
                <option :value="false">停用</option>
              </select>
            </label>
          </div>

          <div class="list-card" style="margin-top: 18px;">
            <strong>当前概览</strong>
            <p class="muted">课程：{{ findCourse(draft.courseId)?.name || '未选择' }}</p>
            <p class="muted">题目数：{{ draft.questions.length }} / 总分：{{ totalScore }}</p>
            <p v-if="draft.shareCode" class="muted">分享码：{{ draft.shareCode }}</p>
          </div>

          <div class="toolbar" style="margin-top: 18px;">
            <button class="primary-btn" @click="persistPaper">保存试卷</button>
            <button class="ghost-btn" @click="removeCurrentPaper">{{ draft.id ? '删除试卷' : '重置草稿' }}</button>
            <RouterLink v-if="draft.id" class="secondary-btn" :to="`/teacher/share/${draft.id}`">分享页面</RouterLink>
          </div>
        </SectionCard>

        <SectionCard title="题目编辑" subtitle="编辑题干、参考答案与得分点，用于 AI 评分与教师复核。">
          <article v-for="(question, index) in draft.questions" :key="question.id" class="question-card">
            <div class="row-between">
              <div>
                <p class="kicker">第 {{ index + 1 }} 题</p>
                <h3 class="section-title" style="font-size: 1.08rem;">{{ question.type === 'FILL_BLANK' ? '填空题' : '简答题' }}</h3>
              </div>
              <div class="toolbar">
                <select v-model="question.type" class="select" style="width: 160px;">
                  <option value="FILL_BLANK">填空题</option>
                  <option value="SHORT_ANSWER">简答题</option>
                </select>
                <button class="text-btn" @click="removeQuestion(question.id)">删除题目</button>
              </div>
            </div>

            <div class="section-grid" style="margin-top: 16px;">
              <label class="label">
                <span>题干</span>
                <textarea v-model="question.stem" class="textarea" placeholder="请输入题干"></textarea>
              </label>
              <div class="two-col section-grid">
                <label class="label">
                  <span>参考答案</span>
                  <textarea v-model="question.referenceAnswer" class="textarea" placeholder="请输入参考答案"></textarea>
                </label>
                <label class="label">
                  <span>满分</span>
                  <input v-model="question.maxScore" class="input" type="number" min="1" step="1" />
                </label>
              </div>
            </div>

            <div class="section-grid" style="margin-top: 16px;">
              <div class="row-between">
                <strong>得分点</strong>
                <button class="ghost-btn" @click="addPoint(question)">添加得分点</button>
              </div>

              <div v-for="point in question.points" :key="point.id" class="list-card" style="padding: 16px;">
                <div class="two-col section-grid">
                  <label class="label">
                    <span>关键词</span>
                    <input v-model="point.keyword" class="input" placeholder="示例：叶绿体|chloroplast" />
                  </label>
                  <label class="label">
                    <span>分值</span>
                    <input v-model="point.score" class="input" type="number" min="1" step="1" />
                  </label>
                </div>
                <label class="label" style="margin-top: 12px;">
                  <span>描述</span>
                  <input v-model="point.description" class="input" placeholder="说明该得分点考查内容" />
                </label>
                <div class="inline-actions" style="margin-top: 12px;">
                  <button class="text-btn" @click="removePoint(question, point.id)">删除得分点</button>
                </div>
              </div>
            </div>
          </article>
        </SectionCard>
      </div>
    </div>
  </section>
</template>

<style scoped>
.paper-manage-layout {
  align-items: start;
}

.paper-sidebar {
  padding: 20px;
  display: grid;
  gap: 16px;
}

.paper-list {
  display: grid;
  gap: 12px;
}

.paper-item {
  border: none;
  text-align: left;
  background: rgba(255, 255, 255, 0.78);
}

.paper-item.active {
  background: linear-gradient(135deg, rgba(31, 94, 255, 0.12), rgba(83, 180, 255, 0.14));
}
</style>