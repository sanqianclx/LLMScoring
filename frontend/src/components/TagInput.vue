<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  placeholder: {
    type: String,
    default: '输入后按回车添加'
  }
})

const emit = defineEmits(['update:modelValue'])
const draft = ref('')
const items = computed(() => props.modelValue || [])

function addDraft() {
  const values = draft.value
    .split(/[,，\n]/)
    .map((item) => item.trim())
    .filter(Boolean)

  if (!values.length) {
    return
  }

  const merged = [...new Set([...items.value, ...values])]
  emit('update:modelValue', merged)
  draft.value = ''
}

function removeItem(target) {
  emit('update:modelValue', items.value.filter((item) => item !== target))
}
</script>

<template>
  <div class="tag-box">
    <div class="tag-list" v-if="items.length">
      <span v-for="item in items" :key="item" class="tag-pill">
        {{ item }}
        <button type="button" class="tag-remove" @click="removeItem(item)">×</button>
      </span>
    </div>
    <input
      v-model="draft"
      class="input"
      :placeholder="placeholder"
      @keydown.enter.prevent="addDraft"
      @blur="addDraft"
    />
  </div>
</template>

<style scoped>
.tag-box { display: grid; gap: 10px; }
.tag-list { display: flex; flex-wrap: wrap; gap: 8px; }
.tag-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(31, 94, 255, 0.1);
  color: var(--primary-deep);
}
.tag-remove {
  border: none;
  background: transparent;
  color: inherit;
  padding: 0;
}
</style>