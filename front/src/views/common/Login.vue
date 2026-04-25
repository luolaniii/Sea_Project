<template>
  <div class="login-page">
    <div class="login-background">
      <div class="login-card">
        <h1 class="card-title">海洋数据可视化系统</h1>
        <p class="subtitle">Ocean Data Visualization System</p>

        <form @submit.prevent="handleLogin" class="login-form">
          <div class="form-item">
            <label>用户名</label>
            <input
              v-model="form.username"
              type="text"
              placeholder="请输入用户名"
              required
              autocomplete="username"
            />
          </div>

          <div class="form-item">
            <label>密码</label>
            <input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              required
              autocomplete="current-password"
            />
          </div>

          <div class="form-actions">
            <button type="submit" class="btn btn-primary" :disabled="loading">
              {{ loading ? '登录中...' : '登录' }}
            </button>
          </div>
        </form>

        <div class="tip">
          <p>请来一起探索吧</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 登录页面组件
 * <p>
 * 提供用户登录功能，支持管理员和普通用户登录
 * 登录成功后根据用户角色跳转到对应的页面
 */

import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/auth';
import { message } from '@/utils/message';
import type { LoginReq } from '@/types/auth';

const router = useRouter();
const authStore = useAuthStore();

/**
 * 登录表单数据
 */
const form = ref<LoginReq>({
  username: '',
  password: '',
});

/**
 * 登录加载状态
 */
const loading = ref(false);

/**
 * 处理登录提交
 * <p>
 * 验证表单数据，调用登录API，成功后根据用户角色跳转
 */
const handleLogin = async () => {
  // 表单验证
  if (!form.value.username || !form.value.password) {
    message.warning('请输入用户名和密码');
    return;
  }

  loading.value = true;
  try {
    // 调用登录接口
    await authStore.login(form.value);

    // 根据角色跳转到对应页面
    if (authStore.isAdmin) {
      router.push('/admin/dashboard');
    } else {
      router.push('/user/home');
    }
  } catch (error) {
    console.error('登录失败:', error);
    // 错误信息已在http拦截器中统一处理
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped lang="less">
.login-page {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0a1a2e 0%, #16213e 50%, #1e3a5f 100%);
  position: relative;
  overflow: hidden;

  // 海洋波浪动画
  &::before {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 200px;
    background: linear-gradient(to top, rgba(74, 144, 226, 0.1), transparent);
    animation: wave 8s ease-in-out infinite;
    clip-path: polygon(0 40%, 100% 30%, 100% 100%, 0 100%);
  }

  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 150px;
    background: linear-gradient(to top, rgba(0, 212, 255, 0.08), transparent);
    animation: wave 6s ease-in-out infinite reverse;
    clip-path: polygon(0 50%, 100% 40%, 100% 100%, 0 100%);
  }
}

@keyframes wave {
  0%, 100% {
    clip-path: polygon(0 40%, 100% 30%, 100% 100%, 0 100%);
  }
  50% {
    clip-path: polygon(0 50%, 100% 40%, 100% 100%, 0 100%);
  }
}

.login-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
  display: flex;
  align-items: center;
  justify-content: center;

  > * {
    pointer-events: auto;
  }

  // 海洋气泡效果
  &::before {
    content: '';
    position: absolute;
    width: 400px;
    height: 400px;
    top: 20%;
    right: 10%;
    background: radial-gradient(circle, rgba(74, 144, 226, 0.15) 0%, transparent 70%);
    animation: bubbleFloat 15s ease-in-out infinite;
    border-radius: 50%;
  }

  &::after {
    content: '';
    position: absolute;
    width: 300px;
    height: 300px;
    bottom: 15%;
    left: 15%;
    background: radial-gradient(circle, rgba(0, 212, 255, 0.12) 0%, transparent 70%);
    animation: bubbleFloat 20s ease-in-out infinite reverse;
    border-radius: 50%;
  }
}

@keyframes bubbleFloat {
  0%, 100% {
    transform: translate(0, 0) scale(1);
    opacity: 0.6;
  }
  33% {
    transform: translate(50px, -30px) scale(1.1);
    opacity: 0.8;
  }
  66% {
    transform: translate(-30px, 40px) scale(0.9);
    opacity: 0.7;
  }
}

