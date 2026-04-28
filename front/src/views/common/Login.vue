<template>
  <div class="auth-page">
    <div class="auth-bg"></div>
    <div class="auth-shell">
      <!-- 左侧：品牌叙事 -->
      <aside class="brand-side">
        <div class="brand-mark">
          <span class="brand-logo"><Icon name="wave" :size="22" color="#ffffff" /></span>
          <span class="brand-text">OCEAN<span class="brand-tag">VISION</span></span>
        </div>

        <div class="brand-content">
          <p class="brand-eyebrow">— OCEAN INTELLIGENCE PLATFORM —</p>
          <h1 class="brand-title">让海洋数据<br /><span class="grad">可视、可读、可决策</span></h1>
          <p class="brand-sub">汇聚 NDBC 浮标、ERDDAP、AI 模型，把分散的海洋观测变成你能用的洞察。</p>
        </div>

        <ul class="brand-features">
          <li><span class="dot"></span> 实时浮标观测、3D 海面回放</li>
          <li><span class="dot"></span> 多维图表对比、跨站点分析</li>
          <li><span class="dot"></span> AI 海况预报与作业可行性建议</li>
        </ul>
      </aside>

      <!-- 右侧：表单 -->
      <main class="form-side">
        <div class="form-card">
          <div class="form-tabs">
            <button
              type="button"
              :class="['tab', { active: mode === 'login' }]"
              @click="mode = 'login'"
            >登录</button>
            <button
              type="button"
              :class="['tab', { active: mode === 'register' }]"
              @click="mode = 'register'"
            >注册</button>
            <span class="tab-indicator" :data-active="mode"></span>
          </div>

          <!-- LOGIN -->
          <form v-if="mode === 'login'" @submit.prevent="handleLogin" class="form" key="login">
            <div class="form-head">
              <h2>欢迎回来</h2>
              <p>用账号登录继续您的海洋探索</p>
            </div>

            <div class="form-item">
              <label>用户名</label>
              <input
                v-model="loginForm.username"
                type="text"
                placeholder="请输入用户名"
                autocomplete="username"
                required
              />
            </div>

            <div class="form-item">
              <label>密码</label>
              <input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                autocomplete="current-password"
                required
              />
            </div>

            <button type="submit" class="submit-btn" :disabled="loading">
              <span v-if="!loading">登录</span>
              <span v-else class="dots">登录中</span>
            </button>

            <p class="form-foot">
              还没有账号？
              <a href="#" @click.prevent="mode = 'register'">立即注册</a>
            </p>
          </form>

          <!-- REGISTER -->
          <form v-else @submit.prevent="handleRegister" class="form" key="register">
            <div class="form-head">
              <h2>创建账号</h2>
              <p>开启您的海洋数据可视化之旅</p>
            </div>

            <div class="form-item">
              <label>用户名 <span class="required">*</span></label>
              <input
                v-model="registerForm.username"
                type="text"
                placeholder="3-20 个字符"
                required
              />
            </div>

            <div class="form-item">
              <label>密码 <span class="required">*</span></label>
              <input
                v-model="registerForm.password"
                type="password"
                placeholder="至少 6 位"
                required
              />
            </div>

            <div class="form-item">
              <label>确认密码 <span class="required">*</span></label>
              <input
                v-model="confirmPassword"
                type="password"
                placeholder="再次输入密码"
                required
              />
            </div>

            <div class="form-row">
              <div class="form-item">
                <label>邮箱</label>
                <input v-model="registerForm.email" type="email" placeholder="选填" />
              </div>
              <div class="form-item">
                <label>手机号</label>
                <input v-model="registerForm.phone" type="text" placeholder="选填" />
              </div>
            </div>

            <button type="submit" class="submit-btn" :disabled="loading">
              <span v-if="!loading">注册账号</span>
              <span v-else class="dots">注册中</span>
            </button>

            <p class="form-foot">
              已有账号？
              <a href="#" @click.prevent="mode = 'login'">直接登录</a>
            </p>
          </form>
        </div>

        <p class="meta">© OCEAN/VISION · Graduation Project · 2026</p>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/auth';
import { message } from '@/utils/message';
import { authApi, type RegisterReq } from '@/utils/api-auth';
import type { LoginReq } from '@/types/auth';
import Icon from '@/components/Icon.vue';

const router = useRouter();
const authStore = useAuthStore();

const mode = ref<'login' | 'register'>('login');
const loading = ref(false);

