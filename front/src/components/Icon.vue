<template>
  <svg
    class="icon"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    :stroke="color"
    fill="none"
    stroke-width="1.5"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <component :is="iconComp" />
  </svg>
</template>

<script setup lang="ts">
import { computed, h } from 'vue';

const props = withDefaults(
  defineProps<{
    name: string;
    size?: number | string;
    color?: string;
  }>(),
  { size: 18, color: 'currentColor' },
);

/** 极简单线条图标库 — 替代 emoji，符合"炫酷简约"风格 */
const PATHS: Record<string, () => any> = {
  // 首页 / 罗盘
  home: () =>
    h('g', null, [
      h('circle', { cx: 12, cy: 12, r: 9 }),
      h('path', { d: 'M12 3v3M12 18v3M3 12h3M18 12h3' }),
      h('path', { d: 'M9 9l6 6M15 9l-6 6' }),
    ]),
  // 地图标记
  map: () =>
    h('g', null, [
      h('path', { d: 'M9 20l-6-3V4l6 3 6-3 6 3v13l-6-3-6 3z' }),
      h('path', { d: 'M9 7v13M15 4v13' }),
    ]),
  // 大脑/AI 决策
  brain: () =>
    h('g', null, [
      h('circle', { cx: 12, cy: 12, r: 4 }),
      h('path', {
        d: 'M12 8V4M12 16v4M8 12H4M16 12h4M7.05 7.05L4.93 4.93M16.95 16.95l2.12 2.12M7.05 16.95l-2.12 2.12M16.95 7.05l2.12-2.12',
      }),
    ]),
  // 对比/统计
  bars: () =>
    h('g', null, [
      h('path', { d: 'M4 20V10M10 20V4M16 20v-7M22 20H2' }),
    ]),
  // 论坛/对话
  chat: () =>
    h('g', null, [
      h('path', {
        d: 'M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z',
      }),
    ]),
  // 我的帖子/笔记
  edit: () =>
    h('g', null, [
      h('path', { d: 'M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7' }),
      h('path', { d: 'M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z' }),
    ]),
  // 退出
  logout: () =>
    h('g', null, [
      h('path', { d: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4' }),
      h('path', { d: 'M16 17l5-5-5-5M21 12H9' }),
    ]),
  // 数据源
  database: () =>
    h('g', null, [
      h('ellipse', { cx: 12, cy: 5, rx: 9, ry: 3 }),
      h('path', { d: 'M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5' }),
      h('path', { d: 'M3 12c0 1.66 4 3 9 3s9-1.34 9-3' }),
    ]),
  // 场景/3D 立方体
  scene: () =>
    h('g', null, [
      h('path', { d: 'M21 16V8l-9-5-9 5v8l9 5 9-5z' }),
      h('path', { d: 'M3.27 8L12 13l8.73-5M12 22V13' }),
    ]),
  // 图表
  chart: () =>
    h('g', null, [
      h('path', { d: 'M3 3v18h18' }),
      h('path', { d: 'M7 14l4-4 4 3 5-7' }),
    ]),
  // 同步刷新
  refresh: () =>
    h('g', null, [
      h('path', { d: 'M23 4v6h-6M1 20v-6h6' }),
      h('path', { d: 'M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15' }),
    ]),
  // 浪/波形
  wave: () =>
    h('g', null, [
      h('path', { d: 'M2 12c2 0 2-2 4-2s2 2 4 2 2-2 4-2 2 2 4 2 2-2 4-2' }),
      h('path', { d: 'M2 18c2 0 2-2 4-2s2 2 4 2 2-2 4-2 2 2 4 2 2-2 4-2' }),
    ]),
  // 警告
  alert: () =>
    h('g', null, [
      h('path', { d: 'M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z' }),
      h('line', { x1: 12, y1: 9, x2: 12, y2: 13 }),
      h('line', { x1: 12, y1: 17, x2: 12, y2: 17 }),
    ]),
  // 卫星/雷达
  radar: () =>
    h('g', null, [
      h('circle', { cx: 12, cy: 12, r: 9 }),
      h('circle', { cx: 12, cy: 12, r: 5 }),
      h('circle', { cx: 12, cy: 12, r: 1 }),
      h('path', { d: 'M12 3v9l6 4' }),
    ]),
  // 闪电/快速
  bolt: () =>
    h('g', null, [
      h('path', { d: 'M13 2L4 14h7l-2 8 9-12h-7l2-8z' }),
    ]),
  // 设置
  settings: () =>
    h('g', null, [
      h('circle', { cx: 12, cy: 12, r: 3 }),
      h('path', {
        d: 'M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z',
      }),
    ]),
  // 用户
  user: () =>
    h('g', null, [
      h('path', { d: 'M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2' }),
      h('circle', { cx: 12, cy: 7, r: 4 }),
    ]),
  // 加号
  plus: () => h('path', { d: 'M12 5v14M5 12h14' }),
  // 关闭
  close: () => h('path', { d: 'M18 6L6 18M6 6l12 12' }),
  // 上传
  upload: () =>
    h('g', null, [
      h('path', { d: 'M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4' }),
      h('path', { d: 'M17 8l-5-5-5 5' }),
      h('path', { d: 'M12 3v12' }),
    ]),
  // 箭头
  arrowRight: () => h('path', { d: 'M5 12h14M12 5l7 7-7 7' }),
  arrowUp: () => h('path', { d: 'M12 19V5M5 12l7-7 7 7' }),
  arrowDown: () => h('path', { d: 'M12 5v14M19 12l-7 7-7-7' }),
  // 暂停 / 播放
  pause: () =>
    h('g', null, [
      h('rect', { x: 6, y: 4, width: 4, height: 16 }),
      h('rect', { x: 14, y: 4, width: 4, height: 16 }),
    ]),
  play: () => h('polygon', { points: '5 3 19 12 5 21 5 3' }),
  // 搜索
  search: () =>
    h('g', null, [
      h('circle', { cx: 11, cy: 11, r: 7 }),
      h('line', { x1: 21, y1: 21, x2: 16.65, y2: 16.65 }),
    ]),
  // 外链
  external: () =>
    h('g', null, [
      h('path', { d: 'M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6' }),
      h('path', { d: 'M15 3h6v6M10 14L21 3' }),
    ]),
  // 信号
  signal: () =>
    h('g', null, [
      h('path', { d: 'M2 17h2v3H2zM7 14h2v6H7zM12 11h2v9h-2zM17 8h2v12h-2zM22 5h0' }),
    ]),
  // 闪烁/星
  spark: () =>
    h('g', null, [
      h('path', {
        d: 'M12 3l1.6 4.6L18 9l-4.4 1.6L12 15l-1.6-4.4L6 9l4.4-1.4L12 3zM5 17l.7 1.8L7.5 19.5l-1.8.7L5 22l-.7-1.8L2.5 19.5l1.8-.7L5 17zM19 14l.5 1.4L21 16l-1.5.5L19 18l-.5-1.5L17 16l1.5-.6L19 14z',
      }),
    ]),
  // 眼睛/查看
  eye: () =>
    h('g', null, [
      h('path', { d: 'M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z' }),
      h('circle', { cx: 12, cy: 12, r: 3 }),
    ]),
  // 爱心
  heart: () =>
    h('path', {
      d: 'M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z',
    }),
  // 删除/垃圾桶
  trash: () =>
    h('g', null, [
      h('path', { d: 'M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6' }),
      h('path', { d: 'M10 11v6M14 11v6' }),
    ]),
  // 置顶/锚
  pin: () =>
    h('g', null, [
      h('path', { d: 'M12 17v5M12 2v15M5 9l7-7 7 7M9 17h6' }),
    ]),
  // 附件
  attach: () =>
    h('path', {
      d: 'M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48',
    }),
  // 仪表盘
  dashboard: () =>
    h('g', null, [
      h('rect', { x: 3, y: 3, width: 7, height: 9, rx: 1 }),
      h('rect', { x: 14, y: 3, width: 7, height: 5, rx: 1 }),
      h('rect', { x: 14, y: 12, width: 7, height: 9, rx: 1 }),
      h('rect', { x: 3, y: 16, width: 7, height: 5, rx: 1 }),
    ]),
  // 检查/对勾
  check: () => h('path', { d: 'M5 12l5 5L20 7' }),
  // 全球网络
  globe: () =>
    h('g', null, [
      h('circle', { cx: 12, cy: 12, r: 9 }),
      h('path', { d: 'M3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18' }),
    ]),
  // 趋势 (折线下降)
  trendDown: () =>
    h('g', null, [
      h('path', { d: 'M3 6l7 8 4-4 7 8' }),
      h('path', { d: 'M14 18h7v-7' }),
    ]),
  // 列表
  list: () =>
    h('g', null, [
      h('path', { d: 'M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01' }),
    ]),
};

const iconComp = computed(() => ({
  render: () => (PATHS[props.name] ? PATHS[props.name]() : null),
}));
</script>

<style scoped>
.icon {
  display: inline-block;
  vertical-align: middle;
  flex-shrink: 0;
}
</style>
