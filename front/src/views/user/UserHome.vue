<template>
  <div class="user-home">
    <!-- Hero: 编辑式开场 -->
    <section class="hero">
      <div class="hero-eyebrow">
        <span class="dash"></span>
        <span>OCEAN INTELLIGENCE PLATFORM</span>
        <span class="dash"></span>
      </div>
      <h1 class="hero-title">
        <span class="ht-line">海洋数据</span>
        <span class="ht-line italic">可视化</span>
      </h1>
      <p class="hero-subtitle">实时观测 · 三维可视 · 智能决策</p>
      <div class="hero-divider"></div>
    </section>

    <!-- 统计：三块大牌，纸面+发丝线 -->
    <section class="stats">
      <article class="stat-card">
        <div class="stat-row">
          <span class="stat-tag">01 · SCENES</span>
          <Icon name="scene" :size="16" color="#0284c7" />
        </div>
        <div class="stat-value">{{ formatNumber(stats.sceneCount) }}</div>
        <div class="stat-label">可视化场景</div>
      </article>

      <article class="stat-card">
        <div class="stat-row">
          <span class="stat-tag">02 · CHARTS</span>
          <Icon name="chart" :size="16" color="#0284c7" />
        </div>
        <div class="stat-value">{{ formatNumber(stats.chartCount) }}</div>
        <div class="stat-label">图表配置</div>
      </article>

      <article class="stat-card highlight">
        <div class="stat-row">
          <span class="stat-tag">03 · OBSERVATIONS</span>
          <Icon name="signal" :size="16" color="#0284c7" />
        </div>
        <div class="stat-value">{{ formatNumber(stats.dataCount) }}</div>
        <div class="stat-label">观测数据条目</div>
        <div class="stat-pulse"></div>
      </article>
    </section>

    <!-- 特性卡：编辑式三栏 -->
    <section class="features">
      <article class="feature-card">
        <div class="feat-num">A</div>
        <div class="feat-icon"><Icon name="scene" :size="22" color="#0284c7" /></div>
        <h3>三维海洋场景</h3>
        <p>基于 WebGL 与 Three.js，把 NDBC 浮标实测的有效波高、周期、波向，落地为可漫游的 3D 海面。</p>
        <router-link to="/user/scene-gallery" class="feat-link">
          <span>浏览场景</span>
          <Icon name="external" :size="13" />
        </router-link>
      </article>

      <article class="feature-card">
        <div class="feat-num">B</div>
        <div class="feat-icon"><Icon name="bars" :size="22" color="#0284c7" /></div>
        <h3>多维图表分析</h3>
        <p>ECharts 驱动的折线、热力、雷达组合，支持同站点跨时序与多站点同时序两种维度对比。</p>
        <router-link to="/user/chart-gallery" class="feat-link">
          <span>浏览图表</span>
          <Icon name="external" :size="13" />
        </router-link>
      </article>

      <article class="feature-card">
        <div class="feat-num">C</div>
        <div class="feat-icon"><Icon name="brain" :size="22" color="#0284c7" /></div>
        <h3>AI 辅助决策</h3>
        <p>结合 Pierson-Moskowitz 谱与 DeepSeek 大模型，给出未来 24h 海况预报与作业可行性建议。</p>
        <router-link to="/user/ocean-analysis" class="feat-link">
          <span>查看决策</span>
          <Icon name="external" :size="13" />
        </router-link>
      </article>
    </section>

    <!-- 页脚标识 -->
    <footer class="page-foot">
      <div class="foot-left">
        <span class="foot-mark"></span>
        <span>OCEAN/VISION · GRADUATION PROJECT · 2026</span>
      </div>
      <div class="foot-right">
        <span>NDBC · ERDDAP · DeepSeek</span>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { userApi } from '@/utils/api-user';
import Icon from '@/components/Icon.vue';

const stats = ref({
  sceneCount: 0,
  chartCount: 0,
  dataCount: 0,
});

