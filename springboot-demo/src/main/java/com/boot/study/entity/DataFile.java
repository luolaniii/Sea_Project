package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boot.study.enums.DataFileTypeEnum;
import com.boot.study.enums.ParseStatusEnum;
import com.boot.study.enums.UploadStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据文件表实体，对应表：data_file
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("data_file")
public class DataFile extends BaseDo {

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型：NETCDF, CSV, JSON, COARDS, WOCE
     */
    private DataFileTypeEnum fileType;

    /**
     * 文件格式
     */
    private String fileFormat;

    /**
     * 上传状态
     */
    private UploadStatusEnum uploadStatus;

    /**
     * 解析状态
     */
    private ParseStatusEnum parseStatus;

    /**
     * 解析结果（JSON格式）
     */
    private String parseResult;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 数据源ID
     */
    private Long dataSourceId;

    /**
     * 描述
     */
    private String description;
}


