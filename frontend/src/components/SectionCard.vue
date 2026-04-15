<script setup>
defineProps({
  title: String,
  subtitle: String
})
</script>

<template>
  <section class="form-card">
    <header class="card-header">
      <div>
        <h3 class="section-title">{{ title }}</h3>
        <p class="muted" v-if="subtitle">{{ subtitle }}</p>
      </div>
      <slot name="actions" />
    </header>
    <slot />
  </section>
</template>