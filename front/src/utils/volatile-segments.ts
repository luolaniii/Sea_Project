import { parseObservationTimeToMs } from './observation-time';

/** 海洋环境辅助决策中的四个分析 */
export type AnalysisKind = '异常预警' | '稳定性' | '舒适度' | '短期趋势';

export type VolatileSegment = {
  typeCode: string;
  typeLabel: string;
  startTime: string;
  endTime: string;
  fromValue: number;
  toValue: number;
  delta: number;
  level: 'high' | 'medium';
  /** 与本次剧烈变化相关的分析标签（来自四个分析） */
  analyses: AnalysisKind[];
};

const TYPE_LABELS: Record<string, string> = {
  TEMP: '温度',
  WTMP: '水温',
  SST: '海表温度',
  SAL: '盐度',
  SALINITY: '盐度',
  SEA_LEVEL: '潮位',
  TIDE: '潮位',
  WVHT: '有效波高',
  WAVE_HEIGHT: '波高',
  WIND_SPEED: '风速',
  WSPD: '风速',
  PRES: '气压',
  PRESSURE: '气压',
};

/**
 * 观测指标 → 涉及的分析维度映射
 * 例如波高同时影响"异常预警"和"舒适度"两项分析
 */
const METRIC_ANALYSES: Record<string, AnalysisKind[]> = {
  TEMP: ['异常预警', '短期趋势'],
  WTMP: ['异常预警', '短期趋势'],
  SST: ['异常预警', '短期趋势'],
  SAL: ['稳定性'],
  SALINITY: ['稳定性'],
  SEA_LEVEL: ['短期趋势', '稳定性'],
  TIDE: ['短期趋势', '稳定性'],
  WVHT: ['异常预警', '舒适度'],
  WAVE_HEIGHT: ['异常预警', '舒适度'],
  WIND_SPEED: ['异常预警', '舒适度'],
  WSPD: ['异常预警', '舒适度'],
  PRES: ['稳定性'],
  PRESSURE: ['稳定性'],
};

const resolveAnalyses = (typeCode: string, level: 'high' | 'medium'): AnalysisKind[] => {
  const base = METRIC_ANALYSES[typeCode] || ['稳定性'];
  // 高强度变化默认会同时触发异常预警
  if (level === 'high' && !base.includes('异常预警')) return ['异常预警', ...base];
  return base;
};

const toNumber = (value: unknown): number | null => {
  if (value === null || value === undefined || value === '') return null;
  const n = Number(value);
  return Number.isFinite(n) ? n : null;
};

const formatValue = (value: number) => Number(value.toFixed(3));

const formatTime = (value: unknown) => {
  const raw = String(value ?? '').trim();
  if (!raw) return '-';
  return raw.replace('T', ' ').slice(0, 16);
};

const metricCode = (row: any) => String(row?.dataTypeCode || row?.typeCode || row?.dataTypeName || 'VALUE').trim().toUpperCase();

const metricLabel = (row: any) => {
  const code = metricCode(row);
  return String(row?.dataTypeName || TYPE_LABELS[code] || code || '观测值');
};

export const buildVolatileSegmentsFromObservations = (
  rows: any[],
  options: { maxSegments?: number; maxPerType?: number } = {}
): VolatileSegment[] => {
  const maxSegments = options.maxSegments ?? 8;
  const maxPerType = options.maxPerType ?? 2;
  const groups = new Map<string, Array<{ time: number; rawTime: string; value: number; row: any }>>();

  (rows || []).forEach((row) => {
    const value = toNumber(row?.dataValue ?? row?.value);
    const time = parseObservationTimeToMs(row?.observationTime ?? row?.time ?? row?.timestamp);
    if (value === null || Number.isNaN(time)) return;
    const code = metricCode(row);
    if (!groups.has(code)) groups.set(code, []);
    groups.get(code)!.push({ time, rawTime: row?.observationTime ?? row?.time ?? row?.timestamp, value, row });
  });

  const segments: VolatileSegment[] = [];
  groups.forEach((points) => {
    const sorted = points.slice().sort((a, b) => a.time - b.time);
    if (sorted.length < 2) return;

    const deltas = sorted.slice(1).map((point, index) => ({
      prev: sorted[index],
      next: point,
      delta: Math.abs(point.value - sorted[index].value),
    })).filter((item) => item.delta > 0);
    if (!deltas.length) return;

    const avg = deltas.reduce((sum, item) => sum + item.delta, 0) / deltas.length;
    const variance = deltas.reduce((sum, item) => sum + Math.pow(item.delta - avg, 2), 0) / deltas.length;
    const std = Math.sqrt(variance);
    const maxDelta = Math.max(...deltas.map((item) => item.delta));
    const threshold = Math.max(avg + std, maxDelta * 0.55);

    deltas
      .sort((a, b) => b.delta - a.delta)
      .slice(0, maxPerType)
      .forEach((item) => {
        const code = metricCode(item.next.row);
        const lvl: 'high' | 'medium' = item.delta >= threshold ? 'high' : 'medium';
        segments.push({
          typeCode: code,
          typeLabel: metricLabel(item.next.row),
          startTime: formatTime(item.prev.rawTime),
          endTime: formatTime(item.next.rawTime),
          fromValue: formatValue(item.prev.value),
          toValue: formatValue(item.next.value),
          delta: formatValue(item.delta),
          level: lvl,
          analyses: resolveAnalyses(code, lvl),
        });
      });
  });

  return segments
    .sort((a, b) => b.delta - a.delta)
    .slice(0, maxSegments);
};

export const buildVolatileSegmentsFromSeries = (series: any[], maxSegments = 8): VolatileSegment[] => {
  const metrics = [
    { key: 'temperature', code: 'TEMP', name: '温度' },
    { key: 'salinity', code: 'SALINITY', name: '盐度' },
    { key: 'seaLevel', code: 'SEA_LEVEL', name: '潮位' },
    { key: 'waveHeight', code: 'WAVE_HEIGHT', name: '波高' },
    { key: 'windSpeed', code: 'WIND_SPEED', name: '风速' },
  ];

  const rows = (series || []).flatMap((item) => metrics.map((metric) => ({
    observationTime: item?.time,
    dataTypeCode: metric.code,
    dataTypeName: metric.name,
    dataValue: item?.[metric.key],
  })));

  return buildVolatileSegmentsFromObservations(rows, { maxSegments, maxPerType: 2 });
};
