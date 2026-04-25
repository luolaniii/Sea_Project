-- observation_data 入库去重能力升级
-- 目标：
-- 1) 通过唯一索引强制保证 api_data_id 不重复
-- 2) 保留历史普通索引兼容处理

ALTER TABLE `observation_data`
  DROP INDEX `idx_api_data_id`,
  ADD UNIQUE INDEX `uk_api_data_id` (`api_data_id`) USING BTREE;

