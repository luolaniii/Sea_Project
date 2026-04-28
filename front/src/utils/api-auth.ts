/**
 * 认证相关API
 * <p>
 * 提供用户登录、登出等认证相关的接口调用
 */

import http from './http';
import type { UserInfo, LoginReq } from '@/types/auth';

/**
 * 注册请求参数
 */
export interface RegisterReq {
  username: string;
  password: string;
  email?: string;
  phone?: string;
  realName?: string;
}

/**
 * 认证API对象
 */
export const authApi = {
  /**
   * 用户登录
   */
  login: (req: LoginReq): Promise<UserInfo> => {
    return http.post('/common/auth/login', req);
  },

  /**
   * 用户登出
   */
  logout: (userInfo: UserInfo): Promise<void> => {
    return http.post('/common/auth/logout', userInfo);
  },

  /**
   * 用户注册
   */
  register: (req: RegisterReq): Promise<void> => {
    return http.post('/common/user/register', req);
  },
};

