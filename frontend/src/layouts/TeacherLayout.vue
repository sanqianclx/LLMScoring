<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppSidebar from '../components/AppSidebar.vue'
import AppTopbar from '../components/AppTopbar.vue'

const route = useRoute()
const title = computed(() => route.meta.title || 'Teacher Workspace')
</script>

<template>
  <div class="app-page">
    <div class="layout-shell">
      <AppSidebar />
      <div class="content-shell">
        <AppTopbar :title="title" />
        <slot />
      </div>
    </div>
  </div>
</template>