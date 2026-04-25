package com.boot.study.controller.user;

import com.boot.study.api.req.user.OceanAnalysisReq;
import com.boot.study.bean.Result;
import com.boot.study.dao.DataSourceMapper;
import com.boot.study.entity.DataSource;
import com.boot.study.entity.OceanAbnormalResultVO;
import com.boot.study.entity.OceanCompositeResultVO;
import com.boot.study.entity.OceanStabilityResultVO;
import com.boot.study.service.AiForecastService;
import com.boot.study.service.OceanAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 客户端 AI 辅助决策接口
 * <p>
 * 基于站点的 composite 海况快照 + DeepSeek 大模型，给出未来 24h 海况预测和作业建议。
 */
@Slf4j
@RestController
@RequestMapping("/api/user/ai-forecast")
@RequiredArgsConstructor
public class UserAiForecastController {

    private final OceanAnalysisService oceanAnalysisService;
    private final AiForecastService aiForecastService;
    private final DataSourceMapper dataSourceMapper;

    @PostMapping("/forecast")
    public Result<Map<String, Object>> forecast(@RequestBody OceanAnalysisReq req) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        try {
            // 1. 拉取站点信息
            DataSource ds = req.getDataSourceId() != null
                    ? dataSourceMapper.selectById(req.getDataSourceId())
                    : null;
            if (ds != null) {
                ctx.put("stationName", ds.getSourceName());
                ctx.put("stationId", ds.getStationId());
                ctx.put("longitude", ds.getLongitude());
                ctx.put("latitude", ds.getLatitude());
            }

            // 2. 跑 composite 拿到当前快照 + 已计算指标
            OceanCompositeResultVO composite = oceanAnalysisService.composite(req);
            if (composite != null) {
                ctx.put("currentTime", composite.getCurrentTime());
                ctx.put("temperature", composite.getTemperature());
                ctx.put("salinity", composite.getSalinity());
                ctx.put("seaLevel", composite.getSeaLevel());
                ctx.put("waveHeight", composite.getWaveHeight());
                ctx.put("windSpeed", composite.getWindSpeed());

                OceanStabilityResultVO stability = composite.getStability();
                if (stability != null) {
                    ctx.put("stabilityLevel", stability.getLevel());
                }
                if (composite.getComfort() != null) {
                    ctx.put("comfortScore", composite.getComfort().getScore());
                }
                OceanAbnormalResultVO abn = composite.getAbnormal();
                if (abn != null) {
                    ctx.put("abnormalLevel1", Boolean.TRUE.equals(abn.getLevel1Alarm()) ? "是" : "否");
                    ctx.put("abnormalLevel2", Boolean.TRUE.equals(abn.getLevel2Alarm()) ? "是" : "否");
                }

                // 3. 简短趋势摘要（最近若干点）
                if (composite.getSeries() != null && !composite.getSeries().isEmpty()) {
                    int n = composite.getSeries().size();
                    int from = Math.max(0, n - 8);
                    StringBuilder sb = new StringBuilder();
                    for (int i = from; i < n; i++) {
                        OceanCompositeResultVO.SeriesPointVO p = composite.getSeries().get(i);
                        sb.append("- ").append(p.getTime()).append(" 温=")
                                .append(p.getTemperature()).append(" 波=")
                                .append(p.getWaveHeight()).append(" 风=")
                                .append(p.getWindSpeed()).append("\n");
                    }
                    ctx.put("history", sb.toString());
                }
            }
        } catch (Exception e) {
            log.warn("AI 预测：拉取站点上下文失败", e);
        }

        // 4. 调用 DeepSeek
        Map<String, Object> result = aiForecastService.forecast(ctx);
        // 把上下文也带回前端，便于调试或展示
        result.put("context", ctx);
        return Result.success(result);
    }
}
