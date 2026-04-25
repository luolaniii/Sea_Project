/**
 * 枚举缓存工具
 * <p>
 * 提供枚举数据的缓存功能，避免重复请求后端接口
 * 支持获取所有枚举、获取指定枚举、根据code获取name等功能
 */

import http from './http';

/**
 * 枚举项接口
 */
interface EnumItem {
  /** 枚举编码 */
  code: string;
  /** 枚举名称 */
  name: string;
  /** 枚举描述（可选） */
  desc?: string;
}

/**
 * 枚举数据接口
 * <p>
 * key为枚举名称，value为枚举项数组
 */
interface EnumData {
  [key: string]: EnumItem[];
}

/**
 * 枚举缓存数据
 */
let enumCache: EnumData = {};

/**
 * 枚举缓存工具对象
 */
export const enumCacheUtil = {
  /**
   * 获取所有枚举数据
   * <p>
   * 如果缓存中已有数据则直接返回，否则从后端获取并缓存
   *
   * @returns 所有枚举数据
   */
  async getAll(): Promise<EnumData> {
    if (Object.keys(enumCache).length > 0) {
      return enumCache;
    }
    try {
      enumCache = await http.get('/common/enum/all');
      return enumCache;
    } catch (error) {
      console.error('获取枚举失败:', error);
      return {};
    }
  },

  /**
   * 获取指定名称的枚举列表
   *
   * @param enumName 枚举名称
   * @returns 枚举项数组
   */
  async get(enumName: string): Promise<EnumItem[]> {
    const all = await this.getAll();
    return all[enumName] || [];
  },

  /**
   * 根据枚举名称和编码获取枚举名称
   * <p>
   * 从缓存中查找指定枚举的指定编码对应的名称
   *
   * @param enumName 枚举名称
   * @param code 枚举编码
   * @returns 枚举名称，如果未找到则返回编码本身
   */
  getName(enumName: string, code: string): string {
    const items = enumCache[enumName] || [];
    const item = items.find((item) => item.code === code);
    return item?.name || code;
  },

  /**
   * 清空枚举缓存
   * <p>
   * 清空所有缓存的枚举数据，下次调用getAll时会重新从后端获取
   */
  clear() {
    enumCache = {};
  },
};