const formatNumber = (n: number) => {
  if (!Number.isFinite(n)) return '0';
  return n.toLocaleString('en-US');
};

const loadStats = async () => {
  try {
    const res = await userApi.getUserStats();
    stats.value = {
      sceneCount: Number(res.sceneCount) || 0,
      chartCount: Number(res.chartCount) || 0,
      dataCount: Number(res.dataCount) || 0,
    };
  } catch (error: any) {
    console.error('加载统计数据失败:', error);
    stats.value = { sceneCount: 0, chartCount: 0, dataCount: 0 };
  }
};

onMounted(() => { loadStats(); });
</script>

<style scoped lang="less">
@bg-base:    #f4f7fa;
@bg-tint:    #ecf3f8;
@paper:      #ffffff;
@hairline:   #e2e8f0;
@accent:     #0284c7;
@accent-soft:#7dd3fc;
@accent-cyan:#06b6d4;
@ink-1:      #0f172a;
@ink-2:      #334155;
@ink-3:      #64748b;
@ink-4:      #94a3b8;

.user-home {
  width: 100%;
  position: relative;
  z-index: 1;
  animation: pageIn 0.7s cubic-bezier(0.2, 0.8, 0.2, 1);
}

@keyframes pageIn {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}

// ============ HERO ============
.hero {
  text-align: center;
  padding: 64px 0 48px;
  margin-bottom: 48px;
  position: relative;
}

.hero::before {
  content: '';
  position: absolute;
  top: -80px;
  left: 50%;
  transform: translateX(-50%);
  width: 800px;
  height: 600px;
  background:
    radial-gradient(ellipse at 50% 50%, rgba(125, 211, 252, 0.18) 0%, transparent 70%);
  pointer-events: none;
  z-index: -1;
}

.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  font-family: 'Inter', -apple-system, sans-serif;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 3px;
  color: @accent;
  text-transform: uppercase;

  .dash {
    width: 24px;
    height: 1px;
    background: @accent;
    opacity: 0.4;
  }
}

.hero-title {
  font-family: 'Inter', -apple-system, sans-serif;
  font-size: 64px;
  font-weight: 800;
  line-height: 1.05;
  color: @ink-1;
  letter-spacing: -2px;
  margin-bottom: 20px;

  .ht-line {
    display: block;
    animation: lineIn 0.9s cubic-bezier(0.2, 0.8, 0.2, 1) both;
  }
  .ht-line:nth-child(2) {
    animation-delay: 0.12s;
    background: linear-gradient(135deg, #0284c7 0%, #06b6d4 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    font-weight: 800;
    font-style: normal;
  }
}

@keyframes lineIn {
  from { opacity: 0; transform: translateY(20px); }
  to   { opacity: 1; transform: translateY(0); }
}

.hero-subtitle {
  font-size: 15px;
  color: @ink-3;
  letter-spacing: 1.5px;
  font-family: 'Inter', sans-serif;
  font-weight: 400;
}

.hero-divider {
  width: 48px;
  height: 3px;
  background: linear-gradient(90deg, @accent, @accent-cyan);
  border-radius: 2px;
  margin: 32px auto 0;
}

// ============ STATS ============
.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
  margin-bottom: 64px;
}

.stat-card {
  padding: 28px 26px 30px;
  background: @paper;
  border: 1px solid @hairline;
  border-radius: 14px;
  position: relative;
  transition: all 0.3s ease;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);
  overflow: hidden;

  &:hover {
    border-color: @accent;
    transform: translateY(-2px);
    box-shadow: 0 14px 32px -10px rgba(2, 132, 199, 0.18);
  }

  &.highlight {
    background: linear-gradient(135deg, #ecf6fe 0%, #ffffff 100%);
    border-color: rgba(2, 132, 199, 0.3);

    .stat-value {
      background: linear-gradient(135deg, #0284c7 0%, #06b6d4 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
  }
}

.stat-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.stat-tag {
  font-family: 'Inter', sans-serif;
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 1.4px;
  color: @ink-4;
  text-transform: uppercase;
}

.stat-value {
  font-family: 'Inter', sans-serif;
  font-size: 42px;
  font-weight: 800;
  color: @ink-1;
  line-height: 1;
  letter-spacing: -1.5px;
  margin-bottom: 6px;
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: 13px;
  color: @ink-3;
  letter-spacing: 0.2px;
  font-weight: 500;
}

.stat-pulse {
  position: absolute;
  top: 32px;
  right: 28px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.55);
  animation: pulse 2.4s ease-out infinite;
}
@keyframes pulse {
  0%   { box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.5); }
  70%  { box-shadow: 0 0 0 14px rgba(34, 197, 94, 0); }
  100% { box-shadow: 0 0 0 0 rgba(34, 197, 94, 0); }
}

