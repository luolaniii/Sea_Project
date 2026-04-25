package com.boot.study.controller.user;

import com.boot.study.bean.Result;
import com.boot.study.service.WaveEstimationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 波浪估算测试/调用端点（Module G）
 * <p>
 * 允许前端/科研人员直接调用 P-M 公式和三级回退链，无需写 SQL。
 */
@RestController
@RequestMapping("/api/user/wave-estimation")
@RequiredArgsConstructor
public class UserWaveEstimationController {

    private final WaveEstimationService waveEstimationService;

    /** P-M 公式：输入风速 → 波高+周期 */
    @GetMapping("/from-wind")
    public Result<WaveEstimationService.WaveEstimate> fromWind(@RequestParam double windSpeed) {
        return Result.success(waveEstimationService.estimateFromWind(windSpeed));
    }

    /** 气候默认值 */
    @GetMapping("/default")
    public Result<WaveEstimationService.WaveEstimate> defaultClimatology() {
        return Result.success(waveEstimationService.defaultClimatology());
    }
}
