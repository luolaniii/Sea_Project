<template>
  <div class="page-header">
    <div class="page-header-content">
      <h2 class="page-title">{{ title }}</h2>
      <div v-if="$slots.extra" class="page-header-extra">
        <slot name="extra"></slot>
      </div>
    </div>
    <div v-if="description" class="page-description">{{ description }}</div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  title: string;
  description?: string;
}

defineProps<Props>();
</script>

<style scoped lang="less">
.page-header {
  margin-bottom: 36px;
  padding-bottom: 22px;
  border-bottom: 1px solid #d8cebd;
  animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to   { opacity: 1; transform: translateY(0); }
}

.page-header-content {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 10px;
  flex-wrap: wrap;
  gap: 16px;
}

.page-title {
  font-family: 'Cormorant Garamond', 'Noto Serif SC', 'Songti SC', serif;
  font-size: 38px;
  font-weight: 500;
  color: #1c1814;
  margin: 0;
  letter-spacing: 0.3px;
  line-height: 1.1;
  position: relative;
  padding-left: 18px;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 12px;
    bottom: 10px;
    width: 2px;
    background: var(--accent, #3a5a7a);
  }
}

.page-description {
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
  font-size: 11.5px;
  color: #6e6759;
  line-height: 1.6;
  letter-spacing: 1.4px;
  text-transform: uppercase;
  padding-left: 18px;
}

.page-header-extra {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .page-header { margin-bottom: 24px; padding-bottom: 16px; }
  .page-title { font-size: 28px; padding-left: 14px; }
  .page-description { font-size: 11px; padding-left: 14px; }
}
</style>

