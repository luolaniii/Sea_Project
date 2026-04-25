<template>
  <div class="user-layout" :data-theme="currentTheme">
    <header class="header">
      <div class="header-content">
        <router-link to="/user/home" class="logo">
          <span class="logo-mark"><Icon name="wave" :size="20" :color="accentColor" /></span>
          <span class="logo-text">OCEAN<span class="logo-divider">/</span><i>vision</i></span>
        </router-link>
        <nav class="nav">
          <router-link to="/user/home" class="nav-item" active-class="active">
            <Icon name="home" :size="15" />
            <span>首页</span>
          </router-link>
          <router-link to="/user/station-map" class="nav-item" active-class="active">
            <Icon name="map" :size="15" />
            <span>站点地图</span>
          </router-link>
          <router-link to="/user/ocean-analysis" class="nav-item" active-class="active">
            <Icon name="brain" :size="15" />
            <span>辅助决策</span>
          </router-link>
          <router-link to="/user/ocean-comparison" class="nav-item" active-class="active">
            <Icon name="bars" :size="15" />
            <span>多站点对比</span>
          </router-link>
          <router-link to="/user/forum" class="nav-item" active-class="active">
            <Icon name="chat" :size="15" />
            <span>论坛</span>
          </router-link>
          <router-link to="/user/my-posts" class="nav-item" active-class="active">
            <Icon name="edit" :size="15" />
            <span>我的帖子</span>
          </router-link>
        </nav>
        <div class="header-right">
          <span class="user-info">
            <Icon name="user" :size="14" />
            {{ authStore.userInfo?.username }}
          </span>
          <button class="logout-btn" @click="handleLogout">
            <Icon name="logout" :size="13" />
            <span>退出</span>
          </button>
        </div>
      </div>
    </header>

    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '@/store/auth';
import { message } from '@/utils/message';
import Icon from '@/components/Icon.vue';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

// 路由 → 莫兰迪色相 key（每个页面一个低饱和度暖色或冷色）
const currentTheme = computed(() => {
  const p = route.path || '';
  if (p.startsWith('/user/station-map')) return 'mist';
  if (p.startsWith('/user/ocean-analysis')) return 'plum';
  if (p.startsWith('/user/ocean-comparison')) return 'sage';
  if (p.startsWith('/user/forum')) return 'taupe';
  if (p.startsWith('/user/my-posts')) return 'mauve';
  if (p.startsWith('/user/scene')) return 'mist';
  if (p.startsWith('/user/chart')) return 'sage';
  return 'ocean';
});

// 主题强调色（用于 logo 标记 / Icon 颜色）
const accentColor = computed(() => {
  switch (currentTheme.value) {
    case 'mist':  return '#5a728a';
    case 'plum':  return '#7a5a78';
    case 'sage':  return '#6f8a6a';
    case 'taupe': return '#9a7e5e';
    case 'mauve': return '#9c6e7a';
    default:      return '#3a5a7a';
  }
});

const handleLogout = async () => {
  await authStore.logout();
  message.success('已退出登录');
};
</script>

<style scoped lang="less">
@cream-base: #f1eadc;
@cream-tint: #e8e0cf;
@paper:      #fbf7ee;
@hairline:   #d8cebd;
@taupe-soft: #c2b8a6;
@taupe:      #8d8576;
@ink-1:      #1c1814;
@ink-2:      #3a342c;
@ink-3:      #6e6759;

