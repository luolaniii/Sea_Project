package com.boot.study.utils;

import cn.hutool.core.util.StrUtil;

/**
 * 将路径中的 ID 字符串解析为 {@link Long}（路径段本身为文本，与前端大整数以字符串传递的约定一致）。
 */
public final class PathLongParser {

    private PathLongParser() {
    }

    /**
     * @return 解析成功返回 Long；空串或非法数字返回 null
     */
    public static Long tryParse(String raw) {
        if (raw == null || StrUtil.isBlank(raw)) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
