/**
 * 认证相关API
 * <p>
 * 提供用户登录、登出等认证相关的接口调用
 */

import http from './http';
import type { UserInfo, LoginReq } from '@/types/auth';

/**
 * 认证API对象
 */
export const authApi = {
  /**
   * 用户登录
   * <p>
   * 发送登录请求，验证用户名和密码
   *
   * @param req 登录请求参数，包含用户名和密码
   * @returns 用户信息（包含Token）
   */
  login: (req: LoginReq): Promise<UserInfo> => {
    return http.post('/common/auth/login', req);
  },

  /**
   * 用户登出
   * <p>
   * 发送登出请求，清除服务端的Token缓存
   *
   * @param userInfo 用户信息，包含Token和用户ID
   * @returns Promise<void>
   */
  logout: (userInfo: UserInfo): Promise<void> => {
    return http.post('/common/auth/logout', userInfo);
  },
};

