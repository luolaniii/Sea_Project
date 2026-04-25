package com.boot.study.api.req.base;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class QueryReq implements Serializable {
    private static final long serialVersionUID = 4255023846210169773L;
    @Schema(description = "分页参数")
    private int pageNum = 1;

    @Schema(description = "分页大小")
    private int pageSize = 10;
}