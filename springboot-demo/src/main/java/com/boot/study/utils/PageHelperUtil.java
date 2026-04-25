package com.boot.study.utils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.study.api.req.base.QueryReq;


public class PageHelperUtil {

    public static <T> Page<T> page(QueryReq req) {
        return Page.of(req.getPageNum(), req.getPageSize());
    }

}
