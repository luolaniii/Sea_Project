/**
 * 管理端API接口定义
 * <p>
 * 提供管理端访问各种数据管理功能的相关接口。
 */

import http from './http';
import { toPathId } from './path-id';
import { stringifyBodyLongIdFields } from './json-parse-ids';

/**
 * 分页请求参数接口
 */
export interface PageReq {
  pageNum: number;
  pageSize: number;
  [key: string]: any;
}

/**
 * 分页响应结果接口
 */
export interface PageBean<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

/**
 * 数据源接口
 */
export interface DataSource {
  id?: string | number;
  sourceName: string;
  sourceType: string;
  apiUrl: string;
  apiKey?: string;
  description?: string;
  status: number;
  configJson?: string;
  // NDBC自动采集相关字段
  stationId?: string;
  longitude?: number;
  latitude?: number;
  fileSuffixes?: string;
  autoSync?: number;
  syncIntervalMinutes?: number;
  lastSyncTime?: string;
}

/**
 * 数据源API对象
 */
export const dataSourceApi = {
  /**
   * 分页查询数据源
   */
  page: (req: PageReq): Promise<PageBean<DataSource>> => {
    return http.post('/admin/data-source/page', req);
  },
  /**
   * 新增数据源
   */
  add: (data: DataSource): Promise<DataSource> => {
    return http.post('/admin/data-source/add', stringifyBodyLongIdFields({ ...data } as Record<string, unknown>));
  },
  /**
   * 更新数据源
   */
  update: (data: DataSource): Promise<DataSource> => {
    return http.put('/admin/data-source', stringifyBodyLongIdFields({ ...data } as Record<string, unknown>));
  },
  /**
   * 删除数据源
   */
  delete: (id: string | number): Promise<void> => {
    return http.delete(`/admin/data-source/${toPathId(id)}`);
  },
  /**
   * 根据ID获取数据源详情
   */
  getById: (id: string | number): Promise<DataSource> => {
    return http.get(`/admin/data-source/${toPathId(id)}`);
  },
  /**
   * 获取所有数据源列表
   */
  list: (): Promise<DataSource[]> => {
    return http.get('/admin/data-source/list');
  },
  /**
   * 一键生成图表与 3D 场景（Module C）
   */
  autoGenerate: (id: string | number): Promise<{
    dataSourceId: number;
    dataSourceName: string;
    createdCharts: number;
    skippedCharts: number;
    createdScenes: number;
    typeCodes: string[];
  }> => {
    return http.post(`/admin/data-source/${toPathId(id)}/auto-generate`, undefined, {
      timeout: 180000,
    });
  },
  /**
   * 扫描 NDBC 目录发现所有站点（Module A）
   */
  discoverNdbcStations: (): Promise<Array<{
    stationId: string;
    availableSuffixes: string[];
    hasWaveData: boolean;
  }>> => {
    return http.get('/admin/noaa/discover-stations', {
      timeout: 120000,
    });
  },
  /**
   * 抓取某站点元数据（Module B）
   */
  ndbcStationMeta: (stationId: string): Promise<{
    stationId: string;
    stationName?: string;
    latitude?: number;
    longitude?: number;
    waterDepth?: string;
    owner?: string;
    description?: string;
  }> => {
    return http.get(`/admin/noaa/station-meta?stationId=${encodeURIComponent(stationId)}`, {
      timeout: 120000,
    });
  },
  /**
   * 批量创建 NDBC 数据源（Module A）
   */
  batchCreateNdbcSources: (
    stationIds: string[],
    options?: { autoGenerate?: boolean; syncData?: boolean }
  ): Promise<{
    created: number;
    skipped: number;
    totalCharts?: number;
    totalScenes?: number;
    details?: any[];
  }> => {
    return http.post('/admin/noaa/batch-create-sources', {
      stationIds,
      autoGenerate: options?.autoGenerate ?? true,
      syncData: options?.syncData ?? true,
    }, {
      // 批量创建+自动生成+同步属于长耗时流程，避免默认30秒超时
      timeout: 180000,
    });
  },
};

/**
 * 观测数据类型接口
 */
export interface ObservationDataType {
  id?: number;
  typeCode: string;
  typeName: string;
  unit?: string;
  dataFormat?: string;
  minValue?: number;
  maxValue?: number;
  description?: string;
}

/**
 * 观测数据类型API对象
 */
export const observationDataTypeApi = {
  /**
   * 获取所有观测数据类型列表
   */
  list: (): Promise<ObservationDataType[]> => {
    return http.get('/admin/observation-data-type/list');
  },
};

