package com.boot.study.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 枚举项DTO，用于返回给前端
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnumItem implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 枚举的code值
     */
    private Object code;

    /**
     * 枚举的desc描述
     */
    private String desc;
}

