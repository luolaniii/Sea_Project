/**
 * 用户端API接口定义
 * <p>
 * 提供用户端访问公开场景和图表的相关接口。
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
 * 可视化场景接口
 */
export interface VisualizationScene {
  /** 后端 Jackson 将 Long 序列化为字符串，避免前端精度丢失 */
  id?: string | number;
  sceneName: string;
  sceneType: string;
  userId?: string | number;
  isPublic: number;
  configJson?: string;
  thumbnail?: string;
  description?: string;
  viewCount?: number;
}

export interface DataSourceItem {
  id?: string | number;
  sourceName?: string;
  stationId?: string;
  status?: number;
}

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
  description?: string;
}

/**
 * 观测数据接口
 */
export interface ObservationData {
  id?: string | number;
  dataSourceId?: string | number;
  dataTypeId?: string | number;
  observationTime?: string;
  dataValue?: number;
  longitude?: number;
  latitude?: number;
  depth?: number;
  qualityFlag?: string;
  // 后端可能返回的关联数据（用于配置驱动渲染）
  dataSourceName?: string;
  dataTypeName?: string;
  dataTypeCode?: string;
  dataTypeUnit?: string;
  qualityFlagDesc?: string;
  dataSourceTypeDesc?: string;
}

/**
 * 数据查询请求参数接口
 */
export interface DataQueryReq {
  pageNum?: number;
  pageSize?: number;
  dataSourceId?: string | number;
  dataTypeId?: string | number;
  typeCodes?: string[];
  startTime?: string;
  endTime?: string;
  minLongitude?: number;
  maxLongitude?: number;
  minLatitude?: number;
  maxLatitude?: number;
}

export interface OceanAnalysisReq {
  dataSourceId: string | number;
  startTime?: string;
  endTime?: string;
  historyHours?: number;
}

/** Module I：多站点对比请求 */
export interface OceanComparisonReq {
  dataSourceIds: Array<string | number>;
  startTime?: string;
  endTime?: string;
  historyHours?: number;
}

/** Module I：多站点对比结果 */
export interface OceanComparisonResult {
  stations?: Array<{
    dataSourceId?: string | number;
    sourceName?: string;
    stationId?: string;
    longitude?: number;
    latitude?: number;
    temperature?: number;
    salinity?: number;
    seaLevel?: number;
    waveHeight?: number;
    windSpeed?: number;
    stabilityIndex?: number;
    abnormal?: boolean;
  }>;
  metrics?: {
    temperature?: MetricStat;
    waveHeight?: MetricStat;
    windSpeed?: MetricStat;
    stabilityIndex?: MetricStat;
  };
  anomalies?: Array<{
    dataSourceId?: string | number;
    sourceName?: string;
    metric?: string;
    value?: number;
    groupMean?: number;
    zScore?: number;
    severity?: string;
    description?: string;
  }>;
  confidence?: string;
  degradeReason?: string[];
}

export interface MetricStat {
  min?: number;
  max?: number;
  mean?: number;
  range?: number;
  stddev?: number;
  minStationId?: string | number;
  maxStationId?: string | number;
}

export interface OceanAbnormalResult {
  level1Alarm?: boolean;
  level2Alarm?: boolean;
  ref15mStatus?: string;
  confidence?: string;
  degradeReason?: string[];
}

export interface OceanStabilityResult {
  stabilityIndex?: number;
  level?: string;
  confidence?: string;
  degradeReason?: string[];
}

export interface OceanComfortResult {
  score?: number;
  level?: string;
  suggestion?: string;
  confidence?: string;
  degradeReason?: string[];
}

export interface OceanTrendResult {
  temperaturePredict?: number;
  temperatureTrend?: string;
  seaLevelPredict?: number;
  seaLevelTrend?: string;
  confidence?: string;
  degradeReason?: string[];
}

export interface OceanCompositeResult {
  currentTime?: string;
  temperature?: number;
  salinity?: number;
  seaLevel?: number;
  waveHeight?: number;
  windSpeed?: number;
  abnormal?: OceanAbnormalResult;
  stability?: OceanStabilityResult;
  comfort?: OceanComfortResult;
  trend?: OceanTrendResult;
}

/**
 * 论坛帖子接口
 */
