<template>
  <div class="data-table">
    <div class="table-container">
      <table>
        <thead>
          <tr>
            <th v-for="col in columns" :key="col.key" :style="col.style">
              {{ col.title }}
            </th>
            <th v-if="actions.length > 0" style="width: 150px; text-align: center">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td :colspan="columns.length + (actions.length > 0 ? 1 : 0)" class="text-center">
              加载中...
            </td>
          </tr>
          <tr v-else-if="data.length === 0">
            <td :colspan="columns.length + (actions.length > 0 ? 1 : 0)" class="text-center">
              暂无数据
            </td>
          </tr>
          <tr v-else v-for="row in data" :key="row.id">
            <td v-for="col in columns" :key="col.key" :style="col.style">
              <slot :name="col.key" :row="row">
                {{ formatValue(row[col.key], col, row) }}
              </slot>
            </td>
            <td v-if="actions.length > 0" class="text-center">
              <div class="table-actions">
                <button
                  v-for="action in actions"
                  :key="action.key"
                  :class="['btn', `btn-${action.type || 'default'}`, 'table-action-btn']"
                  @click="handleAction(action, row)"
                >
                  {{ action.label }}
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 数据表格组件
 * <p>
 * 通用的数据表格组件，支持自定义列、操作按钮、数据格式化等功能
 * 提供加载状态、空数据提示等UI反馈
 */

/**
 * 表格列配置接口
 */
interface Column {
  /** 列字段名 */
  key: string;
  /** 列标题 */
  title: string;
  /** 列样式（可选） */
  style?: Record<string, any>;
  /** 数据格式化函数（可选） */
  formatter?: (value: any, row: any) => string | number;
}

/**
 * 操作按钮配置接口
 */
interface Action {
  /** 操作唯一标识 */
  key: string;
  /** 按钮文本 */
  label: string;
  /** 按钮类型（可选） */
  type?: 'primary' | 'success' | 'danger' | 'default';
}

/**
 * 组件Props接口
 */
interface Props {
  /** 表格列配置数组 */
  columns: Column[];
  /** 表格数据数组 */
  data: any[];
  /** 操作按钮配置数组（可选） */
  actions?: Action[];
  /** 加载状态（可选） */
  loading?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  actions: () => [],
  loading: false,
});

/**
 * 组件事件定义
 */
const emit = defineEmits<{
  /** 操作按钮点击事件 */
  action: [action: Action, row: any];
}>();

/**
 * 格式化单元格值
 * <p>
 * 如果列配置了formatter函数则使用formatter，否则处理null/undefined值
 *
 * @param value 单元格值
 * @param col 列配置
 * @param row 行数据
 * @returns 格式化后的值
 */
const formatValue = (value: any, col: Column, row: any) => {
  if (col.formatter) {
    return col.formatter(value, row);
  }
  if (value === null || value === undefined) {
    return '-';
  }
  return String(value);
};

/**
 * 处理操作按钮点击
 * <p>
 * 触发action事件，传递操作配置和行数据
 *
 * @param action 操作配置
 * @param row 行数据
 */
const handleAction = (action: Action, row: any) => {
  emit('action', action, row);
};
</script>

<style scoped lang="less">
.data-table {
  width: 100%;
  animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.table-container {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #dbe8f4;
  box-shadow: 0 10px 24px -16px rgba(15, 23, 42, 0.35);
}

:deep(table) {
  width: 100%;
  border-collapse: collapse;
  background: #ffffff;

  thead {
    background: #f4f9ff;

    th {
      padding: 14px 18px;
      text-align: left;
      font-weight: 600;
      font-size: 13px;
      color: #475569;
      border-bottom: 1px solid #dbe8f4;
      letter-spacing: 0.2px;
      white-space: nowrap;
    }
  }

  tbody {
    tr {
      border-bottom: 1px solid #eef2f7;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

      &:hover {
        background: #f8fbff;
      }

      &:last-child {
        border-bottom: none;
      }
    }

    td {
      padding: 14px 18px;
      font-size: 14px;
      color: #334155;
      line-height: 1.6;
    }
  }
}

.text-center {
  text-align: center;
  color: #64748b;
  padding: 40px 20px !important;
  font-size: 14px;
}

/* 通用开关样式，供列表页布尔字段使用 */
:deep(.switch) {
  position: relative;
  display: inline-block;
  width: 46px;
  height: 24px;
  vertical-align: middle;
}

:deep(.switch input) {
  opacity: 0;
  width: 0;
  height: 0;
}

:deep(.slider) {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #d1d5db;
  transition: 0.4s;
  border-radius: 24px;
}

:deep(.slider:before) {
  position: absolute;
  content: '';
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: 0.4s;
  border-radius: 50%;
}

:deep(input:checked + .slider) {
  background-color: #0284c7;
}

:deep(input:focus + .slider) {
  box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.18);
}

:deep(input:checked + .slider:before) {
  transform: translateX(22px);
}

.table-actions {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

:deep(.table-action-btn) {
  min-width: 96px;
  height: 32px;
  padding: 0 14px;
  font-size: 12px;
  line-height: 1;
}

// 响应式设计
@media (max-width: 768px) {
  .table-container {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  :deep(table) {
    min-width: 800px;

    thead th,
    tbody td {
      padding: 12px 16px;
      font-size: 13px;
    }
  }
}
</style>