/**
 * 观测数据接口
 */
export interface ObservationData {
  id?: string | number;
  dataSourceId: string | number;
  dataTypeId: string | number;
  observationTime: string;
  dataValue: number;
  longitude: number;
  latitude: number;
  depth?: number;
  qualityFlag: string;
  sourceFileId?: string | number;
  apiDataId?: string;
  remark?: string;
  // 后端返回的关联数据
  dataSourceName?: string;
  dataTypeName?: string;
  dataTypeCode?: string;
  dataTypeUnit?: string;
  qualityFlagDesc?: string;
  dataSourceTypeDesc?: string;
}

/**
 * 观测数据API对象
 */
export const observationDataApi = {
  /**
   * 分页查询观测数据
   */
  page: (req: PageReq): Promise<PageBean<ObservationData>> => {
    return http.post('/admin/observation-data/page', req);
  },
  /**
   * 新增观测数据
   */
  add: (data: ObservationData): Promise<ObservationData> => {
    return http.post(
      '/admin/observation-data/add',
      stringifyBodyLongIdFields({ ...data } as Record<string, unknown>)
    );
  },
  /**
   * 更新观测数据
   */
  update: (data: ObservationData): Promise<ObservationData> => {
    return http.put(
      '/admin/observation-data',
      stringifyBodyLongIdFields({ ...data } as Record<string, unknown>)
    );
  },
  /**
   * 删除观测数据
   */
  delete: (id: string | number): Promise<void> => {
    return http.delete(`/admin/observation-data/${toPathId(id)}`);
  },
  /**
   * 根据ID获取观测数据详情
   */
  getById: (id: string | number): Promise<ObservationData> => {
    return http.get(`/admin/observation-data/${toPathId(id)}`);
  },
  /**
   * 获取所有观测数据列表
   */
  list: (): Promise<ObservationData[]> => {
    return http.get('/admin/observation-data/list');
  },
};

/**
 * 可视化场景接口
 */
export interface VisualizationScene {
  id?: string | number;
  sceneName: string;
  sceneType: string;
  userId?: string | number;
  isPublic: number;
  // 后端VO额外返回的枚举辅助字段（用于管理端展示 & 表单回显）
  sceneTypeDesc?: string;
  sceneTypeCode?: string;
  isPublicDesc?: string;
  isPublicCode?: number;
  configJson?: string;
  thumbnail?: string;
  description?: string;
  viewCount?: number;
}

/**
 * 可视化场景API对象
 */
export const visualizationSceneApi = {
  /**
   * 分页查询可视化场景
   */
  page: (req: PageReq): Promise<PageBean<VisualizationScene>> => {
    return http.post('/admin/visualization-scene/page', req);
  },
  /**
   * 新增可视化场景
   */
  add: (data: VisualizationScene): Promise<VisualizationScene> => {
    return http.post(
      '/admin/visualization-scene/add',
      stringifyBodyLongIdFields({ ...data } as Record<string, unknown>)
    );
  },
  /**
   * 更新可视化场景
   */
  update: (data: VisualizationScene): Promise<VisualizationScene> => {
    return http.put(
      '/admin/visualization-scene',
      stringifyBodyLongIdFields({ ...data } as Record<string, unknown>)
    );
  },
  /**
   * 删除可视化场景
   */
  delete: (id: string | number): Promise<void> => {
    return http.delete(`/admin/visualization-scene/${toPathId(id)}`);
  },
  /**
   * 根据ID获取可视化场景详情
   */
  getById: (id: string | number): Promise<VisualizationScene> => {
    return http.get(`/admin/visualization-scene/${toPathId(id)}`);
  },
  /**
   * 获取所有可视化场景列表
   */
  list: (): Promise<VisualizationScene[]> => {
    return http.get('/admin/visualization-scene/list');
  },
};

/**
 * 图表配置接口
 */
export interface ChartConfig {
  id?: string | number;
  chartName: string;
  chartType: string;
  userId?: string | number;
  dataQueryConfig?: string;
  echartsConfig?: string;
  isPublic: number;
  // 后端VO额外返回的枚举辅助字段（用于管理端展示 & 表单回显）
  chartTypeDesc?: string;
  chartTypeCode?: string;
  isPublicDesc?: string;
  isPublicCode?: number;
  description?: string;
}

/**
 * 图表配置API对象
 */
