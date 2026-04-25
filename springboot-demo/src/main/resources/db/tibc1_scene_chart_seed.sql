-- =============================================================================
-- TIBC1（Tiburon Pier, San Francisco Bay）：visualization_scene + chart_config
-- 对齐：Scene3DViewer.vue（BUOY_MARKER / VECTOR_ARROW / OCEAN_COLUMN / WATER_QUALITY_HALO）
--       Chart2DViewer.vue（dataset 行键 = observation_data.data_type_code，与 encode.y 一致）
-- 数据样本：TIBC1.txt（WDIR/WSPD/PRES/ATMP…）、TIBC1.ocean.txt（DEPTH/OTMP/SAL/O2PPM/CLCON/TURB/PH）
-- 站位：NDBC 公布约 37.892°N, 122.447°W；config 中 position / camera.target 使用 [lat, lon]
-- =============================================================================
-- 使用前请修改：
--   1) 将 dataQuery.dataSourceId（默认 101）改为你的 TIBC1 数据源 data_source.id
--   2) startTime / endTime 覆盖你已导入观测的时间段（下列窗口覆盖样本约 2026-04-03 晚 — 2026-04-04 上午）
--   3) 导入映射建议（与 observation_data_type_seed.sql 中 type_code 一致）：
--        TIBC1.txt     WSPD → WIND_SPEED；WDIR → WIND_DIRECTION；PRES → PRESSURE；ATMP → AIR_TEMPERATURE
--        TIBC1.ocean   OTMP → TEMP；SAL → SAL；DEPTH → observation_data.depth（多深度时柱体分层）
--                      O2PPM → DO；O2% → DOXY_SATURATION；CLCON → CHL；TURB → TURBIDITY；PH → PH
--        TIBC1.txt     WTMP → SST（本段样本多为 MM，图表未默认依赖 SST）
--        TIBC1.txt     TIDE → SEA_LEVEL（本段样本多为 MM，未单独做默认潮位图）
--   4) id 为 Snowflake 风格 bigint；若与库中主键冲突请自行换号。
-- =============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 3D：浮标 + 风矢量 + 表层温盐柱 + 水质光晕（DEPTH 仅 0m 时柱体较薄为正常现象）
-- ---------------------------------------------------------------------------
INSERT INTO `visualization_scene` (
  `id`,
  `scene_name`,
  `scene_type`,
  `user_id`,
  `is_public`,
  `config_json`,
  `thumbnail`,
  `description`,
  `view_count`,
  `deleted_flag`
) VALUES (
  1990000000000000101,
  'TIBC1 站 3D 海况（浮标+风+温盐+水质）',
  '3D_OCEAN',
  NULL,
  1,
  '{"dataQuery":{"dataSourceId":2332918970660686439,"startTime":"2026-03-03 20:00:00","endTime":"2026-04-04 12:00:00"},"camera":{"target":[37.892,-122.447],"distance":650000},"layers":[{"type":"BUOY_MARKER","position":[37.892,-122.447]},{"type":"VECTOR_ARROW","position":[37.892,-122.447],"dataBindings":{"speedTypeCode":"WIND_SPEED","directionTypeCode":"WIND_DIRECTION"},"style":{"minSpeed":0,"maxSpeed":6,"arrowScale":90000,"color":"#5eead4"}},{"type":"OCEAN_COLUMN","position":[37.892,-122.447],"dataBindings":{"temperatureTypeCode":"TEMP","salinityTypeCode":"SAL"},"style":{"heightScale":10000,"minSalinity":27,"maxSalinity":29,"colorGradientForTemperature":[{"value":13,"color":"#1d4ed8"},{"value":15.5,"color":"#22d3ee"},{"value":17,"color":"#f59e0b"}]}},{"type":"WATER_QUALITY_HALO","position":[37.892,-122.447],"dataBindings":{"oxygenTypeCode":"DO","chlorophyllTypeCode":"CHL","turbidityTypeCode":"TURBIDITY","phTypeCode":"PH"},"style":{"baseRadius":22000,"radiusByChlorophyll":true,"colorByTurbidity":true,"opacityByOxygen":true,"phColorRange":{"minPH":7.45,"maxPH":7.55,"acidColor":"#3b82f6","alkalineColor":"#fb923c"}}}]}',
  NULL,
  '绑定 WIND_SPEED/WIND_DIRECTION、TEMP/SAL、DO/CHL/TURBIDITY/PH；静风或缺测时 WDIR 为 MM 则风箭头不绘制。',
  0,
  0
);

