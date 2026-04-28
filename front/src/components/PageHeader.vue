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
  margin-bottom: 32px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e2e8f0;
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
  margin-bottom: 8px;
  flex-wrap: wrap;
  gap: 16px;
}

.page-title {
  font-family: 'Inter', -apple-system, sans-serif;
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  letter-spacing: -0.4px;
  line-height: 1.2;
  position: relative;
  padding-left: 16px;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 6px;
    bottom: 6px;
    width: 4px;
    border-radius: 2px;
    background: linear-gradient(180deg, #0284c7, #06b6d4);
  }
}

.page-description {
  font-family: 'Inter', -apple-system, sans-serif;
  font-size: 13.5px;
  color: #64748b;
  line-height: 1.6;
  letter-spacing: 0.1px;
  padding-left: 16px;
}

.page-header-extra {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .page-header { margin-bottom: 24px; padding-bottom: 16px; }
  .page-title { font-size: 22px; padding-left: 14px; }
  .page-description { font-size: 12.5px; padding-left: 14px; }
}
</style>

