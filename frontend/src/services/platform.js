import { computed, reactive } from 'vue'
import { pushToast } from '../composables/useToast'

const teacherIdStorageKey = 'llm-scoring-teacher-id'

const state = reactive({
  initialized: false,
  bootstrap: null,
  teacher: {
    id: '',
    name: '',
    username: '',
    school: '',
    taughtCourse: '',
    email: '',
    taughtCourses: [],
    role: '任课教师',
    lastLogin: ''
  },
  courses: [],
  papers: [],
  submissions: [],
  metrics: {
    courseCount: 0,
    paperCount: 0,
    submissionCount: 0,
    reviewedCount: 0,
    pendingCount: 0
  },
  activities: [],
  guideItems: [
    '请先创建课程，再在课程下维护试卷。',
    '需要向学生发放试卷时，请使用试卷分享页面。',
    '建议先在评分审核页完成人工复核，再向学生发布最终结果。',
    '可在个人中心维护教师信息与任教课程标签。'
  ],
  studentPaper: null,
  studentResult: null
})

async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    ...options
  })

  if (!response.ok) {
    let message = '请求失败'
    try {
      const body = await response.json()
      message = body.message || body.error || message
    } catch (error) {
      const text = await response.text()
      message = text || message
    }
    throw new Error(message)
  }

  if (response.status === 204) {
    return null
  }

  const text = await response.text()
  return text ? JSON.parse(text) : null
}

function toLabelStatus(status) {
  return status === 'REVIEWED' ? '已审核' : '待审核'
}

function questionTypeLabel(type) {
  return type === 'FILL_BLANK' ? '填空题' : '简答题'
}

