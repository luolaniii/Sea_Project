/*
 * 专家激励 / 充值会员 - 种子数据修复脚本
 * --------------------------------------------------------------------
 * 适用场景：已经执行了 expert_wallet_recharge_20260428.sql 创建表，
 *           但 INSERT 因为客户端字符集问题没有生效，导致 badge / membership_plan 是空表。
 *
 * 安全说明：
 *   1. 使用 INSERT IGNORE，重复执行不会报错。
 *   2. emoji 用 _utf8mb4 X'..' 字面量写入，绕开任何 SET NAMES / 客户端编码问题。
 *   3. 不依赖会话变量，复制贴到任意 MySQL 客户端都能跑。
 *
 * 执行后请用文末的 SELECT 校验。
 */

SET NAMES utf8mb4;

-- =========================================================
-- 1. 徽章定义（铜/银/金/守护者）
-- =========================================================
INSERT IGNORE INTO `badge`
  (`id`, `code`, `name`, `description`, `icon_emoji`,
   `threshold_count`, `reward_coins`, `sort_order`, `status`,
   `created_time`, `updated_time`, `deleted_flag`)
VALUES
  (10001, 'BRONZE_REVIEWER', '青铜评审员',  '完成 5 次有效评估',
   _utf8mb4 X'F09FA589', 5,    50,   1, 1, NOW(), NOW(), 0),
  (10002, 'SILVER_REVIEWER', '白银评审员',  '完成 20 次有效评估',
   _utf8mb4 X'F09FA588', 20,   200,  2, 1, NOW(), NOW(), 0),
  (10003, 'GOLD_REVIEWER',   '黄金评审员',  '完成 50 次有效评估',
   _utf8mb4 X'F09FA587', 50,   600,  3, 1, NOW(), NOW(), 0),
  (10004, 'OCEAN_GUARDIAN',  '海洋守护者',  '完成 100 次有效评估',
   _utf8mb4 X'F09F8C8A', 100,  1500, 4, 1, NOW(), NOW(), 0);

-- =========================================================
-- 2. 会员套餐（月/季/年）
-- =========================================================
INSERT IGNORE INTO `membership_plan`
  (`id`, `plan_code`, `name`, `description`,
   `days`, `price_yuan`, `bonus_coins`, `sort_order`, `status`,
   `created_time`, `updated_time`, `deleted_flag`)
VALUES
  (20001, 'MONTH',  '海洋月卡', '30 天会员 + 100 海洋币',
   30,  9.90,    100, 1, 1, NOW(), NOW(), 0),
  (20002, 'SEASON', '海洋季卡', '90 天会员 + 400 海洋币',
   90,  28.00,   400, 2, 1, NOW(), NOW(), 0),
  (20003, 'YEAR',   '海洋年卡', '365 天会员 + 2000 海洋币',
   365, 99.00,  2000, 3, 1, NOW(), NOW(), 0);

-- =========================================================
-- 3. 校验（执行后请确认两个结果都不是 0）
-- =========================================================
SELECT 'badge_rows' AS metric, COUNT(*) AS rows_count FROM `badge`
UNION ALL
SELECT 'membership_plan_rows', COUNT(*) FROM `membership_plan`;
