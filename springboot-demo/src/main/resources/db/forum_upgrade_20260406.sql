SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================
-- 论坛能力升级（增量迁移）
-- 执行前请先备份数据库
-- =========================

-- 1) 用户角色扩展：支持 expert
ALTER TABLE `sys_user`
  ADD COLUMN `expert_title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '专家头衔（专家用户可填）' AFTER `role`,
  ADD COLUMN `expert_field` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '专家领域（如海况预测、波浪风险）' AFTER `expert_title`;

-- 2) 帖子扩展：支持数据分析、可靠性评选
ALTER TABLE `forum_post`
  ADD COLUMN `post_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'TOPIC_DISCUSSION' COMMENT '帖子类型：TOPIC_DISCUSSION-主题讨论, DATA_ANALYSIS-数据分析' AFTER `category`,
  ADD COLUMN `analysis_target` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分析目标：如海况预测、波浪风险、潮汐变化' AFTER `post_type`,
  ADD COLUMN `reliability_status` tinyint NOT NULL DEFAULT 0 COMMENT '可靠性状态：0-未评选, 1-评选中, 2-已认证' AFTER `analysis_target`,
  ADD COLUMN `reliability_score` decimal(5,2) NOT NULL DEFAULT 0.00 COMMENT '可靠性综合评分（0-100）' AFTER `reliability_status`,
  ADD COLUMN `evaluation_count` int NOT NULL DEFAULT 0 COMMENT '专家评估次数' AFTER `reliability_score`;

CREATE INDEX `idx_post_type` ON `forum_post`(`post_type`);
CREATE INDEX `idx_reliability_status` ON `forum_post`(`reliability_status`);
CREATE INDEX `idx_reliability_score` ON `forum_post`(`reliability_score`);

-- 3) 帖子附件（上传研究成果/分析结果）
CREATE TABLE IF NOT EXISTS `forum_post_attachment` (
  `id` bigint NOT NULL COMMENT '附件ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID（关联forum_post表，关联字段，非外键）',
  `uploader_id` bigint NOT NULL COMMENT '上传者ID（关联sys_user表，关联字段，非外键）',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '附件原始名称',
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '附件访问URL',
  `file_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'FILE' COMMENT '附件类型：FILE-文件, IMAGE-图片, DATASET-数据集, REPORT-报告',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小（字节）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-不可用, 1-可用',
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展信息（JSON格式）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_user` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_user` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_user` bigint NULL DEFAULT NULL COMMENT '删除人ID',
  `deleted_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_post_id`(`post_id`) USING BTREE,
  INDEX `idx_uploader_id`(`uploader_id`) USING BTREE,
  INDEX `idx_file_type`(`file_type`) USING BTREE,
  INDEX `idx_deleted_flag`(`deleted_flag`) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='论坛帖子附件表' ROW_FORMAT=Dynamic;

-- 4) 评选投票（可靠海况分析）
CREATE TABLE IF NOT EXISTS `forum_post_vote` (
  `id` bigint NOT NULL COMMENT '投票ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID（关联forum_post表，关联字段，非外键）',
  `user_id` bigint NOT NULL COMMENT '投票用户ID（关联sys_user表，关联字段，非外键）',
  `vote_type` tinyint NOT NULL COMMENT '投票类型：1-支持, -1-反对',
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展信息（JSON格式）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_user` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_user` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_user` bigint NULL DEFAULT NULL COMMENT '删除人ID',
  `deleted_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_post_user_vote`(`post_id`, `user_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_vote_type`(`vote_type`) USING BTREE,
  INDEX `idx_deleted_flag`(`deleted_flag`) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='论坛帖子投票表（可靠海况评选）' ROW_FORMAT=Dynamic;

-- 5) 专家评估
CREATE TABLE IF NOT EXISTS `forum_post_evaluation` (
  `id` bigint NOT NULL COMMENT '评估ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID（关联forum_post表，关联字段，非外键）',
  `evaluator_id` bigint NOT NULL COMMENT '评估人ID（关联sys_user表，关联字段，非外键）',
  `evaluator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '评估人用户名（冗余字段）',
  `is_expert` tinyint NOT NULL DEFAULT 1 COMMENT '是否专家评估：0-否, 1-是',
  `score_accuracy` decimal(5,2) NOT NULL DEFAULT 0.00 COMMENT '准确性评分（0-100）',
  `score_risk` decimal(5,2) NOT NULL DEFAULT 0.00 COMMENT '风险评估评分（0-100）',
  `score_reasoning` decimal(5,2) NOT NULL DEFAULT 0.00 COMMENT '可解释性评分（0-100）',
  `total_score` decimal(5,2) NOT NULL DEFAULT 0.00 COMMENT '综合评分（0-100）',
  `comment` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '评估意见',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-无效, 1-有效',
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展信息（JSON格式）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_user` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_user` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_user` bigint NULL DEFAULT NULL COMMENT '删除人ID',
  `deleted_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_post_evaluator`(`post_id`, `evaluator_id`) USING BTREE,
  INDEX `idx_is_expert`(`is_expert`) USING BTREE,
  INDEX `idx_total_score`(`total_score`) USING BTREE,
  INDEX `idx_deleted_flag`(`deleted_flag`) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='论坛帖子专家评估表' ROW_FORMAT=Dynamic;

-- 6) 专家答疑表（旧库可能没有）
CREATE TABLE IF NOT EXISTS `expert_answer` (
  `id` bigint NOT NULL COMMENT '专家回答ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID（关联forum_post表，关联字段，非外键）',
  `comment_id` bigint NULL DEFAULT NULL COMMENT '关联评论ID（可空，关联字段，非外键）',
  `expert_id` bigint NOT NULL COMMENT '专家用户ID（关联sys_user表，关联字段，非外键）',
  `expert_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '专家用户名（冗余字段）',
  `expert_title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '专家头衔',
  `answer_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '回答内容',
  `answer_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'QA' COMMENT '回答类型：QA-答疑, EVALUATION-评估意见',
  `like_count` int NOT NULL DEFAULT 0 COMMENT '点赞数',
  `is_accepted` tinyint NOT NULL DEFAULT 0 COMMENT '是否采纳：0-否, 1-是',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0-正常, 1-已删除',
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展信息（JSON格式）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_user` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_user` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_user` bigint NULL DEFAULT NULL COMMENT '删除人ID',
  `deleted_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_post_id`(`post_id`) USING BTREE,
  INDEX `idx_comment_id`(`comment_id`) USING BTREE,
  INDEX `idx_expert_id`(`expert_id`) USING BTREE,
  INDEX `idx_is_accepted`(`is_accepted`) USING BTREE,
  INDEX `idx_deleted_flag`(`deleted_flag`) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专家回答表' ROW_FORMAT=Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
