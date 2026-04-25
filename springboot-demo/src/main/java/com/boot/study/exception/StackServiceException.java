package com.boot.study.exception;

import cn.hutool.core.util.StrUtil;
import com.boot.study.enums.ResultEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StackServiceException extends RuntimeException {

    private Integer code;

    public StackServiceException() {
        super();
    }

    public StackServiceException(String message) {
        super(message);
    }

    public StackServiceException(String message, Object... params) {
        super(StrUtil.format(message, params));
    }

    public StackServiceException(Integer code, String message, Object... params) {
        super(StrUtil.format(message, params));
        this.code = code;
    }

    public StackServiceException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public StackServiceException(ResultEnum resultEnum) {
        this(resultEnum.getCode(), resultEnum.getMsg());
    }
}
