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
  background: rgba(10, 26, 46, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(12px);
  padding: 20px;
}

.modal-container {
  background: linear-gradient(135deg, rgba(30, 58, 95, 0.95), rgba(22, 33, 62, 0.95));
  border: 1px solid rgba(74, 144, 226, 0.4);
  border-radius: 24px;
  width: 100%;
  max-width: 640px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 
    0 25px 80px rgba(0, 0, 0, 0.6),
    0 0 0 1px rgba(74, 144, 226, 0.3) inset,
    0 8px 32px rgba(74, 144, 226, 0.15);
  overflow: hidden;
  position: relative;
  z-index: 1001;
  backdrop-filter: blur(20px);
}

.modal-header {
  padding: 24px 28px;
  border-bottom: 1px solid rgba(74, 144, 226, 0.3);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, rgba(74, 144, 226, 0.15), rgba(0, 212, 255, 0.1));
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: linear-gradient(90deg, #4a90e2, #00d4ff);
  }

  h3 {
    font-size: 20px;
    font-weight: 600;
    color: #e0f2ff;
    margin: 0;
    letter-spacing: 0.3px;
    background: linear-gradient(135deg, #e0f2ff 0%, rgba(224, 242, 255, 0.9) 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
}

.modal-close {
  background: rgba(30, 58, 95, 0.6);
  border: 1px solid rgba(74, 144, 226, 0.3);
  color: rgba(224, 242, 255, 0.8);
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
  padding: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(10px);

  &:hover {
    background: rgba(74, 144, 226, 0.3);
    border-color: rgba(74, 144, 226, 0.5);
    color: #e0f2ff;
    transform: rotate(90deg);
    box-shadow: 0 4px 12px rgba(74, 144, 226, 0.3);
  }
}

.modal-body {
  padding: 28px;
  overflow-y: auto;
  overflow-x: visible;
  flex: 1;
  position: relative;
  scrollbar-width: thin;
  scrollbar-color: rgba(102, 126, 234, 0.3) transparent;
  
  // 确保下拉框不被裁剪
  :deep(select) {
    position: relative;
    z-index: 10;
  }

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(102, 126, 234, 0.3);
    border-radius: 3px;

    &:hover {
      background: rgba(102, 126, 234, 0.5);
    }
  }
}

.modal-footer {
  padding: 20px 28px;
  border-top: 1px solid rgba(74, 144, 226, 0.3);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  background: rgba(30, 58, 95, 0.3);
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
    padding: 20px 24px;

    h3 {
      font-size: 18px;
    }
  }

  .modal-body {
    padding: 24px;
  }

  .modal-footer {
    padding: 16px 24px;
    flex-wrap: wrap;
  }
}
</style>

