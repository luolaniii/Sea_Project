package com.boot.study.cron;

import com.boot.study.entity.DataSource;
import com.boot.study.service.NdbcAutoSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * NDBC数据自动同步定时任务
 * <p>
 * 定时检查并同步启用自动采集的站点数据
 *
 * @author study
 * @since 2024
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NdbcSyncScheduler {

    private final NdbcAutoSyncService ndbcAutoSyncService;

    /**
     * 定时检查并同步站点数据
     * <p>
     * 每分钟检查一次，根据各站点的同步间隔决定是否需要同步
     */
    @Scheduled(fixedRate = 60000)
    public void checkAndSync() {
        log.debug("开始检查需要同步的NDBC站点...");

        try {
            List<DataSource> stationsToSync = ndbcAutoSyncService.findStationsToSync();

            if (stationsToSync.isEmpty()) {
                log.debug("暂无需要同步的站点");
                return;
            }

            log.info("发现 {} 个站点需要同步", stationsToSync.size());

            for (DataSource station : stationsToSync) {
                try {
                    log.info("开始同步站点: {} ({})", station.getSourceName(), station.getStationId());

                    Map<String, Object> result = ndbcAutoSyncService.syncStation(station);

                    if ("success".equals(result.get("status"))) {
                        log.info("站点 {} 同步完成，解析: {} 条，保存: {} 条",
                                station.getStationId(),
                                result.get("totalParsed"),
                                result.get("totalSaved"));
                    } else {
                        log.warn("站点 {} 同步失败: {}",
                                station.getStationId(),
                                result.get("message"));
                    }

                } catch (Exception e) {
                    log.error("同步站点 {} 出错: {}", station.getStationId(), e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("NDBC同步任务执行异常", e);
        }
    }

    /**
     * 每天凌晨2点执行一次全量同步
     * <p>
     * 作为定时任务的补充，确保数据完整性
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyFullSync() {
        log.info("开始执行每日全量同步任务...");

        try {
            Map<String, Object> result = ndbcAutoSyncService.syncAllEnabledStations();

            log.info("每日全量同步完成，站点数: {}，成功: {}，失败: {}",
                    result.get("totalStations"),
                    result.get("successCount"),
                    result.get("failCount"));

        } catch (Exception e) {
            log.error("每日全量同步任务执行异常", e);
        }
    }
}