function formatDateTime(value) {
  if (!value) {
    return '--'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function normalizeTeacher(raw) {
  return {
    ...raw,
    email: raw.username,
    taughtCourses: raw.taughtCourse ? raw.taughtCourse.split(',').map((item) => item.trim()).filter(Boolean) : [],
    role: '任课教师',
    lastLogin: new Date().toLocaleString()
  }
}

function normalizePaper(raw, submissionCount = 0) {
  return {
    ...raw,
    status: raw.active ? '进行中' : '未启用',
    submittedCount: submissionCount,
    questions: (raw.questions || []).map((question) => ({
      ...question,
      stem: question.text,
      points: (question.scoringPoints || []).map((point) => ({
        ...point,
        label: point.keyword,
        keyword: point.keyword
      }))
    }))
  }
}

function normalizeSubmission(raw, paperMap) {
  const paper = paperMap.get(raw.paperId)
  const answerMap = new Map((raw.answers || []).map((answer) => [answer.questionId, answer.answerText]))
  const autoMap = new Map((raw.autoScores || []).map((score) => [score.questionId, score]))
  const finalMap = new Map((raw.finalScores || []).map((score) => [score.questionId, score]))

  const answers = (paper?.questions || []).map((question) => {
    const autoScore = autoMap.get(question.id)
    const finalScore = finalMap.get(question.id) || autoScore
    return {
      questionId: question.id,
      stem: question.text,
      type: questionTypeLabel(question.type),
      answerText: answerMap.get(question.id) || '',
      autoScore: autoScore?.score ?? 0,
      finalScore: finalScore?.score ?? 0,
      maxScore: finalScore?.maxScore ?? question.maxScore,
      comment: finalScore?.comment || '',
      rationale: finalScore?.rationale || '',
      matchedPoints: finalScore?.matchedPoints || [],
      missingPoints: finalScore?.missingPoints || [],
      overridden: finalScore?.overridden || false
    }
  })

  return {
    ...raw,
    statusLabel: toLabelStatus(raw.status),
    submittedAtLabel: formatDateTime(raw.submittedAt),
    reviewedAtLabel: formatDateTime(raw.reviewedAt),
    answers,
    paperTitle: paper?.title || '未知试卷'
  }
}

function buildActivities(papers, submissions) {
  const recentSubmissions = submissions.slice(0, 3).map((submission) => ({
    id: `submission-${submission.id}`,
    title: `收到 ${submission.studentName} 的答卷提交`,
    time: submission.submittedAtLabel,
    kind: '提交'
  }))

  const recentPapers = papers.slice(0, 2).map((paper) => ({
    id: `paper-${paper.id}`,
    title: `试卷已发布：${paper.title}`,
    time: formatDateTime(paper.updatedAt),
    kind: '试卷'
  }))

  return [...recentSubmissions, ...recentPapers]
}

function applyDashboard(rawDashboard) {
  const paperSubmissionCount = new Map()
  for (const submission of rawDashboard.submissions || []) {
    paperSubmissionCount.set(submission.paperId, (paperSubmissionCount.get(submission.paperId) || 0) + 1)
  }

  state.teacher = normalizeTeacher(rawDashboard.teacher)
  state.courses = rawDashboard.courses || []
  state.papers = (rawDashboard.papers || []).map((paper) => normalizePaper(paper, paperSubmissionCount.get(paper.id) || 0))

  const paperMap = new Map(state.papers.map((paper) => [paper.id, paper]))
  state.submissions = (rawDashboard.submissions || []).map((submission) => normalizeSubmission(submission, paperMap))
  state.metrics = {
    courseCount: Number(rawDashboard.stats?.courseCount || 0),
    paperCount: Number(rawDashboard.stats?.paperCount || 0),
    submissionCount: Number(rawDashboard.stats?.submissionCount || 0),
    reviewedCount: Number(rawDashboard.stats?.reviewedCount || 0),
    pendingCount: Number(rawDashboard.stats?.pendingCount || 0)
  }
  state.activities = buildActivities(state.papers, state.submissions)
}

async function initializeApp() {
  if (state.initialized) {
    return
  }

  try {
    state.bootstrap = await api('/api/bootstrap')
    const savedTeacherId = localStorage.getItem(teacherIdStorageKey)
    if (savedTeacherId) {
      await refreshDashboard(savedTeacherId, false)
    }
  } catch (error) {
    pushToast(error.message, 'error')
  } finally {
    state.initialized = true
  }
}

async function refreshDashboard(teacherId = state.teacher.id, showToast = false) {
  if (!teacherId) {
    return null
  }
  const dashboard = await api(`/api/teachers/${teacherId}/dashboard`)
  applyDashboard(dashboard)
  localStorage.setItem(teacherIdStorageKey, teacherId)
  if (showToast) {
    pushToast('工作台已刷新', 'success')
  }
  return dashboard
}

async function login(form) {
  const dashboard = await api('/api/teachers/login', {
    method: 'POST',
    body: JSON.stringify({
      username: form.account,
      password: form.password
    })
  })
  applyDashboard(dashboard)
  localStorage.setItem(teacherIdStorageKey, dashboard.teacher.id)
  pushToast('登录成功', 'success')
  return true
}

async function registerTeacher(form) {
  await api('/api/teachers/register', {
    method: 'POST',
    body: JSON.stringify({
      name: form.name,
      username: form.email,
      password: form.password,
      school: form.school,
      taughtCourse: (form.courses || []).join(', ') || '通用课程'
    })
  })
  pushToast('注册完成', 'success')
  return true
}

async function updateTeacherProfile(form) {
  const teacherId = state.teacher.id
  await api(`/api/teachers/${teacherId}`, {
    method: 'PUT',
    body: JSON.stringify({
      name: form.name,
      school: form.school,
      taughtCourse: (form.taughtCourses || []).join(', ')
    })
  })
  await refreshDashboard(teacherId)
  pushToast('个人资料已更新', 'success')
}

async function createCourse(course) {
  await api(`/api/teachers/${state.teacher.id}/courses`, {
    method: 'POST',
    body: JSON.stringify({
      name: course.name,
      description: course.description
    })
  })
  await refreshDashboard(state.teacher.id)
  pushToast(`课程已创建：${course.name}`, 'success')
}

async function deleteCourse(courseId) {
  await api(`/api/teachers/${state.teacher.id}/courses/${courseId}`, {
    method: 'DELETE'
  })
  await refreshDashboard(state.teacher.id)
  pushToast('课程已删除', 'success')
}

function toPaperPayload(draft) {
  return {
    courseId: draft.courseId,
    title: draft.title,
    description: draft.description,
    active: draft.active ?? draft.status !== '未启用',
    questions: (draft.questions || []).map((question) => ({
      type: question.type,
      text: question.stem,
      referenceAnswer: question.referenceAnswer || question.standardAnswer || '',
      maxScore: Number(question.maxScore || question.points.reduce((sum, point) => sum + Number(point.score || 0), 0)),
      scoringPoints: (question.points || []).map((point) => ({
        keyword: point.keyword || point.label,
        score: Number(point.score || 0),
        description: point.description || point.label || point.keyword
      }))
    }))
  }
}

async function savePaper(draft) {
  const teacherId = state.teacher.id
  const payload = toPaperPayload(draft)
  const saved = draft.id
    ? await api(`/api/teachers/${teacherId}/papers/${draft.id}`, { method: 'PUT', body: JSON.stringify(payload) })
    : await api(`/api/teachers/${teacherId}/papers`, { method: 'POST', body: JSON.stringify(payload) })

  await refreshDashboard(teacherId)
  pushToast('试卷已保存', 'success')
  const refreshedPaper = state.papers.find((paper) => paper.id === saved.id)
  return refreshedPaper || normalizePaper(saved)
}

async function deletePaper(paperId) {
  await api(`/api/teachers/${state.teacher.id}/papers/${paperId}`, {
    method: 'DELETE'
  })
  await refreshDashboard(state.teacher.id)
  pushToast('试卷已删除', 'success')
}

async function loadStudentPaper(shareCode) {
  const raw = await api(`/api/student/papers/${encodeURIComponent(shareCode)}`)
  state.studentPaper = {
    ...raw,
    courseName: state.courses.find((course) => course.id === raw.courseId)?.name || '课程试卷',
    questions: (raw.questions || []).map((question) => ({
      ...question,
      stem: question.text,
      typeLabel: questionTypeLabel(question.type)
    }))
  }
  return state.studentPaper
}

async function submitStudentPaper(payload) {
  const response = await api(`/api/student/papers/${encodeURIComponent(payload.shareCode)}/submissions`, {
    method: 'POST',
    body: JSON.stringify({
      studentId: payload.studentId,
      studentName: payload.studentName,
      answers: payload.answers.map((answer) => ({
        questionId: answer.questionId,
        answerText: answer.answerText
      }))
    })
  })
  pushToast('试卷提交成功', 'success')
  return response
}

async function loadStudentResult(shareCode, studentId) {
  const raw = await api(`/api/student/results/${encodeURIComponent(shareCode)}?studentId=${encodeURIComponent(studentId)}`)
  const questionMap = new Map((raw.paper?.questions || []).map((question) => [question.id, question]))
  state.studentResult = {
    ...raw,
    statusLabel: toLabelStatus(raw.status),
    paper: {
      ...raw.paper,
      questions: (raw.paper?.questions || []).map((question) => ({
        ...question,
        stem: question.text,
        typeLabel: questionTypeLabel(question.type)
      }))
    },
    scores: (raw.scores || []).map((score) => ({
      ...score,
      stem: questionMap.get(score.questionId)?.text || score.questionId,
      typeLabel: questionTypeLabel(questionMap.get(score.questionId)?.type)
    }))
  }
  return state.studentResult
}

async function reviewSubmission(payload) {
  const response = await api(`/api/teachers/${state.teacher.id}/submissions/${payload.id}/review`, {
    method: 'PUT',
    body: JSON.stringify({
      questionReviews: payload.answers.map((answer) => ({
        questionId: answer.questionId,
        score: Number(answer.finalScore),
        comment: answer.comment,
        rationale: answer.rationale
      })),
      overallFeedback: payload.overallFeedback || payload.summary || ''
    })
  })
  await refreshDashboard(state.teacher.id)
  pushToast('审核结果已保存', 'success')
  return response
}

function clearAuth() {
  localStorage.removeItem(teacherIdStorageKey)
  state.teacher = {
    id: '',
    name: '',
    username: '',
    school: '',
    taughtCourse: '',
    email: '',
    taughtCourses: [],
    role: '任课教师',
    lastLogin: ''
  }
  state.courses = []
  state.papers = []
  state.submissions = []
}

export function usePlatform() {
  const dashboardCards = computed(() => [
    { label: '课程总数', value: state.metrics.courseCount, foot: '当前教师名下课程', tone: 'primary' },
    { label: '试卷数量', value: state.metrics.paperCount, foot: '已创建试卷总数', tone: 'info' },
    { label: '待审核数', value: state.metrics.pendingCount, foot: '等待教师处理', tone: 'warning' },
    { label: '已审核数', value: state.metrics.reviewedCount, foot: '已发布审核结果', tone: 'success' }
  ])

  const pendingSubmissions = computed(() => state.submissions.filter((item) => item.status === 'PENDING_REVIEW'))

  function findCourse(courseId) {
    return state.courses.find((course) => course.id === courseId)
  }

  function findPaper(paperId) {
    return state.papers.find((paper) => paper.id === paperId)
  }

  function findSubmission(submissionId) {
    return state.submissions.find((submission) => submission.id === submissionId)
  }

  return {
    state,
    dashboardCards,
    pendingSubmissions,
    initializeApp,
    refreshDashboard,
    login,
    registerTeacher,
    updateTeacherProfile,
    createCourse,
    deleteCourse,
    savePaper,
    deletePaper,
    loadStudentPaper,
    submitStudentPaper,
    loadStudentResult,
    reviewSubmission,
    clearAuth,
    findCourse,
    findPaper,
    findSubmission,
    questionTypeLabel,
    formatDateTime,
    toLabelStatus
  }
}