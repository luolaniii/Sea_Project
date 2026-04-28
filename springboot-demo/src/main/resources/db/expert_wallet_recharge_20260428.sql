SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================
-- 专家申请 / 钱包徽章 / 模拟充值会员（增量迁移）
-- 执行前请先备份数据库
-- =========================

-- 1) 专家申请表
CREATE TABLE IF NOT EXISTS `expert_application` (
  `id` bigint NOT NULL COMMENT '申请ID',
  `user_id` bigint NOT NULL COMMENT '申请人ID（关联sys_user，关联字段，非外键）',
  `real_name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `organization` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '所属单位',
  `profession` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '职业 / 头衔',
  `expertise_tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '擅长领域，逗号分隔',
  `application_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '申请理由',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED',
  `reviewer_id` bigint NULL DEFAULT NULL COMMENT '审核人ID',
  `review_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核备注',
  `reviewed_at` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_user` bigint NULL DEFAULT NULL,
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_user` bigint NULL DEFAULT NULL,
  `deleted_time` datetime NULL DEFAULT NULL,
  `deleted_user` bigint NULL DEFAULT NULL,
  `deleted_flag` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ea_user_id`(`user_id`) USING BTREE,
  INDEX `idx_ea_status`(`status`) USING BTREE,
  INDEX `idx_ea_deleted_flag`(`deleted_flag`) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专家申请表' ROW_FORMAT=Dynamic;


-- 2) 用户钱包
CREATE TABLE IF NOT EXISTS `user_wallet` (
  `id` bigint NOT NULL COMMENT '钱包ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（一对一）',
  `balance_coins` int NOT NULL DEFAULT 0 COMMENT '当前余额（海洋币）',
  `total_earned_coins` int NOT NULL DEFAULT 0 COMMENT '累计获得',
  `total_spent_coins` int NOT NULL DEFAULT 0 COMMENT '累计消费',
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_user` bigint NULL DEFAULT NULL,
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_user` bigint NULL DEFAULT NULL,
  `deleted_time` datetime NULL DEFAULT NULL,
  `deleted_user` bigint NULL DEFAULT NULL,
  `deleted_flag` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_uw_user_id`(`user_id`) USING BTREE,
  INDEX `idx_uw_deleted_flag`(`deleted_flag`) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户钱包表' ROW_FORMAT=Dynamic;


-- 3) 钱包流水
CREATE TABLE IF NOT EXISTS `wallet_transaction` (
  `id` bigint NOT NULL COMMENT '流水ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `type` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型：EARN_BADGE/EARN_REVIEW/RECHARGE_BONUS/SPEND/REFUND',
  `amount` int NOT NULL COMMENT '币数（正负皆可）',
  `balance_after` int NOT NULL COMMENT '操作后余额',
  `ref_type` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联类型：BADGE/EVALUATION/ORDER 等',
  `ref_id` bigint NULL DEFAULT NULL COMMENT '关联记录ID',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_user` bigint NULL DEFAULT NULL,
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_user` bigint NULL DEFAULT NULL,
  `deleted_time` datetime NULL DEFAULT NULL,
  `deleted_user` bigint NULL DEFAULT NULL,
  `deleted_flag` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_wt_user_time`(`user_id`, `created_time`) USING BTREE,
  INDEX `idx_wt_type`(`type`) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='钱包流水表' ROW_FORMAT=Dynamic;


-- 4) 徽章定义
CREATE TABLE IF NOT EXISTS `badge` (
  `id` bigint NOT NULL COMMENT '徽章ID',
  `code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '唯一编码',
  `name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `icon_emoji` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '展示用 emoji',
  `threshold_count` int NOT NULL DEFAULT 0 COMMENT '解锁阈值（评估次数）',
  `reward_coins` int NOT NULL DEFAULT 0 COMMENT '解锁奖励币',
  `sort_order` int NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0-停用 1-启用',
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_user` bigint NULL DEFAULT NULL,
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_user` bigint NULL DEFAULT NULL,
  `deleted_time` datetime NULL DEFAULT NULL,
  `deleted_user` bigint NULL DEFAULT NULL,
  `deleted_flag` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_badge_code`(`code`) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='徽章定义表' ROW_FORMAT=Dynamic;

-- 5) 用户已得徽章
CREATE TABLE IF NOT EXISTS `user_badge` (
  `id` bigint NOT NULL COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `badge_id` bigint NOT NULL COMMENT '徽章ID',
  `evaluated_count_snapshot` int NULL DEFAULT NULL COMMENT '解锁时的累计评估次数',
  `awarded_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '解锁时间',
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_user` bigint NULL DEFAULT NULL,
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_user` bigint NULL DEFAULT NULL,
  `deleted_time` datetime NULL DEFAULT NULL,
  `deleted_user` bigint NULL DEFAULT NULL,
  `deleted_flag` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_ub_user_badge`(`user_id`, `badge_id`) USING BTREE,
  INDEX `idx_ub_user`(`user_id`) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户徽章表' ROW_FORMAT=Dynamic;

-- 种子徽章
INSERT INTO `badge`(`id`, `code`, `name`, `description`, `icon_emoji`, `threshold_count`, `reward_coins`, `sort_order`, `status`)
VALUES
  (10001, 'BRONZE_REVIEWER', '青铜评审员', '完成 5 次有效评估', '🥉', 5, 50, 1, 1),
  (10002, 'SILVER_REVIEWER', '白银评审员', '完成 20 次有效评估', '🥈', 20, 200, 2, 1),
  (10003, 'GOLD_REVIEWER', '黄金评审员', '完成 50 次有效评估', '🥇', 50, 600, 3, 1),
  (10004, 'OCEAN_GUARDIAN', '海洋守护者', '完成 100 次有效评估', '🌊', 100, 1500, 4, 1)
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `description`=VALUES(`description`), `icon_emoji`=VALUES(`icon_emoji`),
  `threshold_count`=VALUES(`threshold_count`), `reward_coins`=VALUES(`reward_coins`),
  `sort_order`=VALUES(`sort_order`), `status`=VALUES(`status`);


-- 6) 会员套餐
CREATE TABLE IF NOT EXISTS `membership_plan` (
  `id` bigint NOT NULL COMMENT '套餐ID',
  `plan_code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '唯一编码',
  `name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `days` int NOT NULL COMMENT '会员天数',
  `price_yuan` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '价格（人民币）',
  `bonus_coins` int NOT NULL DEFAULT 0 COMMENT '附送币数',
  `sort_order` int NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 1,
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_user` bigint NULL DEFAULT NULL,
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_user` bigint NULL DEFAULT NULL,
  `deleted_time` datetime NULL DEFAULT NULL,
  `deleted_user` bigint NULL DEFAULT NULL,
  `deleted_flag` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_mp_code`(`plan_code`) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员套餐表' ROW_FORMAT=Dynamic;

INSERT INTO `membership_plan`(`id`, `plan_code`, `name`, `description`, `days`, `price_yuan`, `bonus_coins`, `sort_order`, `status`)
VALUES
  (20001, 'MONTH', '海洋月卡', '30 天会员 + 100 海洋币',  30, 9.90,  100, 1, 1),
  (20002, 'SEASON', '海洋季卡', '90 天会员 + 400 海洋币', 90, 28.00, 400, 2, 1),
  (20003, 'YEAR', '海洋年卡', '365 天会员 + 2000 海洋币', 365, 99.00, 2000, 3, 1)
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `description`=VALUES(`description`), `days`=VALUES(`days`),
  `price_yuan`=VALUES(`price_yuan`), `bonus_coins`=VALUES(`bonus_coins`),
  `sort_order`=VALUES(`sort_order`), `status`=VALUES(`status`);


-- 7) 充值订单
CREATE TABLE IF NOT EXISTS `recharge_order` (
  `id` bigint NOT NULL,
  `order_no` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号（唯一）',
  `user_id` bigint NOT NULL,
  `plan_id` bigint NOT NULL,
  `amount_yuan` decimal(10,2) NOT NULL DEFAULT 0.00,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PAID/CANCELLED/REFUNDED',
  `mock_pay_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'WECHAT_MOCK/ALIPAY_MOCK',
  `paid_at` datetime NULL DEFAULT NULL,
  `refunded_at` datetime NULL DEFAULT NULL,
  `refund_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_user` bigint NULL DEFAULT NULL,
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_user` bigint NULL DEFAULT NULL,
  `deleted_time` datetime NULL DEFAULT NULL,
  `deleted_user` bigint NULL DEFAULT NULL,
  `deleted_flag` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_ro_order_no`(`order_no`) USING BTREE,
  INDEX `idx_ro_user_status`(`user_id`, `status`) USING BTREE,
  INDEX `idx_ro_status`(`status`) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='充值订单表' ROW_FORMAT=Dynamic;


-- 8) 用户会员状态
CREATE TABLE IF NOT EXISTS `user_membership` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `plan_id` bigint NULL DEFAULT NULL,
  `started_at` datetime NULL DEFAULT NULL,
  `expires_at` datetime NULL DEFAULT NULL,
  `last_order_id` bigint NULL DEFAULT NULL,
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_user` bigint NULL DEFAULT NULL,
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_user` bigint NULL DEFAULT NULL,
  `deleted_time` datetime NULL DEFAULT NULL,
  `deleted_user` bigint NULL DEFAULT NULL,
  `deleted_flag` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_um_user_id`(`user_id`) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户会员状态表' ROW_FORMAT=Dynamic;


SET FOREIGN_KEY_CHECKS = 1;
