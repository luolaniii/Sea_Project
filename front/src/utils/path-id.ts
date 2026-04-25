/**
 * 大整数主键（Snowflake 等）在 URL / 路由中的安全处理。
 * JavaScript Number 无法安全表示超过 2^53-1 的整数，禁止对 ID 使用 Number()。
 */

/** 拼接到 URL 路径的 ID（自动 trim + 编码） */
export function toPathId(id: string | number): string {
  return encodeURIComponent(String(id).trim());
}

/** 读取 Vue Router 的 params.id（或任意单值 param） */
export function routeParamId(raw: unknown): string {
  const v = Array.isArray(raw) ? raw[0] : raw;
  return v != null && String(v).trim() !== '' ? String(v).trim() : '';
}