-- ---------------------------------------------------------------------------
-- 2D 图一：OTMP 水温 + 盐度（双 Y）
-- ---------------------------------------------------------------------------
INSERT INTO `chart_config` (
  `id`,
  `chart_name`,
  `chart_type`,
  `user_id`,
  `data_query_config`,
  `echarts_config`,
  `is_public`,
  `description`,
  `deleted_flag`
) VALUES (
  1990000000000000201,
  'TIBC1 水温(OTMP)与盐度',
  'LINE',
  NULL,
  '{"dataSourceId":2332918970660686439,"typeCodes":["TEMP","SAL"],"time":{"mode":"range","defaultHours":720}}',
  '{"backgroundColor":"transparent","textStyle":{"color":"#e0e0e0"},"tooltip":{"trigger":"axis"},"legend":{"top":8,"textStyle":{"color":"#ccc"},"data":["水温 TEMP(OTMP)","盐度 SAL"]},"grid":{"left":"10%","right":"12%","bottom":"10%","top":"22%","containLabel":true},"xAxis":{"type":"time","axisLabel":{"color":"#aaa"}},"yAxis":[{"type":"value","name":"温度 ℃","nameTextStyle":{"color":"#aaa"},"axisLabel":{"color":"#aaa"},"splitLine":{"lineStyle":{"color":"rgba(255,255,255,0.08)"}}},{"type":"value","name":"盐度 psu","nameTextStyle":{"color":"#aaa"},"axisLabel":{"color":"#aaa"},"splitLine":{"show":false}}],"series":[{"name":"水温 TEMP(OTMP)","type":"line","smooth":true,"showSymbol":false,"yAxisIndex":0,"encode":{"x":"time","y":"TEMP"}},{"name":"盐度 SAL","type":"line","smooth":true,"showSymbol":false,"yAxisIndex":1,"encode":{"x":"time","y":"SAL"}}]}',
  1,
  '需已导入 OTMP→TEMP、SAL；encode 的 y 与 type_code 一致。替换 data_query_config.dataSourceId。',
  0
);

-- ---------------------------------------------------------------------------
-- 2D 图二：风速 + 气温 + 气压（三 Y 轴；WDIR 为圆周量，不宜与标量同轴折线）
-- ---------------------------------------------------------------------------
INSERT INTO `chart_config` (
  `id`,
  `chart_name`,
  `chart_type`,
  `user_id`,
  `data_query_config`,
  `echarts_config`,
  `is_public`,
  `description`,
  `deleted_flag`
) VALUES (
  1990000000000000202,
  'TIBC1 风速·气温·气压',
  'LINE',
  NULL,
  '{"dataSourceId":2332918970660686439,"typeCodes":["WIND_SPEED","AIR_TEMPERATURE","PRESSURE"],"time":{"mode":"range","defaultHours":720}}',
  '{"backgroundColor":"transparent","textStyle":{"color":"#e0e0e0"},"tooltip":{"trigger":"axis"},"legend":{"top":8,"textStyle":{"color":"#ccc"},"data":["风速 WIND_SPEED","气温 AIR_TEMPERATURE","气压 PRESSURE"]},"grid":{"left":"12%","right":"20%","bottom":"10%","top":"22%","containLabel":true},"xAxis":{"type":"time","axisLabel":{"color":"#aaa"}},"yAxis":[{"type":"value","name":"气温 ℃","position":"left","nameTextStyle":{"color":"#aaa"},"axisLabel":{"color":"#aaa"},"splitLine":{"lineStyle":{"color":"rgba(255,255,255,0.08)"}}},{"type":"value","name":"风速 m/s","position":"right","nameTextStyle":{"color":"#aaa"},"axisLabel":{"color":"#aaa"},"splitLine":{"show":false}},{"type":"value","name":"气压 hPa","position":"right","offset":58,"nameTextStyle":{"color":"#aaa"},"axisLabel":{"color":"#aaa"},"splitLine":{"show":false}}],"series":[{"name":"气温 AIR_TEMPERATURE","type":"line","smooth":true,"showSymbol":false,"yAxisIndex":0,"encode":{"x":"time","y":"AIR_TEMPERATURE"}},{"name":"风速 WIND_SPEED","type":"line","smooth":true,"showSymbol":false,"yAxisIndex":1,"encode":{"x":"time","y":"WIND_SPEED"}},{"name":"气压 PRESSURE","type":"line","smooth":true,"showSymbol":false,"yAxisIndex":2,"encode":{"x":"time","y":"PRESSURE"}}]}',
  1,
  '需已导入 WSPD→WIND_SPEED、ATMP→AIR_TEMPERATURE、PRES→PRESSURE（type_code=PRESSURE）。',
  0
);

