import { quoteJsonLongIntegerFields } from './json-parse-ids';

export type RelativeTimeConfig = {
  mode?: 'range';
  /**
   * 当 mode=range 且未显式提供 startTime/endTime 时，默认查询最近 N 小时
   */
  defaultHours?: number;
  /**
   * 可选：显式指定最近 N 小时（优先于 defaultHours）
   */
  hours?: number;
};

export type DataQueryConfig = {
  /** 可能为 Snowflake，保持 string 可避免超过 Number.MAX_SAFE_INTEGER 时丢精度 */
  dataSourceId?: string | number;
  dataTypeId?: string | number;
  /**
   * 多指标查询：按 typeCode 列表查询（后端会转换为 dataTypeIds）
   */
  typeCodes?: string[];
  startTime?: string;
  endTime?: string;
  minLongitude?: number;
  maxLongitude?: number;
  minLatitude?: number;
  maxLatitude?: number;
  pageSize?: number;
  time?: RelativeTimeConfig;
};

export const parseTypeCodes = (input: unknown): string[] | undefined => {
  if (Array.isArray(input)) {
    const list = input
      .map((x) => (typeof x === 'string' ? x.trim() : ''))
      .filter((x) => x.length > 0);
    return list.length ? list : undefined;
  }
  if (typeof input === 'string') {
    const list = input
      .split(/[\s,;|]+/g)
      .map((x) => x.trim())
      .filter((x) => x.length > 0);
    return list.length ? list : undefined;
  }
  return undefined;
};

const asNumber = (v: any): number | undefined => {
  if (v === undefined || v === null || v === '') return undefined;
  const n = Number(v);
  return Number.isFinite(n) ? n : undefined;
};

const firstDefined = (...values: any[]) => values.find((v) => v !== undefined && v !== null && v !== '');

const mergeTypeCodes = (...values: unknown[]): string[] | undefined => {
  const seen = new Set<string>();
  const out: string[] = [];
  values.forEach((value) => {
    const list = parseTypeCodes(value);
    if (!list) return;
    list.forEach((code) => {
      const key = code.trim();
      if (!key || seen.has(key)) return;
      seen.add(key);
      out.push(key);
    });
  });
  return out.length ? out : undefined;
};

/** 数据源/类型等主键：JSON 中建议用字符串存 Snowflake；数值仅在安全整数范围内转 number */
const asIdField = (v: any): string | number | undefined => {
  if (v === undefined || v === null || v === '') return undefined;
  if (typeof v === 'string') {
    const t = v.trim();
    if (!t) return undefined;
    if (/^-?\d+$/.test(t)) return t;
    return undefined;
  }
  if (typeof v === 'number' && Number.isFinite(v)) {
    if (Number.isInteger(v) && Math.abs(v) <= Number.MAX_SAFE_INTEGER) return v;
    return String(Math.trunc(v));
  }
  return undefined;
};

export const parseDataQueryConfigJson = (configJson?: string): DataQueryConfig | null => {
  if (!configJson) return null;
  try {
    const parsed = JSON.parse(quoteJsonLongIntegerFields(configJson));
    if (!parsed || typeof parsed !== 'object') return null;
    const root = parsed as any;
    const nested = root.dataQuery && typeof root.dataQuery === 'object' ? root.dataQuery : undefined;
    const dq = nested || root;
    return {
      dataSourceId: asIdField(firstDefined(root.dataSourceId, root.autoGenSourceId, nested?.dataSourceId, nested?.autoGenSourceId)),
      dataTypeId: asIdField(firstDefined(root.dataTypeId, nested?.dataTypeId)),
      typeCodes: mergeTypeCodes(root.typeCodes, nested?.typeCodes),
      startTime: firstDefined(dq.startTime, root.startTime),
      endTime: firstDefined(dq.endTime, root.endTime),
      minLongitude: asNumber(firstDefined(dq.minLongitude, root.minLongitude)),
      maxLongitude: asNumber(firstDefined(dq.maxLongitude, root.maxLongitude)),
      minLatitude: asNumber(firstDefined(dq.minLatitude, root.minLatitude)),
      maxLatitude: asNumber(firstDefined(dq.maxLatitude, root.maxLatitude)),
      pageSize: asNumber(firstDefined(dq.pageSize, root.pageSize)) ?? 1000,
      time: firstDefined(dq.time, root.time) && typeof firstDefined(dq.time, root.time) === 'object'
        ? (firstDefined(dq.time, root.time) as RelativeTimeConfig)
        : undefined,
    };
  } catch (e) {
    console.error('解析 dataQueryConfig 失败:', e);
    return null;
  }
};

export const buildDataQueryConfigJson = (cfg: Partial<DataQueryConfig>): string => {
  const out: any = { ...cfg };
  // 规范 typeCodes（允许外部传 string / string[]）
  out.typeCodes = parseTypeCodes((out as any).typeCodes);

  // 序列化前将主键改为字符串，使 JSON 中为带引号的数字串，避免下游 JSON.parse 丢精度
  if (out.dataSourceId != null && out.dataSourceId !== '') {
    out.dataSourceId = String(out.dataSourceId).trim();
  }
  if (out.dataTypeId != null && out.dataTypeId !== '') {
    out.dataTypeId = String(out.dataTypeId).trim();
  }

  Object.keys(out).forEach((key) => {
    const v = out[key];
    if (v === undefined || v === null || v === '') {
      delete out[key];
    }
  });

  return Object.keys(out).length ? JSON.stringify(out) : '';
};

/**
 * 支持 dataQueryConfig.time.mode=range 时的“相对时间”转换
 */
export const resolveRelativeTimeRange = (cfg?: DataQueryConfig | null) => {
  if (!cfg) return { startTime: undefined as string | undefined, endTime: undefined as string | undefined };
  if (cfg.startTime || cfg.endTime) return { startTime: cfg.startTime, endTime: cfg.endTime };
  if (cfg.time?.mode !== 'range') return { startTime: undefined, endTime: undefined };

  const hours = cfg.time.hours ?? cfg.time.defaultHours;
  if (!hours || !Number.isFinite(hours) || hours <= 0) return { startTime: undefined, endTime: undefined };

  const end = new Date();
  const start = new Date(end.getTime() - hours * 60 * 60 * 1000);
  const toIso = (d: Date) => d.toISOString();
  return { startTime: toIso(start), endTime: toIso(end) };
};


