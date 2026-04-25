<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <div class="sidebar-header">
        <h2>管理系统</h2>
      </div>
      <nav class="sidebar-nav">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          active-class="active"
        >
          <span class="nav-icon">{{ item.icon }}</span>
          <span class="nav-text">{{ item.label }}</span>
        </router-link>
      </nav>
    </aside>

    <div class="main-content">
      <header class="header">
        <div class="header-left">
          <h3>{{ currentPageTitle }}</h3>
        </div>
        <div class="header-right">
          <span class="user-info">{{ authStore.userInfo?.username }}</span>
          <button class="btn btn-default" @click="handleLogout">退出</button>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '@/store/auth';
import { message } from '@/utils/message';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const menuItems = [
  { path: '/admin/dashboard', label: '仪表盘', icon: '📊' },
  { path: '/admin/data-source', label: '数据源管理', icon: '🗄️' },
  { path: '/admin/observation-data', label: '观测数据', icon: '📈' },
  { path: '/admin/noaa-data-sync', label: 'NOAA数据同步', icon: '🌐' },
  { path: '/admin/scene', label: '场景管理', icon: '🌊' },
  { path: '/admin/chart-config', label: '图表配置', icon: '📉' },
  { path: '/admin/system-config', label: '系统配置', icon: '⚙️' },
  { path: '/admin/forum-post', label: '帖子管理', icon: '💬' },
];

const currentPageTitle = computed(() => {
  const item = menuItems.find((item) => item.path === route.path);
  return item?.label || '管理后台';
});

const handleLogout = async () => {
  await authStore.logout();
  message.success('已退出登录');
};
</script>

<style scoped lang="less">
.admin-layout {
  display: flex;
  height: 100vh;
  background: linear-gradient(135deg, #0a1a2e 0%, #16213e 50%, #1e3a5f 100%);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: 
      radial-gradient(circle at 20% 30%, rgba(74, 144, 226, 0.08) 0%, transparent 50%),
      radial-gradient(circle at 80% 70%, rgba(0, 212, 255, 0.06) 0%, transparent 50%);
    pointer-events: none;
    animation: oceanFlow 25s ease-in-out infinite;
  }
}

.sidebar {
  width: 260px;
  background: linear-gradient(135deg, rgba(30, 58, 95, 0.85), rgba(22, 33, 62, 0.85));
  backdrop-filter: blur(20px);
  border-right: 1px solid rgba(74, 144, 226, 0.3);
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 1;
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.4), inset -1px 0 0 rgba(74, 144, 226, 0.1);
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(180deg, rgba(74, 144, 226, 0.05) 0%, transparent 100%);
    pointer-events: none;
  }
}

.sidebar-header {
  padding: 24px 20px;
  border-bottom: 1px solid rgba(74, 144, 226, 0.3);
  background: linear-gradient(135deg, rgba(74, 144, 226, 0.15), rgba(0, 212, 255, 0.1));
  position: relative;
  z-index: 1;

  h2 {
    font-size: 20px;
    font-weight: 700;
    background: linear-gradient(135deg, #4a90e2 0%, #00d4ff 50%, #20b2aa 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    margin: 0;
    letter-spacing: 0.5px;
    text-shadow: 0 0 20px rgba(74, 144, 226, 0.3);
  }
}

.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  overflow-y: auto;
  overflow-x: hidden;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 14px 18px;
  margin-bottom: 6px;
  color: rgba(224, 242, 255, 0.8);
  text-decoration: none;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  font-size: 14px;
  font-weight: 500;
  z-index: 1;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 4px;
    height: 0;
    background: linear-gradient(135deg, #4a90e2, #00d4ff);
    border-radius: 0 2px 2px 0;
    transition: height 0.3s;
  }

  &:hover {
    background: rgba(74, 144, 226, 0.2);
    color: #e0f2ff;
    transform: translateX(4px);
    box-shadow: 0 4px 12px rgba(74, 144, 226, 0.25);
  }

  &.active {
    background: linear-gradient(135deg, rgba(74, 144, 226, 0.3), rgba(0, 212, 255, 0.2));
    color: #fff;
    font-weight: 600;
    box-shadow: 0 4px 16px rgba(74, 144, 226, 0.35);

    &::before {
      height: 70%;
    }
  }
}

.nav-icon {
  font-size: 20px;
  margin-right: 14px;
  width: 24px;
  text-align: center;
}

.nav-text {
  font-size: 14px;
  letter-spacing: 0.3px;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  z-index: 1;
}

.header {
  height: 64px;
  padding: 0 32px;
  background: linear-gradient(135deg, rgba(30, 58, 95, 0.7), rgba(22, 33, 62, 0.7));
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(74, 144, 226, 0.3);
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3), inset 0 1px 0 rgba(74, 144, 226, 0.1);
  position: relative;
  z-index: 1;

  h3 {
    font-size: 18px;
    font-weight: 600;
    color: #e0f2ff;
    margin: 0;
    letter-spacing: 0.3px;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-info {
  font-size: 14px;
  color: #e0f2ff;
  padding: 6px 14px;
  background: rgba(74, 144, 226, 0.2);
  border-radius: 20px;
  border: 1px solid rgba(74, 144, 226, 0.4);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 8px rgba(74, 144, 226, 0.15);
}

.content {
  flex: 1;
  padding: 32px;
  overflow-y: auto;
  overflow-x: hidden;
}

// 响应式设计
@media (max-width: 768px) {
  .sidebar {
    width: 200px;
  }

  .header {
    padding: 0 20px;
    height: 56px;

    h3 {
      font-size: 16px;
    }
  }

  .content {
    padding: 20px;
  }
}
</style>

