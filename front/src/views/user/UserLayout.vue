<template>
  <div class="user-layout" :data-theme="currentTheme">
    <header class="header">
      <div class="header-content">
        <router-link to="/user/home" class="logo">
          <span class="logo-mark"><Icon name="wave" :size="18" color="#ffffff" /></span>
          <span class="logo-text">OCEAN<span class="logo-tag">VISION</span></span>
        </router-link>

        <nav class="nav">
          <router-link to="/user/home" class="nav-item">首页</router-link>
          <router-link to="/user/profile" class="nav-item">我的</router-link>
          <router-link to="/user/station-map" class="nav-item">地图</router-link>
          <router-link to="/user/ocean-analysis" class="nav-item">分析</router-link>
          <router-link to="/user/forum" class="nav-item">社区</router-link>
        </nav>

        <div class="header-right">
          <router-link to="/user/profile" class="header-user-profile" :title="authStore.userInfo?.username">
            <div class="avatar-mini" :class="{ 'has-image': headerHasAvatar }">
              <img
                v-if="headerHasAvatar"
                :src="headerAvatarSrc"
                alt="avatar"
                @error="onHeaderAvatarError"
              />
              <span v-else>{{ headerAvatarChar }}</span>
            </div>
            <span class="header-username">{{ authStore.userInfo?.username }}</span>
          </router-link>
          <button class="logout-icon" @click="handleLogout" title="退出">
            <Icon name="logout" :size="14" />
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
import { computed, ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '@/store/auth';
import { message } from '@/utils/message';
import Icon from '@/components/Icon.vue';

const route = useRoute();
const authStore = useAuthStore();

const currentTheme = computed(() => {
  const p = route.path || '';
  if (p.includes('station-map')) return 'ocean';
  if (p.includes('analysis')) return 'cyan';
  return 'default';
});

// 头像（mini）渲染：优先显示图片；失败回退到首字母
const headerAvatarLoadFailed = ref(false);
const headerAvatarSrc = computed(() => authStore.userInfo?.avatar || '');
const headerHasAvatar = computed(() => !!headerAvatarSrc.value && !headerAvatarLoadFailed.value);
const headerAvatarChar = computed(() => {
  const u = authStore.userInfo?.username || '';
  return u.charAt(0)?.toUpperCase() || 'U';
});
const onHeaderAvatarError = () => {
  headerAvatarLoadFailed.value = true;
};

const handleLogout = async () => {
  await authStore.logout();
  message.success('已安全退出');
};
</script>

<style scoped lang="less">
.user-layout {
  min-height: 100vh;
  background: #f4f7fa;
  display: flex;
  flex-direction: column;
}

.header {
  position: sticky;
  top: 0;
  z-index: 1000;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.header-content {
  max-width: 1440px;
  margin: 0 auto;
  height: 64px;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;

  .logo-mark {
    width: 32px; height: 32px; border-radius: 9px;
    background: linear-gradient(135deg, #0284c7, #0ea5e9);
    display: flex; align-items: center; justify-content: center;
    box-shadow: 0 4px 10px rgba(2, 132, 199, 0.3);
  }

  .logo-text {
    font-size: 18px; font-weight: 800; color: #0f172a; letter-spacing: 1.5px;
    .logo-tag { font-weight: 300; color: #0284c7; margin-left: 2px; }
  }
}

.nav { display: flex; gap: 32px; }
.nav-item {
  color: #64748b; font-size: 14px; font-weight: 600; text-decoration: none;
  transition: all 0.2s; padding: 4px 0; border-bottom: 2px solid transparent;

  &:hover { color: #0f172a; }
  &.router-link-active {
    color: #0284c7;
    border-bottom-color: #0284c7;
  }
}

.header-right { display: flex; align-items: center; gap: 20px; }
.header-user-profile {
  display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 500; color: #334155;
  text-decoration: none; padding: 4px 8px; border-radius: 999px;
  transition: background 0.2s ease, color 0.2s ease;

  .avatar-mini {
    width: 28px; height: 28px; border-radius: 50%;
    background: linear-gradient(135deg, #0284c7, #06b6d4);
    border: 1.5px solid #ffffff;
    box-shadow: 0 2px 8px rgba(2, 132, 199, 0.25);
    display: flex; align-items: center; justify-content: center;
    color: #ffffff; font-size: 12px; font-weight: 700;
    overflow: hidden;
    position: relative;

    img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
    &.has-image { background: #e2e8f0; }
  }

  .header-username { line-height: 1.2; }

  &:hover { background: rgba(2, 132, 199, 0.08); color: #0284c7; }
}

.logout-icon {
  background: none; border: none; color: #94a3b8; cursor: pointer; padding: 6px;
  border-radius: 6px; transition: all 0.2s;
  &:hover { background: #fee2e2; color: #ef4444; }
}

.content {
  flex: 1; max-width: 1440px; width: 100%; margin: 0 auto; padding: 32px;
}
</style>
