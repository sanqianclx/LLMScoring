<script setup>
import { reactive } from 'vue'
import { pushToast } from '../../composables/useToast'

const form = reactive({
  email: ''
})

function submit() {
  if (!form.email) {
    pushToast('请输入邮箱地址', 'error')
    return
  }
  pushToast('当前版本暂未接入找回密码流程，请先使用本地演示账号登录。', 'info')
}
</script>

<template>
  <section class="section-grid">
    <div class="form-card">
      <h3 class="section-title">找回密码</h3>
      <p class="muted">该页面已保留在完整流程中，当前本地版本暂未接入实际邮件重置能力。</p>
      <label class="label" style="margin-top: 18px;">
        <span>邮箱</span>
        <input v-model="form.email" class="input" type="email" placeholder="示例：teacher@demo.com" />
      </label>
      <div class="inline-actions" style="margin-top: 20px;">
        <button class="primary-btn" @click="submit">发送重置申请</button>
      </div>
    </div>
  </section>
</template>