// ============ FEATURES ============
.features {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 22px;
  margin-bottom: 64px;
}

.feature-card {
  position: relative;
  padding: 32px 28px 28px;
  background: @paper;
  border: 1px solid @hairline;
  border-radius: 14px;
  transition: transform 0.3s cubic-bezier(0.2, 0.8, 0.2, 1),
              box-shadow 0.3s cubic-bezier(0.2, 0.8, 0.2, 1),
              border-color 0.3s ease;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);

  &:hover {
    transform: translateY(-4px);
    border-color: @accent;
    box-shadow: 0 18px 40px -12px rgba(2, 132, 199, 0.22);

    .feat-num { color: @accent; transform: scale(1.1); }
    .feat-link { color: @accent; }
    .feat-link .icon { transform: translate(3px, -3px); }
  }
}

.feat-num {
  position: absolute;
  top: 22px;
  right: 24px;
  font-family: 'Inter', sans-serif;
  font-weight: 800;
  font-size: 22px;
  color: @ink-4;
  transition: all 0.3s ease;
  letter-spacing: -1px;
}

.feat-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, rgba(2, 132, 199, 0.08), rgba(6, 182, 212, 0.08));
  border: 1px solid rgba(2, 132, 199, 0.18);
  border-radius: 12px;
  margin-bottom: 18px;
}

.feature-card h3 {
  font-family: 'Inter', sans-serif;
  font-size: 18px;
  font-weight: 700;
  color: @ink-1;
  letter-spacing: -0.3px;
  margin-bottom: 10px;
}

.feature-card p {
  font-size: 13.5px;
  color: @ink-3;
  line-height: 1.65;
  margin-bottom: 20px;
  min-height: 66px;
}

.feat-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: 'Inter', sans-serif;
  font-size: 12.5px;
  font-weight: 600;
  letter-spacing: 0.3px;
  color: @ink-2;
  text-decoration: none;
  padding: 12px 0 0;
  border-top: 1px solid @hairline;
  width: 100%;
  transition: color 0.25s ease;

  .icon {
    transition: transform 0.25s cubic-bezier(0.2, 0.8, 0.2, 1);
  }
}

// ============ FOOT ============
.page-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 0 8px;
  border-top: 1px solid @hairline;
  font-family: 'Inter', sans-serif;
  font-size: 11.5px;
  font-weight: 500;
  letter-spacing: 0.8px;
  color: @ink-4;
  text-transform: uppercase;
}

.foot-left {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.foot-mark {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: @accent;
  box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.18);
  display: inline-block;
}

// ============ RESPONSIVE ============
@media (max-width: 1024px) {
  .hero { padding: 50px 0 40px; }
  .hero-title { font-size: 52px; }
  .features { grid-template-columns: 1fr 1fr; }
}

@media (max-width: 768px) {
  .hero { padding: 36px 0 30px; }
  .hero-title { font-size: 38px; letter-spacing: -1px; }
  .hero-subtitle { font-size: 13px; letter-spacing: 1px; }
  .stats { grid-template-columns: 1fr; gap: 14px; }
  .features { grid-template-columns: 1fr; }
  .stat-value { font-size: 36px; }
  .page-foot { flex-direction: column; gap: 8px; align-items: flex-start; }
}
</style>
