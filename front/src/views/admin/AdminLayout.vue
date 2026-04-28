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
          <span class="nav-icon">
            <Icon :name="item.icon" :size="18" />
          </span>
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
import Icon from '@/components/Icon.vue';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const menuItems = [
  { path: '/admin/dashboard', label: '仪表盘', icon: 'dashboard' },
  { path: '/admin/data-source', label: '数据源管理', icon: 'database' },
  { path: '/admin/observation-data', label: '观测数据', icon: 'chart' },
  { path: '/admin/noaa-data-sync', label: 'NOAA数据同步', icon: 'refresh' },
  { path: '/admin/scene', label: '场景管理', icon: 'scene' },
  { path: '/admin/chart-config', label: '图表配置', icon: 'trendDown' },
  { path: '/admin/system-config', label: '系统配置', icon: 'settings' },
  { path: '/admin/forum-post', label: '帖子管理', icon: 'chat' },
  { path: '/admin/expert-application', label: '专家审核', icon: 'check' },
  { path: '/admin/recharge', label: '充值订单', icon: 'database' },
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
  background: linear-gradient(180deg, #f5f9ff 0%, #eef4fb 100%);
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
      radial-gradient(circle at 18% 24%, rgba(14, 165, 233, 0.15), transparent 48%),
      radial-gradient(circle at 84% 76%, rgba(6, 182, 212, 0.12), transparent 45%);
    pointer-events: none;
  }
}

.sidebar {
  width: 260px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border-right: 1px solid #dbe8f4;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 1;
  box-shadow: 8px 0 28px -24px rgba(15, 23, 42, 0.35);
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(180deg, rgba(2, 132, 199, 0.06) 0%, transparent 100%);
    pointer-events: none;
  }
}

.sidebar-header {
  padding: 24px 20px;
  border-bottom: 1px solid #dbe8f4;
  background: linear-gradient(180deg, #f4f9ff 0%, #eff6ff 100%);
  position: relative;
  z-index: 1;

  h2 {
    font-size: 20px;
    font-weight: 700;
    background: linear-gradient(135deg, #0284c7 0%, #06b6d4 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    margin: 0;
    letter-spacing: 0.5px;
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
  color: #334155;
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
    background: linear-gradient(135deg, #0284c7, #06b6d4);
    border-radius: 0 2px 2px 0;
    transition: height 0.3s;
  }

  &:hover {
    background: rgba(2, 132, 199, 0.1);
    color: #0f172a;
    transform: translateX(4px);
  }

  &.active {
    background: linear-gradient(135deg, rgba(2, 132, 199, 0.18), rgba(6, 182, 212, 0.14));
    color: #0f172a;
    font-weight: 600;
    box-shadow: 0 8px 18px -14px rgba(2, 132, 199, 0.45);

    &::before {
      height: 70%;
    }
  }
}

.nav-icon {
  font-size: 0;
  margin-right: 14px;
  width: 24px;
  text-align: center;
  display: inline-flex;
  align-items: center;
  justify-content: center;
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
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(16px) saturate(140%);
  border-bottom: 1px solid #dbe8f4;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 8px 22px -18px rgba(15, 23, 42, 0.35);
  position: relative;
  z-index: 1;

  h3 {
    font-size: 18px;
    font-weight: 600;
    color: #0f172a;
    margin: 0;
    letter-spacing: 0.3px;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-info {
  font-size: 14px;
  color: #334155;
  padding: 6px 14px;
  background: #f4f9ff;
  border-radius: 20px;
  border: 1px solid #dbe8f4;
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

