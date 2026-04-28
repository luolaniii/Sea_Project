<template>
  <div class="pagination">
    <div class="pagination-left">
      <div class="pagination-info">
        <span class="info-text">共 <strong>{{ total }}</strong> 条</span>
        <span class="info-divider">|</span>
        <span class="info-text">每页</span>
        <select
          class="page-size-select"
          :value="pageSize"
          @change="handlePageSizeChange"
        >
          <option v-for="size in pageSizeOptions" :key="size" :value="size">
            {{ size }}
          </option>
        </select>
        <span class="info-text">条</span>
      </div>
    </div>

    <div class="pagination-center">
      <button
        class="pagination-btn pagination-btn-icon"
        :disabled="currentPage === 1"
        @click="handlePageChange(1)"
        title="首页"
      >
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <path
            d="M11 3L6 8L11 13M5 3L0 8L5 13"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </button>
      <button
        class="pagination-btn"
        :disabled="currentPage === 1"
        @click="handlePageChange(currentPage - 1)"
      >
        上一页
      </button>

      <div class="pagination-pages">
        <span class="page-info">第</span>
        <input
          class="page-input"
          type="number"
          :value="inputPage"
          :min="1"
          :max="totalPages"
          @input="handlePageInput"
          @keyup.enter="handlePageJump"
          @blur="handlePageJump"
        />
        <span class="page-info">/ {{ totalPages }} 页</span>
      </div>

      <button
        class="pagination-btn"
        :disabled="currentPage === totalPages"
        @click="handlePageChange(currentPage + 1)"
      >
        下一页
      </button>
      <button
        class="pagination-btn pagination-btn-icon"
        :disabled="currentPage === totalPages"
        @click="handlePageChange(totalPages)"
        title="末页"
      >
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <path
            d="M5 3L10 8L5 13M11 3L6 8L11 13"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';

interface Props {
  currentPage: number;
  pageSize: number;
  total: number;
  pageSizeOptions?: number[];
}

const props = withDefaults(defineProps<Props>(), {
  pageSizeOptions: () => [10, 20, 50, 100],
});

const emit = defineEmits<{
  'update:currentPage': [page: number];
  'update:pageSize': [size: number];
  change: [page: number];
  'page-size-change': [size: number];
}>();

const totalPages = computed(() => Math.ceil(props.total / props.pageSize) || 1);
const inputPage = ref(props.currentPage);

// 监听 currentPage 变化，同步到输入框
watch(
  () => props.currentPage,
  (newPage) => {
    inputPage.value = newPage;
  }
);

const handlePageChange = (page: number) => {
  const validPage = Math.max(1, Math.min(page, totalPages.value));
  if (validPage !== props.currentPage) {
    emit('update:currentPage', validPage);
    emit('change', validPage);
    inputPage.value = validPage;
  }
};

const handlePageInput = (e: Event) => {
  const target = e.target as HTMLInputElement;
  const value = parseInt(target.value, 10);
  if (!isNaN(value)) {
    inputPage.value = value;
  }
};

const handlePageJump = (e: Event) => {
  const target = e.target as HTMLInputElement;
  let page = parseInt(target.value, 10);
  
  if (isNaN(page) || page < 1) {
    page = 1;
  } else if (page > totalPages.value) {
    page = totalPages.value;
  }
  
  if (page !== props.currentPage) {
    handlePageChange(page);
  } else {
    inputPage.value = page;
  }
};

const handlePageSizeChange = (e: Event) => {
  const target = e.target as HTMLSelectElement;
  const newSize = parseInt(target.value, 10);
  const maxPage = Math.ceil(props.total / newSize) || 1;
  const newPage = Math.min(props.currentPage, maxPage);
  
  emit('update:pageSize', newSize);
  emit('page-size-change', newSize);
  
  if (newPage !== props.currentPage) {
    emit('update:currentPage', newPage);
    emit('change', newPage);
    inputPage.value = newPage;
  }
};
</script>

<style scoped lang="less">
@hairline: #e2e8f0;
@accent: #0284c7;
@ink-1: #0f172a;
@ink-2: #334155;
@ink-3: #64748b;
@paper: #ffffff;
@bg-soft: #f8fafc;

.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 0;
  gap: 20px;
  flex-wrap: wrap;
}

.pagination-left {
  display: flex;
  align-items: center;
}

.pagination-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: @ink-3;
  font-family: 'Inter', -apple-system, sans-serif;

  .info-text {
    white-space: nowrap;

    strong {
      color: @ink-1;
      font-weight: 600;
    }
  }

  .info-divider {
    color: @hairline;
    margin: 0 4px;
  }
}

.page-size-select {
  width: 70px;
  padding: 5px 10px;
  padding-right: 28px;
  font-size: 12px;
  color: @ink-2;
  background: @paper;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2364748b' d='M6 9L1 4h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 10px center;
  border: 1px solid @hairline;
  border-radius: 6px;
  outline: none;
  cursor: pointer;
  transition: all 0.2s;
  appearance: none;
  font-family: 'Inter', -apple-system, sans-serif;

  &::-ms-expand { display: none; }

  &:hover { border-color: @accent; }

  &:focus {
    border-color: @accent;
    box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.12);
  }

  option {
    background: #fff !important;
    color: @ink-2 !important;
  }
}

.pagination-center {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pagination-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 7px 14px;
  font-size: 12.5px;
  font-weight: 500;
  color: @ink-2;
  background: @paper;
  border: 1px solid @hairline;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  outline: none;
  white-space: nowrap;
  font-family: 'Inter', -apple-system, sans-serif;

  &:hover:not(:disabled) {
    background: @bg-soft;
    border-color: @accent;
    color: @accent;
  }

  &:active:not(:disabled) { transform: scale(0.98); }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  &.pagination-btn-icon {
    padding: 7px 10px;
    color: @ink-3;

    svg { display: block; }
  }
}

.pagination-pages {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 6px;
}

.page-info {
  font-size: 12.5px;
  color: @ink-3;
  white-space: nowrap;
  font-family: 'Inter', -apple-system, sans-serif;
}

.page-input {
  width: 56px;
  padding: 6px 8px;
  font-size: 12.5px;
  font-weight: 600;
  text-align: center;
  color: @ink-1;
  background: @paper;
  border: 1px solid @hairline;
  border-radius: 6px;
  outline: none;
  transition: all 0.2s;
  font-family: 'Inter', -apple-system, sans-serif;

  &:hover { border-color: @accent; }

  &:focus {
    border-color: @accent;
    box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.12);
  }

  &::-webkit-inner-spin-button,
  &::-webkit-outer-spin-button {
    -webkit-appearance: none;
    margin: 0;
  }

  &[type='number'] { -moz-appearance: textfield; }
}

// 响应式设计
@media (max-width: 768px) {
  .pagination {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .pagination-left {
    justify-content: center;
  }

  .pagination-center {
    justify-content: center;
    flex-wrap: wrap;
  }

  .pagination-btn {
    padding: 6px 12px;
    font-size: 12px;
  }

  .page-input {
    width: 50px;
    padding: 6px 8px;
  }
}
</style>

