/**
 * 解析后端观测时间（常见：Jackson LocalDateTime -> "yyyy-MM-dd HH:mm:ss"）。
 * 浏览器原生 Date.parse 对空格分隔的日期时间常返回 NaN，需显式处理。
 */
export function parseObservationTimeToMs(value: unknown): number {
  if (value == null) return NaN;
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  if (Array.isArray(value) && value.length >= 3) {
    const y = Number(value[0]);
    const mo = Number(value[1]);
    const d = Number(value[2]);
    const h = value.length > 3 ? Number(value[3]) : 0;
    const mi = value.length > 4 ? Number(value[4]) : 0;
    const s = value.length > 5 ? Number(value[5]) : 0;
    if ([y, mo, d].some((n) => Number.isNaN(n))) return NaN;
    return new Date(y, mo - 1, d, h, mi, s).getTime();
  }
  const s = String(value).trim();
  if (!s) return NaN;

  let ms = Date.parse(s);
  if (!Number.isNaN(ms)) return ms;

  ms = Date.parse(s.replace(' ', 'T'));
  if (!Number.isNaN(ms)) return ms;

  const m = s.match(/^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})(?::(\d{2}))?/);
  if (m) {
    const sec = m[6] != null ? +m[6] : 0;
    return new Date(+m[1], +m[2] - 1, +m[3], +m[4], +m[5], sec).getTime();
  }

  return NaN;
}
