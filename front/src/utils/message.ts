/**
 * 消息提示工具
 * <p>
 * 提供统一的消息提示功能，支持成功、错误、警告、信息四种类型
 * 使用原生DOM操作实现，不依赖第三方UI库
 */

/**
 * 消息提示类型
 */
type MessageType = 'success' | 'error' | 'warning' | 'info';

/**
 * 创建消息提示元素
 * <p>
 * 动态创建消息提示DOM元素，并添加到页面中
 *
 * @param type 消息类型
 * @param msg 消息内容
 */
const createMessage = (type: MessageType, msg: string) => {
  // 创建消息容器（如果不存在）
  let container = document.getElementById('message-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'message-container';
    container.style.cssText = `
      position: fixed;
      top: 20px;
      left: 50%;
      transform: translateX(-50%);
      z-index: 10000;
      pointer-events: none;
    `;
    document.body.appendChild(container);
  }

  // 创建消息元素
  const messageEl = document.createElement('div');
  messageEl.style.cssText = `
    min-width: 300px;
    max-width: 500px;
    padding: 12px 20px;
    margin-bottom: 10px;
    border-radius: 8px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
    backdrop-filter: blur(10px);
    pointer-events: auto;
    animation: messageSlideIn 0.3s ease-out;
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 14px;
    line-height: 1.5;
    word-wrap: break-word;
  `;

  // 根据类型设置样式
  const typeStyles: Record<MessageType, { bg: string; color: string; border: string }> = {
    success: {
      bg: 'linear-gradient(135deg, rgba(32, 178, 170, 0.9), rgba(64, 224, 208, 0.9))',
      color: '#fff',
      border: '1px solid rgba(32, 178, 170, 0.5)',
    },
    error: {
      bg: 'linear-gradient(135deg, rgba(255, 107, 107, 0.9), rgba(238, 90, 111, 0.9))',
      color: '#fff',
      border: '1px solid rgba(255, 107, 107, 0.5)',
    },
    warning: {
      bg: 'linear-gradient(135deg, rgba(255, 193, 7, 0.9), rgba(255, 152, 0, 0.9))',
      color: '#fff',
      border: '1px solid rgba(255, 193, 7, 0.5)',
    },
    info: {
      bg: 'linear-gradient(135deg, rgba(74, 144, 226, 0.9), rgba(0, 212, 255, 0.9))',
      color: '#fff',
      border: '1px solid rgba(74, 144, 226, 0.5)',
    },
  };

  const style = typeStyles[type];
  messageEl.style.background = style.bg;
  messageEl.style.color = style.color;
  messageEl.style.border = style.border;
  messageEl.textContent = msg;

  // 添加到容器
  container.appendChild(messageEl);

  // 3秒后自动移除
  setTimeout(() => {
    messageEl.style.animation = 'messageSlideOut 0.3s ease-in';
    setTimeout(() => {
      if (messageEl.parentNode) {
        messageEl.parentNode.removeChild(messageEl);
      }
      // 如果容器为空，移除容器
      if (container && container.children.length === 0) {
        container.parentNode?.removeChild(container);
      }
    }, 300);
  }, 3000);
};

// 添加动画样式
if (!document.getElementById('message-styles')) {
  const style = document.createElement('style');
  style.id = 'message-styles';
  style.textContent = `
    @keyframes messageSlideIn {
      from {
        opacity: 0;
        transform: translateY(-20px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }
    @keyframes messageSlideOut {
      from {
        opacity: 1;
        transform: translateY(0);
      }
      to {
        opacity: 0;
        transform: translateY(-20px);
      }
    }
  `;
  document.head.appendChild(style);
}

/**
 * 外部消息实例（用于集成第三方UI库）
 */
let messageInstance: any = null;

/**
 * 消息提示对象
 * <p>
 * 提供四种类型的消息提示方法，如果设置了外部实例则使用外部实例，否则使用原生实现
 */
export const message = {
  /**
   * 成功消息提示
   *
   * @param msg 消息内容
   */
  success: (msg: string) => {
    if (messageInstance) {
      messageInstance.success(msg);
    } else {
      createMessage('success', msg);
      // 生产环境不输出console日志
      if (process.env.NODE_ENV === 'development') {
        console.log(`[成功] ${msg}`);
      }
    }
  },

  /**
   * 错误消息提示
   *
   * @param msg 消息内容
   */
  error: (msg: string) => {
    if (messageInstance) {
      messageInstance.error(msg);
    } else {
      createMessage('error', msg);
      // 错误日志始终输出，便于调试
      if (process.env.NODE_ENV === 'development') {
        console.error(`[错误] ${msg}`);
      }
    }
  },

  /**
   * 警告消息提示
   *
   * @param msg 消息内容
   */
  warning: (msg: string) => {
    if (messageInstance) {
      messageInstance.warning(msg);
    } else {
      createMessage('warning', msg);
      // 生产环境不输出console日志
      if (process.env.NODE_ENV === 'development') {
        console.warn(`[警告] ${msg}`);
      }
    }
  },

  /**
   * 信息消息提示
   *
   * @param msg 消息内容
   */
  info: (msg: string) => {
    if (messageInstance) {
      messageInstance.info(msg);
    } else {
      createMessage('info', msg);
      // 生产环境不输出console日志
      if (process.env.NODE_ENV === 'development') {
        console.info(`[信息] ${msg}`);
      }
    }
  },

  /**
   * 设置外部消息实例
   * <p>
   * 用于集成第三方UI库的消息提示组件
   *
   * @param instance 外部消息实例
   */
  setInstance: (instance: any) => {
    messageInstance = instance;
  },
};