export const chartConfigApi = {
  /**
   * 分页查询图表配置
   */
  page: (req: PageReq): Promise<PageBean<ChartConfig>> => {
    return http.post('/admin/chart-config/page', req);
  },
  /**
   * 新增图表配置
   */
  add: (data: ChartConfig): Promise<ChartConfig> => {
    return http.post(
      '/admin/chart-config/add',
      stringifyBodyLongIdFields({ ...data } as Record<string, unknown>)
    );
  },
  /**
   * 更新图表配置
   */
  update: (data: ChartConfig): Promise<ChartConfig> => {
    return http.put(
      '/admin/chart-config',
      stringifyBodyLongIdFields({ ...data } as Record<string, unknown>)
    );
  },
  /**
   * 删除图表配置
   */
  delete: (id: string | number): Promise<void> => {
    return http.delete(`/admin/chart-config/${toPathId(id)}`);
  },
  /**
   * 根据ID获取图表配置详情
   */
  getById: (id: string | number): Promise<ChartConfig> => {
    return http.get(`/admin/chart-config/${toPathId(id)}`);
  },
  /**
   * 获取所有图表配置列表
   */
  list: (): Promise<ChartConfig[]> => {
    return http.get('/admin/chart-config/list');
  },
};

/**
 * 系统配置接口
 */
export interface SystemConfig {
  id?: string | number;
  configKey: string;
  configValue: string;
  configType: string;
  configGroup?: string;
  description?: string;
  isSystem?: boolean;
}

/**
 * 系统配置API对象
 */
export const systemConfigApi = {
  /**
   * 分页查询系统配置
   */
  page: (req: PageReq): Promise<PageBean<SystemConfig>> => {
    return http.post('/admin/system-config/page', req);
  },
  /**
   * 新增系统配置
   */
  add: (data: SystemConfig): Promise<SystemConfig> => {
    return http.post(
      '/admin/system-config/add',
      stringifyBodyLongIdFields({ ...data } as Record<string, unknown>)
    );
  },
  /**
   * 更新系统配置
   */
  update: (data: SystemConfig): Promise<SystemConfig> => {
    return http.put(
      '/admin/system-config',
      stringifyBodyLongIdFields({ ...data } as Record<string, unknown>)
    );
  },
  /**
   * 删除系统配置
   */
  delete: (id: string | number): Promise<void> => {
    return http.delete(`/admin/system-config/${toPathId(id)}`);
  },
  /**
   * 根据ID获取系统配置详情
   */
  getById: (id: string | number): Promise<SystemConfig> => {
    return http.get(`/admin/system-config/${toPathId(id)}`);
  },
  /**
   * 获取所有系统配置列表
   */
  list: (): Promise<SystemConfig[]> => {
    return http.get('/admin/system-config/list');
  },
};

/**
 * 论坛帖子接口（管理端）
 */
export interface ForumPost {
  id?: string | number;
  title: string;
  content: string;
  authorId?: string | number;
  authorName?: string;
  category?: string; // GENERAL-普通讨论, QUESTION-问题求助, SHARE-经验分享, NEWS-新闻资讯
  postType?: string; // TOPIC_DISCUSSION-主题讨论, DATA_ANALYSIS-数据分析
  analysisTarget?: string;
  reliabilityStatus?: number; // 0-未评选, 1-评估中/未通过, 2-已认证
  reliabilityScore?: number;
  evaluationCount?: number;
  tags?: string; // JSON数组格式
  viewCount?: number;
  likeCount?: number;
  commentCount?: number;
  isTop?: number; // 0-否, 1-是
  isEssence?: number; // 0-否, 1-是
  status?: number; // 0-草稿, 1-已发布, 2-已关闭
  allowComment?: number; // 0-否, 1-是
  createdTime?: string;
  updatedTime?: string;
}

/**
 * 论坛评论接口（管理端）
 */
export interface ForumComment {
  id?: string | number;
  postId: string | number;
  content: string;
  userId?: string | number;
  userName?: string;
  parentId?: string | number; // 0表示顶级评论
  replyToUserId?: string | number;
  replyToUserName?: string;
  likeCount?: number;
  status?: number; // 0-正常, 1-已删除, 2-已屏蔽
  createdTime?: string;
  updatedTime?: string;
  children?: ForumComment[]; // 子评论列表
}

/**
 * 论坛帖子API对象（管理端）
 */
