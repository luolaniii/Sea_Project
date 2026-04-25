/**
 * 认证相关类型定义
 */

/**
 * 用户信息接口
 */
export interface UserInfo {
  /** 用户ID（后端 Long 多为 JSON 字符串） */
  id: string | number;
  /** 用户名 */
  username: string;
  /** 认证Token */
  token: string;
  /** 用户角色 */
  role: 'admin' | 'user' | 'ADMIN' | 'USER';
}

/**
 * 登录请求接口
 */
export interface LoginReq {
  /** 用户名 */
  username: string;
  /** 密码 */
  password: string;
}

