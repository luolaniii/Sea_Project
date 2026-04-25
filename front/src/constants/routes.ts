/**
 * 路由路径常量
 * <p>
 * 统一管理应用中的所有路由路径，避免硬编码
 */

/**
 * 路由路径常量
 */
export const ROUTE_PATHS = {
  // 登录
  LOGIN: '/login',
  
  // 管理端
  ADMIN: {
    ROOT: '/admin',
    DASHBOARD: '/admin/dashboard',
    DATA_SOURCE: '/admin/data-source',
    OBSERVATION_DATA: '/admin/observation-data',
    NOAA_DATA_SYNC: '/admin/noaa-data-sync',
    SCENE: '/admin/scene',
    CHART_CONFIG: '/admin/chart-config',
    SYSTEM_CONFIG: '/admin/system-config',
    FORUM_POST: '/admin/forum-post',
  },
  
  // 用户端
  USER: {
    ROOT: '/user',
    HOME: '/user/home',
    SCENE_GALLERY: '/user/scene-gallery',
    CHART_GALLERY: '/user/chart-gallery',
    OCEAN_ANALYSIS: '/user/ocean-analysis',
    SCENE_VIEW: (id: string | number) => `/user/scene/${id}`,
    CHART_VIEW: (id: string | number) => `/user/chart/${id}`,
    FORUM_LIST: '/user/forum',
    FORUM_DETAIL: (id: string | number) => `/user/forum/${id}`,
    MY_POSTS: '/user/my-posts',
  },
} as const;

/**
 * 路由名称常量
 */
export const ROUTE_NAMES = {
  LOGIN: 'Login',
  ADMIN_DASHBOARD: 'AdminDashboard',
  ADMIN_DATA_SOURCE: 'DataSourcePage',
  ADMIN_OBSERVATION_DATA: 'ObservationDataPage',
  ADMIN_NOAA_DATA_SYNC: 'NoaaDataSyncPage',
  ADMIN_SCENE: 'ScenePage',
  ADMIN_CHART_CONFIG: 'ChartConfigPage',
  ADMIN_SYSTEM_CONFIG: 'SystemConfigPage',
  ADMIN_FORUM_POST: 'ForumPostPage',
  USER_HOME: 'UserHome',
  USER_SCENE_GALLERY: 'UserSceneGallery',
  USER_CHART_GALLERY: 'UserChartGallery',
  USER_OCEAN_ANALYSIS: 'UserOceanAnalysis',
  USER_SCENE_VIEW: 'UserSceneView',
  USER_CHART_VIEW: 'UserChartView',
  USER_FORUM_LIST: 'ForumList',
  USER_FORUM_DETAIL: 'ForumDetail',
  USER_MY_POSTS: 'MyPosts',
} as const;