const loginForm = ref<LoginReq>({ username: '', password: '' });
const registerForm = ref<RegisterReq>({
  username: '',
  password: '',
  email: '',
  phone: '',
  realName: '',
});
const confirmPassword = ref('');

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    message.warning('请输入用户名和密码');
    return;
  }
  loading.value = true;
  try {
    await authStore.login(loginForm.value);
    if (authStore.isAdmin) {
      router.push('/admin/dashboard');
    } else {
      router.push('/user/home');
    }
  } catch (error) {
    console.error('登录失败:', error);
  } finally {
    loading.value = false;
  }
};

const handleRegister = async () => {
  if (!registerForm.value.username || !registerForm.value.password) {
    message.warning('用户名和密码不能为空');
    return;
  }
  if (registerForm.value.username.length < 3 || registerForm.value.username.length > 20) {
    message.warning('用户名长度需为 3-20 个字符');
    return;
  }
  if (registerForm.value.password.length < 6) {
    message.warning('密码至少需要 6 位');
    return;
  }
  if (registerForm.value.password !== confirmPassword.value) {
    message.warning('两次输入的密码不一致');
    return;
  }

  loading.value = true;
  try {
    await authApi.register(registerForm.value);
    message.success('注册成功，请登录');
    loginForm.value.username = registerForm.value.username;
    loginForm.value.password = '';
    mode.value = 'login';
  } catch (error) {
    console.error('注册失败:', error);
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped lang="less">
@accent: #0284c7;
@accent-2: #0ea5e9;
@accent-cyan: #06b6d4;
@ink-1: #0f172a;
@ink-2: #334155;
@ink-3: #64748b;
@ink-4: #94a3b8;
@hairline: #e2e8f0;
@paper: #ffffff;
@bg-soft: #f4f7fa;

.auth-page {
  min-height: 100vh;
  width: 100%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background: @bg-soft;
  overflow: hidden;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}

.auth-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 18% 22%, rgba(14, 165, 233, 0.18), transparent 55%),
    radial-gradient(circle at 80% 78%, rgba(6, 182, 212, 0.14), transparent 55%),
    radial-gradient(circle at 50% 50%, rgba(125, 211, 252, 0.15), transparent 70%),
    linear-gradient(180deg, #f8fbff 0%, #eef5fb 100%);

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background-image:
      linear-gradient(rgba(15, 23, 42, 0.04) 1px, transparent 1px),
      linear-gradient(90deg, rgba(15, 23, 42, 0.04) 1px, transparent 1px);
    background-size: 48px 48px;
    mask-image: radial-gradient(circle at 50% 50%, #000 30%, transparent 80%);
    -webkit-mask-image: radial-gradient(circle at 50% 50%, #000 30%, transparent 80%);
  }
}

.auth-shell {
  position: relative;
  z-index: 1;
  width: min(1080px, 92vw);
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  gap: 0;
  background: @paper;
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 24px;
  overflow: hidden;
  box-shadow:
    0 30px 80px -20px rgba(2, 132, 199, 0.18),
    0 8px 24px rgba(15, 23, 42, 0.06);
  animation: shellIn 0.7s cubic-bezier(0.2, 0.8, 0.2, 1) both;
}

@keyframes shellIn {
  from { opacity: 0; transform: translateY(20px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ============ Brand side ============ */
.brand-side {
  position: relative;
  padding: 48px 44px;
  background:
    radial-gradient(circle at 0% 0%, rgba(14, 165, 233, 0.6), transparent 60%),
    radial-gradient(circle at 100% 100%, rgba(6, 182, 212, 0.55), transparent 60%),
    linear-gradient(135deg, #0284c7 0%, #0ea5e9 60%, #06b6d4 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  isolation: isolate;
}

.brand-side::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.06) 1px, transparent 1px);
  background-size: 32px 32px;
  z-index: -1;
  opacity: 0.6;
}

.brand-side::after {
  content: '';
  position: absolute;
  bottom: -40%;
  left: -20%;
  width: 140%;
  height: 80%;
  background: radial-gradient(ellipse at 50% 0%, rgba(255, 255, 255, 0.18), transparent 60%);
  z-index: -1;
  filter: blur(20px);
}

.brand-mark {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-logo {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.22);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(10px);
}

.brand-text {
  font-size: 19px;
  font-weight: 800;
  letter-spacing: 1.5px;
  color: #fff;

  .brand-tag {
    font-weight: 300;
    margin-left: 2px;
    color: rgba(255, 255, 255, 0.8);
  }
}

.brand-content {
  margin: 36px 0;
}

.brand-eyebrow {
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 3px;
  margin: 0 0 18px;
  color: rgba(255, 255, 255, 0.78);
  text-transform: uppercase;
}

.brand-title {
  font-size: 34px;
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -0.6px;
  margin: 0 0 20px;
  color: #fff;

  .grad {
    background: linear-gradient(135deg, #ffffff 0%, #bae6fd 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
}

.brand-sub {
  font-size: 14px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.86);
  max-width: 360px;
  margin: 0;
}

.brand-features {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.brand-features li {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13.5px;
  color: rgba(255, 255, 255, 0.92);
  font-weight: 500;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.18);
  flex-shrink: 0;
}

/* ============ Form side ============ */
.form-side {
  padding: 48px 56px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: @paper;
}

.form-card {
  width: 100%;
}

.form-tabs {
  position: relative;
  display: inline-flex;
  background: @bg-soft;
  border-radius: 10px;
  padding: 4px;
  margin-bottom: 28px;
  border: 1px solid @hairline;
}

.tab {
  position: relative;
  z-index: 1;
  padding: 8px 22px;
  background: transparent;
  border: none;
  font-size: 13.5px;
  font-weight: 600;
  color: @ink-3;
  cursor: pointer;
  border-radius: 8px;
  transition: color 0.25s ease;
  font-family: inherit;

  &.active {
    color: @ink-1;
  }
}

.tab-indicator {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.06);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  pointer-events: none;
  z-index: 0;

  &[data-active="register"] {
    transform: translateX(100%);
  }
}

.form-head {
  margin-bottom: 24px;

  h2 {
    font-size: 24px;
    font-weight: 800;
    color: @ink-1;
    margin: 0 0 6px;
    letter-spacing: -0.4px;
  }

  p {
    font-size: 13.5px;
    color: @ink-3;
    margin: 0;
  }
}

.form {
  animation: formIn 0.4s ease-out;
}

@keyframes formIn {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.form-item {
  margin-bottom: 16px;

  label {
    display: block;
    font-size: 12.5px;
    font-weight: 600;
    color: @ink-2;
    margin-bottom: 6px;
    letter-spacing: 0.1px;

    .required {
      color: #ef4444;
      margin-left: 2px;
    }
  }

  input {
    width: 100%;
    padding: 11px 14px;
    font-size: 14px;
    color: @ink-1;
    background: #f8fafc;
    border: 1.5px solid @hairline;
    border-radius: 9px;
    outline: none;
    transition: all 0.2s;
    font-family: inherit;

    &::placeholder { color: @ink-4; }

    &:hover { border-color: rgba(2, 132, 199, 0.4); }

    &:focus {
      border-color: @accent;
      background: #fff;
      box-shadow: 0 0 0 3.5px rgba(2, 132, 199, 0.12);
    }
  }
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  padding: 12px;
  font-size: 14.5px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, @accent, @accent-2);
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s;
  font-family: inherit;
  letter-spacing: 0.3px;
  box-shadow: 0 6px 20px -4px rgba(2, 132, 199, 0.4);

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 10px 24px -6px rgba(2, 132, 199, 0.5);
  }

  &:active:not(:disabled) { transform: translateY(0); }

  &:disabled {
    opacity: 0.65;
    cursor: not-allowed;
  }
}

.dots::after {
  content: '...';
  display: inline-block;
  animation: dots 1.4s infinite;
}

@keyframes dots {
  0%, 20% { content: '.'; }
  40% { content: '..'; }
  60%, 100% { content: '...'; }
}

.form-foot {
  margin-top: 18px;
  text-align: center;
  font-size: 13px;
  color: @ink-3;

  a {
    color: @accent;
    text-decoration: none;
    font-weight: 600;
    margin-left: 4px;

    &:hover {
      text-decoration: underline;
      text-underline-offset: 3px;
    }
  }
}

.meta {
  margin: 32px 0 0;
  font-size: 11px;
  color: @ink-4;
  letter-spacing: 1px;
  text-align: center;
  font-weight: 500;
}

/* ============ Responsive ============ */
@media (max-width: 880px) {
  .auth-shell {
    grid-template-columns: 1fr;
    width: min(440px, 92vw);
  }
  .brand-side {
    padding: 32px 28px;
  }
  .brand-content { margin: 18px 0; }
  .brand-title { font-size: 26px; }
  .brand-features { display: none; }
  .form-side { padding: 36px 28px; }
}
</style>