export const forumPostApi = {
  /**
   * 分页查询论坛帖子
   */
  page: (req: PageReq): Promise<PageBean<ForumPost>> => {
    return http.post('/admin/forum-post/page', req);
  },
  /**
   * 获取帖子详情（管理端）
   */
  getById: (id: string | number): Promise<ForumPost> => {
    return http.get(`/admin/forum-post/${toPathId(id)}`);
  },
  /**
   * 获取帖子评论列表（管理端）
   */
  getComments: (postId: string | number): Promise<ForumComment[]> => {
    return http.get(`/admin/forum-comment/post/${toPathId(postId)}`);
  },
  /**
   * 设置帖子置顶状态
   */
  setTop: (id: string | number, isTop: number): Promise<void> => {
    return http.put(`/admin/forum-post/${toPathId(id)}/top?isTop=${isTop}`);
  },
  /**
   * 设置帖子精华状态
   */
  setEssence: (id: string | number, isEssence: number): Promise<void> => {
    return http.put(`/admin/forum-post/${toPathId(id)}/essence?isEssence=${isEssence}`);
  },
  /**
   * 删除帖子
   */
  delete: (id: string | number): Promise<void> => {
    return http.delete(`/admin/forum-post/${toPathId(id)}`);
  },
};

/**
 * NOAA 数据文件上传导入结果接口
 */
export interface NoaaUploadResult {
  totalCrawled: number;
  savedCount: number;
  duplicateCount: number;
  qualityStats?: {
    goodCount: number;
    questionableCount: number;
    badCount: number;
    totalCount: number;
  };
}

/**
 * NOAA 数据文件上传 API 对象
 */
export const noaaDataUploadApi = {
  /**
   * 上传 NDBC 文本文件并导入观测数据
   */
  uploadNdbcText: (options: {
    dataSourceId: string | number;
    variable: string;
    dataTypeCode: string;
    file: File;
  }): Promise<NoaaUploadResult> => {
    const formData = new FormData();
    formData.append('dataSourceId', String(options.dataSourceId));
    formData.append('variable', options.variable);
    formData.append('dataTypeCode', options.dataTypeCode);
    formData.append('file', options.file);

    // 让浏览器自动设置 multipart/form-data 边界
    return http.post('/admin/noaa/upload-ndbc-text', formData, {
      timeout: 120000,
    });
  },
};

/**
 * NOAA同步结果接口
 */
export interface NoaaSyncResult {
  dataSourceId?: string | number;
  stationId?: string;
  sourceName?: string;
  status: string;
  message?: string;
  totalParsed?: number;
  totalSaved?: number;
  totalDuplicate?: number;
  costMs?: number;
  savePerSecond?: number;
  parsePerSecond?: number;
  duplicateRate?: number;
  fileResults?: Array<{
    suffix: string;
    status: string;
    message?: string;
    errorType?: string;
    fetchAttempts?: number;
    fetchCostMs?: number;
    costMs?: number;
    parsed?: number;
    saved?: number;
    duplicate?: number;
    qualityStats?: {
      goodCount: number;
      questionableCount: number;
      badCount: number;
    };
  }>;
  syncTime?: string;
}

/**
 * NOAA变量映射接口
 */
export interface NoaaVariableMapping {
  typeId: number;
  typeCode: string;
  typeName: string;
  noaaVariableName: string;
  unit?: string;
}

/**
 * NOAA 自动采集 API 对象
 */
export const noaaAutoSyncApi = {
  /**
   * 手动触发单个站点同步
   */
  syncStation: (dataSourceId: string | number): Promise<NoaaSyncResult> => {
    return http.post(`/admin/noaa/sync/${toPathId(dataSourceId)}`, undefined, {
      timeout: 120000,
    });
  },

  /**
   * 同步所有启用自动同步的站点
   */
  syncAll: (): Promise<{
    totalStations: number;
    successCount: number;
    failCount: number;
    details: NoaaSyncResult[];
    syncTime: string;
  }> => {
    return http.post('/admin/noaa/sync-all', undefined, {
      timeout: 300000,
    });
  },

  /**
   * 预览远程NDBC数据
   */
  previewRemoteData: (dataSourceId: string | number): Promise<{
    stationId: string;
    files: Array<{
      suffix: string;
      available: boolean;
      fetchAttempts?: number;
      fetchCostMs?: number;
      errorType?: string;
      message?: string;
      totalRecords?: number;
      variables?: string[];
    }>;
  }> => {
    return http.get(`/admin/noaa/preview/${toPathId(dataSourceId)}`, {
      timeout: 120000,
    });
  },

  /**
   * 查询站点可用的文件类型
   */
  getAvailableFileSuffixes: (stationId: string): Promise<string[]> => {
    return http.get(`/admin/noaa/available-files/${stationId}`, {
      timeout: 60000,
    });
  },

  /**
   * 获取数据类型与NOAA变量的映射关系
   */
  getVariableMapping: (): Promise<NoaaVariableMapping[]> => {
    return http.get('/admin/noaa/variable-mapping');
  },
};