export interface ForumPost {
  id?: string | number;
  title: string;
  content: string;
  authorId?: string | number;
  authorName?: string;
  category?: string; // GENERAL-普通讨论, QUESTION-问题求助, SHARE-经验分享, NEWS-新闻资讯
  postType?: string; // TOPIC_DISCUSSION-主题讨论, DATA_ANALYSIS-数据分析
  analysisTarget?: string; // 分析目标
  reliabilityStatus?: number; // 0-未评选, 1-评选中, 2-已认证
  reliabilityScore?: number; // 可靠性评分
  evaluationCount?: number; // 专家评估次数
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
 * 论坛评论接口
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

export interface ExpertAnswer {
  id?: string | number;
  postId?: string | number;
  commentId?: string | number;
  expertId?: string | number;
  expertName?: string;
  expertTitle?: string;
  answerContent: string;
  answerType?: string;
  likeCount?: number;
  isAccepted?: number;
  status?: number;
  createdTime?: string;
}

export interface ForumAttachment {
  id?: string | number;
  postId: string | number;
  uploaderId?: string | number;
  fileName: string;
  fileUrl: string;
  fileType?: string;
  fileSize?: number;
  status?: number;
  createdTime?: string;
}

export interface ForumUploadResult {
  fileName: string;
  fileUrl: string;
  fileSize?: number;
}

export interface ForumEvaluation {
  id?: string | number;
  postId: string | number;
  evaluatorId?: string | number;
  evaluatorName?: string;
  isExpert?: number;
  scoreAccuracy?: number;
  scoreRisk?: number;
  scoreReasoning?: number;
  totalScore?: number;
  comment?: string;
  status?: number;
  createdTime?: string;
}

/**
 * 用户端API对象
 * <p>
 * 提供用户端访问公开场景和图表的相关接口方法。
 */
export const userApi = {
  /**
   * 获取公开的场景列表（分页）
   *
   * @param req 分页查询参数
   * @returns 场景列表分页结果
   */
  getPublicScenes: (req: PageReq): Promise<PageBean<VisualizationScene>> => {
    return http.post('/user/visualization/scene/page', req);
  },

  getUserDataSourceList: (): Promise<DataSourceItem[]> => {
    return http.get('/user/data-source/list');
  },

  /**
   * 用户首页统计：场景/图表/观测数据条数
   */
  getUserStats: (): Promise<{ sceneCount: number; chartCount: number; dataCount: number }> => {
    return http.get('/user/data-source/stats');
  },

  /**
   * Module H: 获取地图视图所需的站点信息（含关联图表/场景）
   */
  getMapStations: (): Promise<Array<{
    id: number | string;
    stationId: string;
    name: string;
    longitude: number;
    latitude: number;
    description?: string;
    fileSuffixes?: string;
    chartCount: number;
    sceneCount: number;
    charts: Array<{ id: number | string; name: string }>;
    scenes: Array<{ id: number | string; name: string }>;
  }>> => {
    return http.get('/user/data-source/map-stations');
  },

  /**
   * 根据ID获取场景详情
   *
   * @param id 场景ID
   * @returns 场景详情
   */
  getSceneById: (id: string | number): Promise<VisualizationScene> => {
    return http.get(`/user/visualization/scene/${toPathId(id)}`);
  },

  /**
   * 获取场景关联的观测数据
   *
   * @param sceneId 场景ID
   * @param req 数据查询参数
   * @returns 观测数据分页结果
   */
  getSceneData: (sceneId: string | number, req: DataQueryReq): Promise<PageBean<ObservationData>> => {
    const body = stringifyBodyLongIdFields({
      pageNum: req.pageNum || 1,
      pageSize: req.pageSize || 1000,
      ...req,
    } as Record<string, unknown>);
    return http.post(`/user/visualization/scene/${toPathId(sceneId)}/data`, body);
  },

  /**
   * 获取公开的图表列表（分页）
   *
   * @param req 分页查询参数
   * @returns 图表列表分页结果
   */
  getPublicCharts: (req: PageReq): Promise<PageBean<ChartConfig>> => {
    return http.post('/user/chart/page', req);
  },

  /**
   * 根据ID获取图表详情
   *
   * @param id 图表ID
   * @returns 图表详情
   */
  getChartById: (id: string | number): Promise<ChartConfig> => {
    return http.get(`/user/chart/${toPathId(id)}`);
  },

  /**
   * 获取图表关联的观测数据
   *
   * @param chartId 图表ID
   * @param req 数据查询参数
   * @returns 观测数据分页结果
   */
  getChartData: (chartId: string | number, req: DataQueryReq): Promise<PageBean<ObservationData>> => {
    const body = stringifyBodyLongIdFields({
      pageNum: req.pageNum || 1,
      pageSize: req.pageSize || 1000,
      ...req,
    } as Record<string, unknown>);
    return http.post(`/user/chart/${toPathId(chartId)}/data`, body);
  },

  analyzeOceanAbnormal: (req: OceanAnalysisReq): Promise<OceanAbnormalResult> => {
    const body = stringifyBodyLongIdFields({ ...req } as Record<string, unknown>);
    return http.post('/user/ocean-analysis/abnormal', body);
  },

  analyzeOceanStability: (req: OceanAnalysisReq): Promise<OceanStabilityResult> => {
    const body = stringifyBodyLongIdFields({ ...req } as Record<string, unknown>);
    return http.post('/user/ocean-analysis/stability', body);
  },

  analyzeOceanComfort: (req: OceanAnalysisReq): Promise<OceanComfortResult> => {
    const body = stringifyBodyLongIdFields({ ...req } as Record<string, unknown>);
    return http.post('/user/ocean-analysis/comfort', body);
  },

  analyzeOceanTrend: (req: OceanAnalysisReq): Promise<OceanTrendResult> => {
    const body = stringifyBodyLongIdFields({ ...req } as Record<string, unknown>);
    return http.post('/user/ocean-analysis/trend', body);
  },

  analyzeOceanComposite: (req: OceanAnalysisReq): Promise<OceanCompositeResult> => {
    const body = stringifyBodyLongIdFields({ ...req } as Record<string, unknown>);
    return http.post('/user/ocean-analysis/composite', body);
  },

  /** Module I：多站点对比 */
  analyzeOceanCompare: (req: OceanComparisonReq): Promise<OceanComparisonResult> => {
    const body = stringifyBodyLongIdFields({ ...req } as Record<string, unknown>);
    return http.post('/user/ocean-analysis/compare', body);
  },

  /** AI 辅助决策：基于站点 composite 上下文 + DeepSeek 给出未来海况预测 */
  aiForecast: (req: OceanAnalysisReq): Promise<{
    provider?: string;
    model?: string;
    content?: string;
    error?: string;
    elapsedMs?: number;
    promptTokens?: number;
    completionTokens?: number;
    totalTokens?: number;
    context?: Record<string, unknown>;
  }> => {
    const body = stringifyBodyLongIdFields({ ...req } as Record<string, unknown>);
    return http.post('/user/ai-forecast/forecast', body);
  },

  // ========== 论坛相关接口 ==========

  /**
   * 分页查询帖子列表
   *
   * @param page 页码
   * @param size 每页数量
   * @param category 分类（可选）
   * @param keyword 关键词（可选）
   * @returns 分页结果
   */
  getForumPosts: (params: {
    page?: number;
    size?: number;
    category?: string;
    keyword?: string;
    postType?: string;
    reliabilityStatus?: number;
  }): Promise<PageBean<ForumPost>> => {
    return http.get('/user/forum-post/page', { params });
  },

  /**
   * 查询帖子详情
   *
   * @param id 帖子ID
   * @returns 帖子详情
   */
  getForumPostById: (id: string | number): Promise<ForumPost> => {
    return http.get(`/user/forum-post/${toPathId(id)}`);
  },

  /**
   * 发布帖子
   *
   * @param post 帖子信息
   * @returns 保存后的帖子
   */
  publishPost: (post: ForumPost): Promise<ForumPost> => {
    return http.post('/user/forum-post', stringifyBodyLongIdFields({ ...post } as Record<string, unknown>));
  },

  /**
   * 更新帖子
   *
   * @param post 帖子信息
   * @returns 更新结果
   */
  updatePost: (post: ForumPost): Promise<ForumPost> => {
    return http.put('/user/forum-post', stringifyBodyLongIdFields({ ...post } as Record<string, unknown>));
  },

  /**
   * 删除帖子
   *
   * @param id 帖子ID
   * @returns 删除结果
   */
  deletePost: (id: string | number): Promise<void> => {
    return http.delete(`/user/forum-post/${toPathId(id)}`);
  },

  /**
   * 点赞帖子
   *
   * @param id 帖子ID
   * @returns 操作结果
   */
  likePost: (id: string | number): Promise<void> => {
    return http.post(`/user/forum-post/${toPathId(id)}/like`);
  },

  /**
   * 取消点赞帖子
   *
   * @param id 帖子ID
   * @returns 操作结果
   */
  unlikePost: (id: string | number): Promise<void> => {
    return http.post(`/user/forum-post/${toPathId(id)}/unlike`);
  },

  /**
   * 获取热门帖子
   *
   * @param limit 数量限制（默认10）
   * @returns 热门帖子列表
   */
  getHotPosts: (limit?: number): Promise<ForumPost[]> => {
    return http.get('/user/forum-post/hot', { params: { limit } });
  },

  /**
   * 获取置顶帖子
   *
   * @returns 置顶帖子列表
   */
  getTopPosts: (): Promise<ForumPost[]> => {
    return http.get('/user/forum-post/top');
  },

  /**
   * 分页查询当前用户的帖子列表
   *
   * @param page 页码
   * @param size 每页数量
   * @param category 分类（可选）
   * @param keyword 关键词（可选）
   * @returns 分页结果
   */
  getMyPosts: (params: {
    page?: number;
    size?: number;
    category?: string;
    keyword?: string;
    postType?: string;
    reliabilityStatus?: number;
  }): Promise<PageBean<ForumPost>> => {
    return http.get('/user/forum-post/my', { params });
  },

  /**
   * 根据帖子ID查询评论列表
   *
   * @param postId 帖子ID
   * @returns 评论列表（树形结构）
   */
  getCommentsByPostId: (postId: string | number): Promise<ForumComment[]> => {
    return http.get(`/user/forum-comment/post/${toPathId(postId)}`);
  },

  /**
   * 添加评论
   *
   * @param comment 评论信息
   * @returns 保存后的评论
   */
  addComment: (comment: ForumComment): Promise<ForumComment> => {
    return http.post('/user/forum-comment', stringifyBodyLongIdFields({ ...comment } as Record<string, unknown>));
  },

  /**
   * 更新评论
   *
   * @param comment 评论信息
   * @returns 更新结果
   */
  updateComment: (comment: ForumComment): Promise<ForumComment> => {
    return http.put('/user/forum-comment', stringifyBodyLongIdFields({ ...comment } as Record<string, unknown>));
  },

  /**
   * 删除评论
   *
   * @param id 评论ID
   * @returns 删除结果
   */
  deleteComment: (id: string | number): Promise<void> => {
    return http.delete(`/user/forum-comment/${toPathId(id)}`);
  },

  /**
   * 点赞评论
   *
   * @param id 评论ID
   * @returns 操作结果
   */
  likeComment: (id: string | number): Promise<void> => {
    return http.post(`/user/forum-comment/${toPathId(id)}/like`);
  },

  /**
   * 取消点赞评论
   *
   * @param id 评论ID
   * @returns 操作结果
   */
  unlikeComment: (id: string | number): Promise<void> => {
    return http.post(`/user/forum-comment/${toPathId(id)}/unlike`);
  },

  // ========== 论坛增强：附件/评选/专家评估 ==========
  addForumAttachment: (payload: ForumAttachment): Promise<ForumAttachment> => {
    return http.post('/user/forum-post-attachment', stringifyBodyLongIdFields({ ...payload } as Record<string, unknown>));
  },

  uploadForumAttachmentFile: (file: File): Promise<ForumUploadResult> => {
    const formData = new FormData();
    formData.append('file', file);
    return http.post('/user/forum-post-attachment/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  getForumAttachments: (postId: string | number): Promise<ForumAttachment[]> => {
    return http.get(`/user/forum-post-attachment/post/${toPathId(postId)}`);
  },

  deleteForumAttachment: (id: string | number): Promise<void> => {
    return http.delete(`/user/forum-post-attachment/${toPathId(id)}`);
  },

  voteForumPost: (postId: string | number, voteType: 1 | -1): Promise<void> => {
    return http.post(`/user/forum-post-vote/post/${toPathId(postId)}`, null, { params: { voteType } });
  },

  getForumVoteSummary: (postId: string | number): Promise<{ postId: string | number; supportCount: number; opposeCount: number; totalCount: number }> => {
    return http.get(`/user/forum-post-vote/post/${toPathId(postId)}/summary`);
  },

  submitForumEvaluation: (payload: ForumEvaluation): Promise<ForumEvaluation> => {
    return http.post('/user/forum-post-evaluation', stringifyBodyLongIdFields({ ...payload } as Record<string, unknown>));
  },

  getForumEvaluations: (postId: string | number): Promise<ForumEvaluation[]> => {
    return http.get(`/user/forum-post-evaluation/post/${toPathId(postId)}`);
  },

  getExpertAnswersByPostId: (postId: string | number): Promise<ExpertAnswer[]> => {
    return http.get(`/user/expert-answer/post/${toPathId(postId)}`);
  },

  addExpertAnswer: (payload: ExpertAnswer): Promise<ExpertAnswer> => {
    return http.post('/user/expert-answer', stringifyBodyLongIdFields({ ...payload } as Record<string, unknown>));
  },
};
