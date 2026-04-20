<script setup>
import { computed, markRaw, onMounted } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AuthLayout from './layouts/AuthLayout.vue'
import TeacherLayout from './layouts/TeacherLayout.vue'
import StudentLayout from './layouts/StudentLayout.vue'
import ToastStack from './components/ToastStack.vue'
import { usePlatform } from './services/platform'

const route = useRoute()
const { initializeApp } = usePlatform()

const layouts = {
  auth: markRaw(AuthLayout),
  teacher: markRaw(TeacherLayout),
  student: markRaw(StudentLayout)
}

const activeLayout = computed(() => layouts[route.meta.layout || 'auth'])

onMounted(() => {
  initializeApp()
})
</script>

<template>
  <component :is="activeLayout">
    <RouterView v-slot="{ Component }">
      <Transition name="page-fade" mode="out-in">
        <component :is="Component" />
      </Transition>
    </RouterView>
  </component>
  <ToastStack />
</template>