-- ---------------------------------------------------------------------------
-- 2D 图三：水质 DO / CHL / 浊度 / pH（三 Y：DO+CHL | 浊度 | pH）
-- ---------------------------------------------------------------------------
INSERT INTO `chart_config` (
  `id`,
  `chart_name`,
  `chart_type`,
  `user_id`,
  `data_query_config`,
  `echarts_config`,
  `is_public`,
  `description`,
  `deleted_flag`
) VALUES (
  1990000000000000203,
  'TIBC1 水质过程线',
  'LINE',
  NULL,
  '{"dataSourceId":2332918970660686439,"typeCodes":["DO","CHL","TURBIDITY","PH"],"time":{"mode":"range","defaultHours":720}}',
  '{"backgroundColor":"transparent","textStyle":{"color":"#e0e0e0"},"tooltip":{"trigger":"axis"},"legend":{"top":8,"textStyle":{"color":"#ccc"},"data":["溶解氧 DO","叶绿素 CHL","浊度 TURBIDITY","pH"]},"grid":{"left":"11%","right":"22%","bottom":"10%","top":"24%","containLabel":true},"xAxis":{"type":"time","axisLabel":{"color":"#aaa"}},"yAxis":[{"type":"value","name":"DO / CHL","position":"left","nameTextStyle":{"color":"#aaa"},"axisLabel":{"color":"#aaa"},"splitLine":{"lineStyle":{"color":"rgba(255,255,255,0.08)"}}},{"type":"value","name":"浊度","position":"right","nameTextStyle":{"color":"#aaa"},"axisLabel":{"color":"#aaa"},"splitLine":{"show":false}},{"type":"value","name":"pH","position":"right","offset":56,"nameTextStyle":{"color":"#aaa"},"axisLabel":{"color":"#aaa"},"splitLine":{"show":false}}],"series":[{"name":"溶解氧 DO","type":"line","smooth":true,"showSymbol":false,"yAxisIndex":0,"encode":{"x":"time","y":"DO"}},{"name":"叶绿素 CHL","type":"line","smooth":true,"showSymbol":false,"yAxisIndex":0,"encode":{"x":"time","y":"CHL"}},{"name":"浊度 TURBIDITY","type":"line","smooth":true,"showSymbol":false,"yAxisIndex":1,"encode":{"x":"time","y":"TURBIDITY"}},{"name":"pH","type":"line","smooth":true,"showSymbol":false,"yAxisIndex":2,"encode":{"x":"time","y":"PH"}}]}',
  1,
  '需已导入 O2PPM→DO、CLCON→CHL、TURB→TURBIDITY、PH→PH。浊度文件为 FTU，与库单位 NTU 接近可按同一曲线展示。',
  0
);
