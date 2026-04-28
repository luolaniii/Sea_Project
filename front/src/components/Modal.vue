<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="modal-overlay" @click.self="handleClose">
        <div class="modal-container">
          <div class="modal-header">
            <h3>{{ title }}</h3>
            <button class="modal-close" @click="handleClose">×</button>
          </div>
          <div class="modal-body">
            <slot></slot>
          </div>
          <div v-if="showFooter" class="modal-footer">
            <button class="btn btn-default" @click="handleClose">取消</button>
            <button class="btn btn-primary" @click="handleConfirm">确定</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
/**
 * 模态框组件
 * <p>
 * 通用的模态框组件，支持自定义标题、内容、底部按钮等
 * 使用Teleport渲染到body，支持v-model双向绑定显示状态
 */

/**
 * 组件Props接口
 */
interface Props {
  /** 是否显示模态框 */
  visible: boolean;
  /** 模态框标题 */
  title: string;
  /** 是否显示底部按钮（可选，默认true） */
  showFooter?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  showFooter: true,
});

/**
 * 组件事件定义
 */
const emit = defineEmits<{
  /** 更新visible状态（用于v-model） */
  'update:visible': [value: boolean];
  /** 关闭事件 */
  close: [];
  /** 确认事件 */
  confirm: [];
}>();

/**
 * 处理关闭操作
 * <p>
 * 更新visible状态并触发close事件
 */
const handleClose = () => {
  emit('update:visible', false);
  emit('close');
};

/**
 * 处理确认操作
 * <p>
 * 触发confirm事件并关闭模态框
 */
const handleConfirm = () => {
  emit('confirm');
  handleClose();
};
</script>

<style scoped lang="less">
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(8px);
  padding: 20px;
}

.modal-container {
  background: #ffffff;
  border: 1px solid #dbe8f4;
  border-radius: 18px;
  width: 100%;
  max-width: 640px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 
    0 26px 60px -24px rgba(15, 23, 42, 0.35),
    0 10px 24px rgba(2, 132, 199, 0.12);
  overflow: hidden;
  position: relative;
  z-index: 1001;
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(180deg, #f8fbff 0%, #f2f8ff 100%);
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 2px;
    background: linear-gradient(90deg, #0284c7, #06b6d4);
  }

  h3 {
    font-size: 18px;
    font-weight: 700;
    color: #0f172a;
    margin: 0;
    letter-spacing: 0.2px;
  }
}

.modal-close {
  background: #f8fafc;
  border: 1px solid #dbe8f4;
  color: #64748b;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    background: #eff6ff;
    border-color: #bae6fd;
    color: #0284c7;
    transform: rotate(90deg);
  }
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
  overflow-x: visible;
  flex: 1;
  color: #334155;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(148, 163, 184, 0.4);
    border-radius: 3px;

    &:hover {
      background: rgba(148, 163, 184, 0.6);
    }
  }

  :deep(.form-item label) {
    color: #334155;
  }

  :deep(.form-item small) {
    color: #64748b;
  }

  :deep(select) {
    color-scheme: light;
  }
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  background: #f8fbff;
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-active .modal-container,
.modal-leave-active .modal-container {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.modal-enter-from .modal-container,
.modal-leave-to .modal-container {
  transform: scale(0.9) translateY(20px);
  opacity: 0;
}

// 响应式设计
@media (max-width: 768px) {
  .modal-overlay {
    padding: 16px;
  }

  .modal-container {
    max-width: 100%;
    border-radius: 16px;
  }

  .modal-header {
    padding: 18px 20px;
  }

  .modal-body {
    padding: 18px;
  }

  .modal-footer {
    padding: 14px 18px;
    flex-wrap: wrap;
  }
}
</style>

