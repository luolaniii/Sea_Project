package com.boot.study.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public enum FileTypeEnum {
    VIDEO("VIDEO", "视频"),
    DOC("DOC", "文档"),
    EXCEL("EXCEL", "EXCEL文件"),

    PDF("PDF", "pdf文件"),

    PPT("PPT", "ppt文件");

    private final String type;
    private final String desc;



    public static FileTypeEnum  getByType(String type){
        for (FileTypeEnum value : values()) {
            if (value.getType().equals(type)){
                return value;
            }
        }
        return null;
    }
}