.login-card {
  position: relative;
  z-index: 10;
  width: 440px;
  padding: 48px 40px;
  background: linear-gradient(135deg, rgba(30, 58, 95, 0.85), rgba(22, 33, 62, 0.85));
  backdrop-filter: blur(20px);
  border: 1px solid rgba(74, 144, 226, 0.3);
  border-radius: 24px;
  box-shadow:
    0 25px 80px rgba(0, 0, 0, 0.5),
    0 0 0 1px rgba(74, 144, 226, 0.2) inset,
    0 8px 32px rgba(74, 144, 226, 0.1);
  animation: slideUp 0.6s ease-out;
  overflow: visible;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: linear-gradient(90deg, #4a90e2 0%, #00d4ff 50%, #20b2aa 100%);
    border-radius: 24px 24px 0 0;
  }

  &::after {
    content: '';
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: radial-gradient(circle, rgba(74, 144, 226, 0.1) 0%, transparent 70%);
    animation: cardGlow 8s ease-in-out infinite;
    pointer-events: none;
    z-index: 0;
  }
}

@keyframes cardGlow {
  0%, 100% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 0.3;
  }
  50% {
    transform: translate(-50%, -50%) scale(1.2);
    opacity: 0.5;
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.card-title {
  font-size: 28px;
  font-weight: 700;
  text-align: center;
  margin-bottom: 12px;
  background: linear-gradient(135deg, #4a90e2 0%, #00d4ff 50%, #20b2aa 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 0.5px;
  position: relative;
  z-index: 20;
  text-shadow: 0 0 30px rgba(74, 144, 226, 0.3);
  animation: titleShine 3s ease-in-out infinite;
}

@keyframes titleShine {
  0%, 100% {
    filter: brightness(1);
  }
  50% {
    filter: brightness(1.2);
  }
}

.subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.75);
  text-align: center;
  margin-bottom: 36px;
  letter-spacing: 0.3px;
  position: relative;
  z-index: 20;
}

.login-form {
  margin-top: 32px;
  position: relative;
  z-index: 20;
}

.form-item {
  margin-bottom: 24px;

  label {
    display: block;
    margin-bottom: 10px;
    font-size: 14px;
    font-weight: 500;
    color: rgba(255, 255, 255, 0.9);
    letter-spacing: 0.2px;
  }

  input {
    width: 100%;
    padding: 14px 18px;
    font-size: 15px;
    color: #e0f2ff;
    background: rgba(30, 58, 95, 0.5);
    border: 1px solid rgba(74, 144, 226, 0.3);
    border-radius: 12px;
    outline: none;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    backdrop-filter: blur(10px);
    position: relative;
    z-index: 30;
    pointer-events: auto;

    &:focus {
      border-color: #4a90e2;
      background: rgba(30, 58, 95, 0.7);
      box-shadow:
        0 0 0 4px rgba(74, 144, 226, 0.2),
        0 4px 12px rgba(74, 144, 226, 0.3),
        inset 0 0 20px rgba(74, 144, 226, 0.1);
      transform: translateY(-2px);
    }

    &:hover:not(:focus) {
      border-color: rgba(74, 144, 226, 0.5);
      background: rgba(30, 58, 95, 0.6);
    }

    &::placeholder {
      color: rgba(224, 242, 255, 0.5);
    }
  }
}

.form-actions {
  margin-top: 32px;
  position: relative;
  z-index: 20;

  .btn {
    width: 100%;
    padding: 14px;
    font-size: 16px;
    font-weight: 600;
    letter-spacing: 0.5px;
    box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
    transition: all 0.3s;
    position: relative;
    z-index: 30;
    pointer-events: auto;
    cursor: pointer;

    &:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
    }

    &:active:not(:disabled) {
      transform: translateY(0);
    }
  }
}

.tip {
  margin-top: 28px;
  padding: 18px;
  background: rgba(74, 144, 226, 0.15);
  border-radius: 12px;
  border: 1px solid rgba(74, 144, 226, 0.3);
  backdrop-filter: blur(10px);
  position: relative;
  overflow: hidden;
  z-index: 20;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 4px;
    height: 100%;
    background: linear-gradient(135deg, #4a90e2, #00d4ff);
    border-radius: 12px 0 0 12px;
  }

  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg, rgba(74, 144, 226, 0.1), transparent);
    opacity: 0;
    transition: opacity 0.3s;
  }

  &:hover::after {
    opacity: 1;
  }

  p {
    font-size: 13px;
    color: rgba(255, 255, 255, 0.75);
    margin: 6px 0;
    text-align: center;
    line-height: 1.6;
  }
}

// 响应式设计
@media (max-width: 480px) {
  .login-card {
    width: 90%;
    max-width: 400px;
    padding: 36px 28px;
  }

  .card-title {
    font-size: 24px;
  }
}
</style>

