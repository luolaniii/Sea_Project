/*
 Navicat Premium Data Transfer

 Source Server         : yangfan918.cn_33123
 Source Server Type    : MySQL
 Source Server Version : 80029
 Source Host           : yangfan918.cn:33123
 Source Schema         : sea_db

 Target Server Type    : MySQL
 Target Server Version : 80029
 File Encoding         : 65001

 Date: 09/04/2026 13:44:32
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for chart_config
-- ----------------------------
DROP TABLE IF EXISTS `chart_config`;
CREATE TABLE `chart_config`  (
  `id` bigint NOT NULL COMMENT '图表ID',
  `chart_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图表名称',
  `chart_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图表类型：LINE-折线图, BAR-柱状图, SCATTER-散点图, HEATMAP-热力图, 3D_SURFACE-3D表面图',
  `user_id` bigint NULL DEFAULT NULL COMMENT '创建用户ID（关联字段，非外键）',
  `data_query_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '数据查询配置（JSON格式）',
  `echarts_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'ECharts配置（JSON格式）',
  `is_public` tinyint NOT NULL DEFAULT 0 COMMENT '是否公开：0-私有, 1-公开',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展信息（JSON格式）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_user` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_user` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_user` bigint NULL DEFAULT NULL COMMENT '删除人ID',
  `deleted_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_chart_type`(`chart_type`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_is_public`(`is_public`) USING BTREE,
  INDEX `idx_deleted_flag`(`deleted_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '图表配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for data_file
-- ----------------------------
DROP TABLE IF EXISTS `data_file`;
CREATE TABLE `data_file`  (
  `id` bigint NOT NULL COMMENT '文件ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件名',
  `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '原始文件名',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件路径',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小（字节）',
  `file_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件类型：NETCDF, CSV, JSON, COARDS, WOCE',
  `file_format` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件格式',
  `upload_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '上传状态：PENDING-待处理, PROCESSING-处理中, SUCCESS-成功, FAILED-失败',
  `parse_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '解析状态：PENDING-待解析, PROCESSING-解析中, SUCCESS-成功, FAILED-失败',
  `parse_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '解析结果（JSON格式）',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
  `data_source_id` bigint NULL DEFAULT NULL COMMENT '数据源ID（关联字段，非外键）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展信息（JSON格式）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_user` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_user` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_user` bigint NULL DEFAULT NULL COMMENT '删除人ID',
  `deleted_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_file_type`(`file_type`) USING BTREE,
  INDEX `idx_upload_status`(`upload_status`) USING BTREE,
  INDEX `idx_data_source_id`(`data_source_id`) USING BTREE,
  INDEX `idx_deleted_flag`(`deleted_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '数据文件表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for data_source
-- ----------------------------
DROP TABLE IF EXISTS `data_source`;
CREATE TABLE `data_source`  (
  `id` bigint NOT NULL COMMENT '数据源ID',
  `source_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据源名称',
  `source_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据源类型：NOAA-美国海洋大气局, ERDDAP-环境研究数据访问协议, CUSTOM-自定义',
  `api_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'API地址',
  `api_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'API密钥',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '描述',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-禁用, 1-启用',
  `config_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '配置信息（JSON格式）',
  `station_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '站点编号(如TIBC1、WIWF1)',
  `longitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '纬度',
  `file_suffixes` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '采集文件后缀(逗号分隔,如:txt,ocean,rain)',
  `auto_sync` tinyint NULL DEFAULT 0 COMMENT '是否自动同步: 0-否, 1-是',
  `sync_interval_minutes` int NULL DEFAULT 30 COMMENT '同步间隔(分钟)',
  `last_sync_time` datetime NULL DEFAULT NULL COMMENT '最后同步时间',
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展信息（JSON格式）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_user` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_user` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_user` bigint NULL DEFAULT NULL COMMENT '删除人ID',
  `deleted_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_source_type`(`source_type`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_deleted_flag`(`deleted_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '数据源表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for data_sync_log
-- ----------------------------
DROP TABLE IF EXISTS `data_sync_log`;
CREATE TABLE `data_sync_log`  (
  `id` bigint NOT NULL COMMENT '日志ID',
  `task_id` bigint NOT NULL COMMENT '任务ID（关联字段，非外键）',
  `sync_time` datetime NOT NULL COMMENT '同步时间',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态：SUCCESS-成功, FAILED-失败',
  `data_count` int NULL DEFAULT 0 COMMENT '同步数据条数',
  `duration` int NULL DEFAULT NULL COMMENT '耗时（毫秒）',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
  `log_detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '日志详情（JSON格式）',
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展信息（JSON格式）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_user` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_user` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_user` bigint NULL DEFAULT NULL COMMENT '删除人ID',
  `deleted_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_id`(`task_id`) USING BTREE,
  INDEX `idx_sync_time`(`sync_time`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_deleted_flag`(`deleted_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '数据同步日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for data_sync_task
-- ----------------------------
DROP TABLE IF EXISTS `data_sync_task`;
CREATE TABLE `data_sync_task`  (
  `id` bigint NOT NULL COMMENT '任务ID',
  `task_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称',
  `data_source_id` bigint NOT NULL COMMENT '数据源ID（关联字段，非外键）',
  `task_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务类型：AUTO-自动同步, MANUAL-手动同步',
  `sync_frequency` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '同步频率：HOURLY-每小时, DAILY-每天, WEEKLY-每周',
  `cron_expression` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Cron表达式',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'STOPPED' COMMENT '状态：RUNNING-运行中, STOPPED-已停止, PAUSED-已暂停',
  `last_sync_time` datetime NULL DEFAULT NULL COMMENT '最后同步时间',
  `next_sync_time` datetime NULL DEFAULT NULL COMMENT '下次同步时间',
  `sync_count` int NOT NULL DEFAULT 0 COMMENT '同步次数',
  `success_count` int NOT NULL DEFAULT 0 COMMENT '成功次数',
  `fail_count` int NOT NULL DEFAULT 0 COMMENT '失败次数',
  `config_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '任务配置（JSON格式）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展信息（JSON格式）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_user` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_user` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_user` bigint NULL DEFAULT NULL COMMENT '删除人ID',
  `deleted_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_data_source_id`(`data_source_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_deleted_flag`(`deleted_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '数据同步任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for expert_answer
-- ----------------------------
DROP TABLE IF EXISTS `expert_answer`;
CREATE TABLE `expert_answer`  (
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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '专家回答表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for forum_comment
-- ----------------------------
DROP TABLE IF EXISTS `forum_comment`;
CREATE TABLE `forum_comment`  (
  `id` bigint NOT NULL COMMENT '评论ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID（关联forum_post表，关联字段，非外键）',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评论内容',
  `user_id` bigint NOT NULL COMMENT '评论人ID（关联sys_user表，关联字段，非外键）',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '评论人用户名（冗余字段，便于查询）',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父评论ID（用于多级回复，0表示顶级评论）',
  `reply_to_user_id` bigint NULL DEFAULT NULL COMMENT '回复的用户ID（如果是对某个用户的回复）',
  `reply_to_user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '回复的用户名（冗余字段）',
  `like_count` int NOT NULL DEFAULT 0 COMMENT '点赞数',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0-正常, 1-已删除, 2-已屏蔽',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_user` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_user` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_user` bigint NULL DEFAULT NULL COMMENT '删除人ID',
  `deleted_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除, 1-已删除',
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展信息（JSON格式）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_post_id`(`post_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id`) USING BTREE,
  INDEX `idx_created_time`(`created_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '论坛评论表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for forum_post
-- ----------------------------
DROP TABLE IF EXISTS `forum_post`;
CREATE TABLE `forum_post`  (
  `id` bigint NOT NULL COMMENT '帖子ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '帖子标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '帖子内容',
  `author_id` bigint NOT NULL COMMENT '作者ID（关联sys_user表，关联字段，非外键）',
  `author_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '作者用户名（冗余字段，便于查询）',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'GENERAL' COMMENT '分类：GENERAL-普通讨论, QUESTION-问题求助, SHARE-经验分享, NEWS-新闻资讯',
  `post_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'TOPIC_DISCUSSION' COMMENT '帖子类型：TOPIC_DISCUSSION-主题讨论, DATA_ANALYSIS-数据分析',
  `analysis_target` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分析目标：如海况预测、波浪风险、潮汐变化',
  `reliability_status` tinyint NOT NULL DEFAULT 0 COMMENT '可靠性状态：0-未评选, 1-评选中, 2-已认证',
  `reliability_score` decimal(5, 2) NOT NULL DEFAULT 0.00 COMMENT '可靠性综合评分（0-100）',
  `evaluation_count` int NOT NULL DEFAULT 0 COMMENT '专家评估次数',
  `tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标签（JSON数组格式，如：[\"海洋\", \"波浪\", \"数据\"]）',
  `view_count` int NOT NULL DEFAULT 0 COMMENT '浏览量',
  `like_count` int NOT NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` int NOT NULL DEFAULT 0 COMMENT '评论数',
  `is_top` tinyint NOT NULL DEFAULT 0 COMMENT '是否置顶：0-否, 1-是',
  `is_essence` tinyint NOT NULL DEFAULT 0 COMMENT '是否精华：0-否, 1-是',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-草稿, 1-已发布, 2-已关闭',
  `allow_comment` tinyint NOT NULL DEFAULT 1 COMMENT '是否允许评论：0-否, 1-是',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_user` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_user` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_user` bigint NULL DEFAULT NULL COMMENT '删除人ID',
  `deleted_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除, 1-已删除',
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展信息（JSON格式）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_author_id`(`author_id`) USING BTREE,
  INDEX `idx_category`(`category`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_created_time`(`created_time`) USING BTREE,
  INDEX `idx_post_type`(`post_type`) USING BTREE,
  INDEX `idx_reliability_status`(`reliability_status`) USING BTREE,
  INDEX `idx_reliability_score`(`reliability_score`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '论坛帖子表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for forum_post_attachment
-- ----------------------------
DROP TABLE IF EXISTS `forum_post_attachment`;
CREATE TABLE `forum_post_attachment`  (
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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '论坛帖子附件表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for forum_post_evaluation
-- ----------------------------
DROP TABLE IF EXISTS `forum_post_evaluation`;
CREATE TABLE `forum_post_evaluation`  (
  `id` bigint NOT NULL COMMENT '评估ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID（关联forum_post表，关联字段，非外键）',
  `evaluator_id` bigint NOT NULL COMMENT '评估人ID（关联sys_user表，关联字段，非外键）',
  `evaluator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '评估人用户名（冗余字段）',
  `is_expert` tinyint NOT NULL DEFAULT 1 COMMENT '是否专家评估：0-否, 1-是',
  `score_accuracy` decimal(5, 2) NOT NULL DEFAULT 0.00 COMMENT '准确性评分（0-100）',
  `score_risk` decimal(5, 2) NOT NULL DEFAULT 0.00 COMMENT '风险评估评分（0-100）',
  `score_reasoning` decimal(5, 2) NOT NULL DEFAULT 0.00 COMMENT '可解释性评分（0-100）',
  `total_score` decimal(5, 2) NOT NULL DEFAULT 0.00 COMMENT '综合评分（0-100）',
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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '论坛帖子专家评估表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for forum_post_vote
-- ----------------------------
DROP TABLE IF EXISTS `forum_post_vote`;
CREATE TABLE `forum_post_vote`  (
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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '论坛帖子投票表（可靠海况评选）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for observation_data
-- ----------------------------
DROP TABLE IF EXISTS `observation_data`;
CREATE TABLE `observation_data`  (
  `id` bigint NOT NULL COMMENT '数据ID',
  `data_source_id` bigint NOT NULL COMMENT '数据源ID（第三方API数据源，关联字段，非外键）',
  `data_type_id` bigint NOT NULL COMMENT '数据类型ID（关联字段，非外键）',
  `observation_time` datetime NOT NULL COMMENT '观测时间',
  `data_value` decimal(20, 6) NOT NULL COMMENT '数据值',
  `longitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '经度（-180到180，从API获取）',
  `latitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '纬度（-90到90，从API获取）',
  `depth` decimal(10, 2) NULL DEFAULT NULL COMMENT '深度（米，如果是剖面数据）',
  `quality_flag` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '质量标志：GOOD-良好, QUESTIONABLE-可疑, BAD-坏',
  `source_file_id` bigint NULL DEFAULT NULL COMMENT '来源文件ID（如果是从文件解析，关联字段，非外键）',
  `api_data_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'API返回的原始数据ID',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展信息（JSON格式）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_user` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_user` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_user` bigint NULL DEFAULT NULL COMMENT '删除人ID',
  `deleted_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_api_data_id`(`api_data_id`) USING BTREE,
  INDEX `idx_data_source_time`(`data_source_id`, `observation_time`) USING BTREE,
  INDEX `idx_data_type`(`data_type_id`) USING BTREE,
  INDEX `idx_observation_time`(`observation_time`) USING BTREE,
  INDEX `idx_location`(`longitude`, `latitude`) USING BTREE,
  INDEX `idx_source_file_id`(`source_file_id`) USING BTREE,
  INDEX `idx_deleted_flag`(`deleted_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '观测数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for observation_data_type
-- ----------------------------
DROP TABLE IF EXISTS `observation_data_type`;
CREATE TABLE `observation_data_type`  (
  `id` bigint NOT NULL COMMENT '数据类型ID',
  `type_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型编码',
  `type_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型名称',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '单位',
  `data_format` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '数据格式：FLOAT-浮点数, INTEGER-整数',
  `min_value` decimal(20, 6) NULL DEFAULT NULL COMMENT '最小值',
  `max_value` decimal(20, 6) NULL DEFAULT NULL COMMENT '最大值',
  `rate_of_change_limit` decimal(20, 6) NULL DEFAULT NULL COMMENT '变化率阈值(每小时)',
  `spike_threshold` decimal(20, 6) NULL DEFAULT NULL COMMENT '尖峰检测阈值(与均值偏差倍数)',
  `persistence_limit` int NULL DEFAULT 5 COMMENT '持续性检测阈值(连续相同值数量)',
  `noaa_variable_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'NOAA文件中的变量名(如WSPD,ATMP,SAL等)',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展信息（JSON格式）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_user` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_user` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_user` bigint NULL DEFAULT NULL COMMENT '删除人ID',
  `deleted_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_type_code`(`type_code`) USING BTREE,
  INDEX `idx_deleted_flag`(`deleted_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '观测数据类型表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for scene_data_relation
-- ----------------------------
DROP TABLE IF EXISTS `scene_data_relation`;
CREATE TABLE `scene_data_relation`  (
  `id` bigint NOT NULL COMMENT '关联ID',
  `scene_id` bigint NOT NULL COMMENT '场景ID（关联字段，非外键）',
  `data_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据类型：DATA_SOURCE-数据源, DATA-观测数据, FILE-文件',
  `data_id` bigint NOT NULL COMMENT '数据ID（关联字段，非外键）',
  `display_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '显示配置（JSON格式）',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序顺序',
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展信息（JSON格式）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_user` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_user` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_user` bigint NULL DEFAULT NULL COMMENT '删除人ID',
  `deleted_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_scene_id`(`scene_id`) USING BTREE,
  INDEX `idx_data`(`data_type`, `data_id`) USING BTREE,
  INDEX `idx_deleted_flag`(`deleted_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '场景数据关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码（加密）',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'user' COMMENT '角色：admin-管理员, user-用户',
  `expert_title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '专家头衔（专家用户可填）',
  `expert_field` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '专家领域（如海况预测、波浪风险）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-禁用, 1-启用',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展信息（JSON格式）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_user` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_user` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_user` bigint NULL DEFAULT NULL COMMENT '删除人ID',
  `deleted_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username`) USING BTREE,
  INDEX `idx_role`(`role`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_deleted_flag`(`deleted_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for system_config
-- ----------------------------
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config`  (
  `id` bigint NOT NULL COMMENT '配置ID',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置键',
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '配置值',
  `config_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '配置类型：STRING-字符串, NUMBER-数字, JSON-JSON对象, BOOLEAN-布尔值',
  `config_group` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '配置分组',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `is_system` tinyint NOT NULL DEFAULT 0 COMMENT '是否系统配置：0-用户配置, 1-系统配置',
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展信息（JSON格式）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_user` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_user` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_user` bigint NULL DEFAULT NULL COMMENT '删除人ID',
  `deleted_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key`) USING BTREE,
  INDEX `idx_config_group`(`config_group`) USING BTREE,
  INDEX `idx_deleted_flag`(`deleted_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for visualization_scene
-- ----------------------------
DROP TABLE IF EXISTS `visualization_scene`;
CREATE TABLE `visualization_scene`  (
  `id` bigint NOT NULL COMMENT '场景ID',
  `scene_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '场景名称',
  `scene_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '场景类型：3D_OCEAN-3D海洋, 2D_CHART-2D图表, COMPOSITE-复合场景',
  `user_id` bigint NULL DEFAULT NULL COMMENT '创建用户ID（关联字段，非外键）',
  `is_public` tinyint NOT NULL DEFAULT 0 COMMENT '是否公开：0-私有, 1-公开',
  `config_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '场景配置（JSON格式，包含Three.js场景配置）',
  `thumbnail` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '缩略图URL',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `view_count` int NOT NULL DEFAULT 0 COMMENT '查看次数',
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展信息（JSON格式）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_user` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `updated_user` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_user` bigint NULL DEFAULT NULL COMMENT '删除人ID',
  `deleted_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_scene_type`(`scene_type`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_is_public`(`is_public`) USING BTREE,
  INDEX `idx_deleted_flag`(`deleted_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '可视化场景表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
