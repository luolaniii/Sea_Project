package com.boot.study.service;

import com.boot.study.api.req.user.OceanAnalysisReq;
import com.boot.study.api.req.user.OceanComparisonReq;
import com.boot.study.entity.OceanAbnormalResultVO;
import com.boot.study.entity.OceanComfortResultVO;
import com.boot.study.entity.OceanComparisonResultVO;
import com.boot.study.entity.OceanCompositeResultVO;
import com.boot.study.entity.OceanStabilityResultVO;
import com.boot.study.entity.OceanTrendResultVO;

public interface OceanAnalysisService {

    OceanAbnormalResultVO abnormal(OceanAnalysisReq req);

    OceanStabilityResultVO stability(OceanAnalysisReq req);

    OceanComfortResultVO comfort(OceanAnalysisReq req);

    OceanTrendResultVO trend(OceanAnalysisReq req);

    OceanCompositeResultVO composite(OceanAnalysisReq req);

    /**
     * Module I：多站点对比分析与空间异常检测
     */
    OceanComparisonResultVO compare(OceanComparisonReq req);
}

