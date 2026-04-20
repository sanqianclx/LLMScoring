<script setup>
defineProps({
  label: String,
  value: [String, Number],
  foot: String,
  tone: {
    type: String,
    default: 'primary'
  }
})
</script>

<template>
  <article class="metric-card" :class="tone">
    <div class="metric-label">{{ label }}</div>
    <div class="metric-value">{{ value }}</div>
    <div class="metric-foot">{{ foot }}</div>
  </article>
</template>

<style scoped>
.metric-card.primary { background: linear-gradient(180deg, rgba(255,255,255,.95), rgba(230,240,255,.95)); }
.metric-card.info { background: linear-gradient(180deg, rgba(255,255,255,.95), rgba(235,248,255,.95)); }
.metric-card.warning { background: linear-gradient(180deg, rgba(255,255,255,.95), rgba(255,246,232,.95)); }
.metric-card.success { background: linear-gradient(180deg, rgba(255,255,255,.95), rgba(235,255,246,.95)); }
</style>