.user-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow-x: hidden;

  // 默认（ocean）色相变量
  --tint-1: rgba(90, 114, 138, 0.08);
  --tint-2: rgba(154, 126, 94, 0.06);
  --accent: #3a5a7a;

  background: @cream-base;
  transition: background 0.6s ease;

  // 极淡的色相光晕，固定在视口左上+右下
  &::before {
    content: '';
    position: fixed;
    inset: 0;
    pointer-events: none;
    z-index: 0;
    background:
      radial-gradient(ellipse at 8% 0%, var(--tint-1) 0%, transparent 55%),
      radial-gradient(ellipse at 95% 100%, var(--tint-2) 0%, transparent 50%);
    transition: background 0.6s ease;
  }

  // 莫兰迪色相主题
  &[data-theme='mist']  { --tint-1: rgba(90,114,138,0.10); --tint-2: rgba(120,140,160,0.06); --accent: #5a728a; }
  &[data-theme='plum']  { --tint-1: rgba(122,90,120,0.10); --tint-2: rgba(154,126,134,0.06); --accent: #7a5a78; }
  &[data-theme='sage']  { --tint-1: rgba(111,138,106,0.10); --tint-2: rgba(140,160,120,0.06); --accent: #6f8a6a; }
  &[data-theme='taupe'] { --tint-1: rgba(154,126,94,0.10); --tint-2: rgba(176,144,108,0.06); --accent: #9a7e5e; }
  &[data-theme='mauve'] { --tint-1: rgba(156,110,122,0.10); --tint-2: rgba(180,140,150,0.06); --accent: #9c6e7a; }
}

.header {
  background: rgba(251, 247, 238, 0.78);
  backdrop-filter: blur(20px) saturate(140%);
  -webkit-backdrop-filter: blur(20px) saturate(140%);
  border-bottom: 1px solid @hairline;
  padding: 0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 1600px;
  margin: 0 auto;
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  font-family: 'JetBrains Mono', 'Fira Code', 'Menlo', monospace;
}

.logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: 1px solid @hairline;
  background: @paper;
  transition: border-color 0.3s ease;
}
.logo:hover .logo-mark {
  border-color: var(--accent);
}

.logo-text {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 2.4px;
  color: @ink-1;
  text-transform: uppercase;

  i {
    font-style: normal;
    font-weight: 400;
    letter-spacing: 1.2px;
    color: var(--accent);
  }
}

.logo-divider {
  margin: 0 4px;
  color: @taupe-soft;
  font-weight: 300;
}

.nav {
  display: flex;
  gap: 2px;
  flex: 1;
  justify-content: center;
  margin: 0 40px;
}

.nav-item {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 8px 14px;
  color: @ink-3;
  text-decoration: none;
  border-radius: 4px;
  transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.4px;
  position: relative;

  .icon { opacity: 0.7; transition: all 0.22s ease; }

  &::after {
    content: '';
    position: absolute;
    left: 14px;
    right: 14px;
    bottom: 4px;
    height: 1px;
    background: currentColor;
    transform: scaleX(0);
    transform-origin: left;
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    opacity: 0.5;
  }

  &:hover {
    color: @ink-1;
    .icon { opacity: 1; }
    &::after { transform: scaleX(1); }
  }

  &.active {
    color: var(--accent);
    .icon { opacity: 1; }
    &::after { transform: scaleX(1); opacity: 1; }
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: @ink-2;
  padding: 6px 12px;
  background: @cream-tint;
  border-radius: 999px;
  letter-spacing: 0.3px;
  font-family: 'JetBrains Mono', monospace;
}

.logout-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 13px;
  font-size: 12.5px;
  letter-spacing: 0.3px;
  color: @ink-3;
  background: transparent;
  border: 1px solid @hairline;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.22s ease;
  font-family: inherit;

  &:hover {
    color: #a64a3e;
    border-color: #d6a89e;
    background: rgba(166, 74, 62, 0.06);
  }
}

.content {
  flex: 1;
  max-width: 1600px;
  width: 100%;
  margin: 0 auto;
  padding: 36px 32px;
  position: relative;
  z-index: 1;
}

@media (max-width: 1024px) {
  .header-content { padding: 0 24px; height: 60px; }
  .nav { gap: 2px; margin: 0 20px; }
  .nav-item { padding: 7px 12px; font-size: 12.5px; }
  .content { padding: 24px; }
}

@media (max-width: 768px) {
  .header-content {
    flex-wrap: wrap;
    height: auto;
    padding: 12px 20px;
  }
  .nav {
    order: 3;
    width: 100%;
    margin: 12px 0 0;
    justify-content: flex-start;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
  .content { padding: 20px; }
}
</style>
