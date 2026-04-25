/**
 * JSON 中的大整数（Snowflake 等）在浏览器 JSON.parse 时会按 IEEE754 截断。
 * 解析前将常见主键字段的裸数字改为 JSON 字符串，再 parse 即可得到精确字符串值。
 */

const DEFAULT_LONG_ID_KEYS = [
  'dataSourceId',
  'dataTypeId',
  'sceneId',
  'chartId',
] as const;

/**
 * 将 JSON 文本里指定字段的 `"key": 123` 改为 `"key":"123"`（仅匹配非负整数字面量）
 */
export function quoteJsonLongIntegerFields(
  jsonStr: string,
  fieldNames: readonly string[] = DEFAULT_LONG_ID_KEYS
): string {
  let s = jsonStr;
  for (const key of fieldNames) {
    const re = new RegExp(`"${key}"\\s*:\\s*(\\d+)`, 'g');
    s = s.replace(re, `"${key}":"$1"`);
  }
  return s;
}

export function parseJsonPreservingLongIds<T = unknown>(jsonStr: string): T {
  return JSON.parse(quoteJsonLongIntegerFields(jsonStr)) as T;
}

/** 请求体中可能与 Java Long 对应的字段（避免 JSON 数字精度问题） */
const BODY_LONG_KEYS = [
  'dataSourceId',
  'dataTypeId',
  'sceneId',
  'chartId',
  'id',
  'sourceFileId',
  'userId',
  'authorId',
  'postId',
  'parentId',
  'replyToUserId',
  'expertId',
  'commentId',
  'createdUser',
  'updatedUser',
] as const;

/** POST 请求体：将 ID 字段统一为字符串，便于后端 Jackson 反序列化为 Long */
export function stringifyBodyLongIdFields<T extends Record<string, unknown>>(body: T): T {
  const out = { ...body } as Record<string, unknown>;
  for (const k of BODY_LONG_KEYS) {
    const v = out[k];
    if (v === undefined || v === null || v === '') continue;
    if (typeof v === 'number' || typeof v === 'string') {
      out[k] = String(v).trim();
    }
  }
  return out as T;
}
