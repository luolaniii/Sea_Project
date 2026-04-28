/**
 * 认证状态管理Store
 * <p>
 * 使用Pinia管理用户认证状态，包括：
 * - 用户信息
 * - Token管理
 * - 登录/登出功能
 * - 状态持久化（localStorage）
 */

import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { authApi } from '@/utils/api-auth';
import type { UserInfo, LoginReq } from '@/types/auth';
import { message } from '@/utils/message';
import router from '@/router';
import { ROUTE_PATHS } from '@/constants/routes';

/**
 * 认证Store
 */
export const useAuthStore = defineStore('auth', () => {
  /**
   * 用户信息
   */
  const userInfo = ref<UserInfo | null>(null);

  /**
   * 认证Token
   */
  const token = ref<string>('');

  /**
   * 从localStorage恢复认证状态
   * <p>
   * 应用启动时自动调用，恢复用户的登录状态
   */
  const initAuth = () => {
    const savedToken = localStorage.getItem('token');
    const savedUserInfo = localStorage.getItem('userInfo');
    if (savedToken && savedUserInfo) {
      token.value = savedToken;
      userInfo.value = JSON.parse(savedUserInfo);
    }
  };

  /**
   * 用户登录
   * <p>
   * 调用登录API，成功后保存用户信息和Token到localStorage
   *
   * @param req 登录请求参数（用户名和密码）
   * @returns 用户信息（包含Token）
   */
  const login = async (req: LoginReq) => {
    try {
      const data = await authApi.login(req);
      userInfo.value = data;
      token.value = data.token;
      localStorage.setItem('token', data.token);
      localStorage.setItem('userInfo', JSON.stringify(data));
      message.success('登录成功');
      return data;
    } catch (error: any) {
      // 错误信息已在http拦截器中统一处理，这里只抛出错误
      throw error;
    }
  };

  /**
   * 用户登出
   * <p>
   * 清除用户信息和Token，并跳转到登录页
   */
  const logout = async () => {
    if (userInfo.value) {
      try {
        await authApi.logout(userInfo.value);
      } catch (error) {
        console.error('登出失败:', error);
      }
    }
    userInfo.value = null;
    token.value = '';
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
    router.push(ROUTE_PATHS.LOGIN);
  };

  /**
   * 是否已登录（计算属性）
   */
  const isLoggedIn = computed(() => !!token.value);

  /**
   * 是否是管理员（计算属性）
   */
  const isAdmin = computed(() => userInfo.value?.role?.toUpperCase() === 'ADMIN');

  /**
   * 更新当前用户的头像 URL（同步更新 localStorage 持久化）
   */
  const updateAvatar = (avatarUrl: string) => {
    if (!userInfo.value) return;
    userInfo.value = { ...userInfo.value, avatar: avatarUrl };
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value));
  };

  /**
   * 用最新的服务器数据刷新用户资料（不更换 token）
   */
  const refreshUser = (info: Partial<UserInfo>) => {
    if (!userInfo.value) return;
    userInfo.value = {
      ...userInfo.value,
      ...info,
      token: userInfo.value.token, // token 始终以本地存储为准
    } as UserInfo;
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value));
  };

  // 应用启动时初始化认证状态
  initAuth();

  // 导出Store的公共接口

  return {
    userInfo,
    token,
    login,
    logout,
    isLoggedIn,
    isAdmin,
    updateAvatar,
    refreshUser,
  };
});

