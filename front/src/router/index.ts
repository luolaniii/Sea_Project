/**
 * 路由配置
 * <p>
 * 定义应用程序的所有路由规则，包括：
 * - 登录页面
 * - 管理端路由（需要管理员权限）
 * - 用户端路由（需要登录）
 * - 路由守卫（权限验证）
 */

import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/store/auth';
import { message } from '@/utils/message';
import { ROUTE_PATHS, ROUTE_NAMES } from '@/constants/routes';

/**
 * 创建路由实例
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: ROUTE_PATHS.LOGIN,
      name: ROUTE_NAMES.LOGIN,
      component: () => import('@/views/common/Login.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: ROUTE_PATHS.ADMIN.ROOT,
      component: () => import('@/views/admin/AdminLayout.vue'),
      meta: { requiresAuth: true, role: 'ADMIN' },
      children: [
        {
          path: '',
          redirect: ROUTE_PATHS.ADMIN.DASHBOARD,
        },
        {
          path: 'dashboard',
          name: ROUTE_NAMES.ADMIN_DASHBOARD,
          component: () => import('@/views/admin/AdminDashboard.vue'),
        },
        {
          path: 'data-source',
          name: ROUTE_NAMES.ADMIN_DATA_SOURCE,
          component: () => import('@/views/admin/DataSourcePage.vue'),
        },
        {
          path: 'observation-data',
          name: ROUTE_NAMES.ADMIN_OBSERVATION_DATA,
          component: () => import('@/views/admin/ObservationDataPage.vue'),
        },
        {
          path: 'noaa-data-sync',
          name: ROUTE_NAMES.ADMIN_NOAA_DATA_SYNC,
          component: () => import('@/views/admin/NoaaDataSyncPage.vue'),
        },
        {
          path: 'scene',
          name: ROUTE_NAMES.ADMIN_SCENE,
          component: () => import('@/views/admin/ScenePage.vue'),
        },
        {
          path: 'chart-config',
          name: ROUTE_NAMES.ADMIN_CHART_CONFIG,
          component: () => import('@/views/admin/ChartConfigPage.vue'),
        },
        {
          path: 'system-config',
          name: ROUTE_NAMES.ADMIN_SYSTEM_CONFIG,
          component: () => import('@/views/admin/SystemConfigPage.vue'),
        },
        {
          path: 'forum-post',
          name: ROUTE_NAMES.ADMIN_FORUM_POST,
          component: () => import('@/views/admin/ForumPostPage.vue'),
        },
      ],
    },
    {
      path: ROUTE_PATHS.USER.ROOT,
      component: () => import('@/views/user/UserLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          redirect: ROUTE_PATHS.USER.HOME,
        },
        {
          path: 'home',
          name: ROUTE_NAMES.USER_HOME,
          component: () => import('@/views/user/UserHome.vue'),
        },
        {
          path: 'scene-gallery',
          name: ROUTE_NAMES.USER_SCENE_GALLERY,
          component: () => import('@/views/user/UserSceneGallery.vue'),
        },
        {
          path: 'chart-gallery',
          name: ROUTE_NAMES.USER_CHART_GALLERY,
          component: () => import('@/views/user/UserChartGallery.vue'),
        },
        {
          path: 'ocean-analysis',
          name: ROUTE_NAMES.USER_OCEAN_ANALYSIS,
          component: () => import('@/views/user/UserOceanAnalysis.vue'),
        },
        {
          path: 'ocean-comparison',
          name: 'UserOceanComparison',
          component: () => import('@/views/user/UserOceanComparison.vue'),
        },
        {
          path: 'station-map',
          name: 'UserStationMap',
          component: () => import('@/views/user/UserStationMap.vue'),
        },
        {
          path: 'scene/:id',
          name: ROUTE_NAMES.USER_SCENE_VIEW,
          component: () => import('@/views/user/UserSceneView.vue'),
        },
        {
          path: 'chart/:id',
          name: ROUTE_NAMES.USER_CHART_VIEW,
          component: () => import('@/views/user/UserChartView.vue'),
        },
        {
          path: 'forum',
          name: ROUTE_NAMES.USER_FORUM_LIST,
          component: () => import('@/views/user/ForumList.vue'),
        },
        {
          path: 'forum/:id',
          name: ROUTE_NAMES.USER_FORUM_DETAIL,
          component: () => import('@/views/user/ForumDetail.vue'),
        },
        {
          path: 'my-posts',
          name: ROUTE_NAMES.USER_MY_POSTS,
          component: () => import('@/views/user/MyPosts.vue'),
        },
      ],
    },
    {
      path: '/',
      redirect: ROUTE_PATHS.LOGIN,
    },
  ],
});

/**
 * 路由守卫
 * <p>
 * 在路由跳转前进行权限验证：
 * 1. 检查是否需要登录（requiresAuth）
 * 2. 检查是否需要特定角色（role）
 * 3. 已登录用户访问登录页时重定向到对应首页
 */
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();

  // 需要登录但未登录，跳转到登录页
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    next(ROUTE_PATHS.LOGIN);
  }
  // 需要特定角色但用户角色不匹配，拒绝访问
  else if (to.meta.role && authStore.userInfo?.role?.toUpperCase() !== to.meta.role) {
    message.error('无权限访问');
    next(from.path || ROUTE_PATHS.LOGIN);
  }
  // 已登录用户访问登录页，重定向到对应首页
  else if (to.path === ROUTE_PATHS.LOGIN && authStore.isLoggedIn) {
    if (authStore.isAdmin) {
      next(ROUTE_PATHS.ADMIN.DASHBOARD);
    } else {
      next(ROUTE_PATHS.USER.HOME);
    }
  }
  // 其他情况正常跳转
  else {
    next();
  }
});

export default router;

