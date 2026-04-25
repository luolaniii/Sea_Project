package com.boot.study.dao;

import com.boot.study.entity.ObservationData;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 观测数据表 Mapper
 */
@Mapper
public interface ObservationDataMapper extends BaseMapperX<ObservationData> {

    /**
     * 使用单条 INSERT 语句批量插入观测数据
     * <p>
     * 注意：
     * - 需要调用方保证每条数据的主键 ID 已经生成（例如使用 Snowflake）
     * - 未显式插入的审计字段（created_time 等）由数据库默认值负责
     *
     * @param list 待插入的数据列表
     * @return 影响的行数
     */
    @Insert({
            "<script>",
            "INSERT IGNORE INTO observation_data (",
            "  id,",
            "  data_source_id,",
            "  data_type_id,",
            "  observation_time,",
            "  data_value,",
            "  longitude,",
            "  latitude,",
            "  depth,",
            "  quality_flag,",
            "  source_file_id,",
            "  api_data_id,",
            "  remark",
            ") VALUES ",
            "<foreach collection='list' item='item' separator=','>",
            "  (",
            "    #{item.id},",
            "    #{item.dataSourceId},",
            "    #{item.dataTypeId},",
            "    #{item.observationTime},",
            "    #{item.dataValue},",
            "    #{item.longitude},",
            "    #{item.latitude},",
            "    #{item.depth},",
            "    #{item.qualityFlag},",
            "    #{item.sourceFileId},",
            "    #{item.apiDataId},",
            "    #{item.remark}",
            "  )",
            "</foreach>",
            "</script>"
    })
    int insertBatch(@Param("list") List<ObservationData> list);
}

