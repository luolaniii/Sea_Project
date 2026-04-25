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
          <Icon name="scene" :size="16" color="#8d8576" />
        </div>
        <div class="stat-value">{{ formatNumber(stats.sceneCount) }}</div>
        <div class="stat-label">可视化场景</div>
      </article>

      <article class="stat-card">
        <div class="stat-row">
          <span class="stat-tag">02 · CHARTS</span>
          <Icon name="chart" :size="16" color="#8d8576" />
        </div>
        <div class="stat-value">{{ formatNumber(stats.chartCount) }}</div>
        <div class="stat-label">图表配置</div>
      </article>

      <article class="stat-card highlight">
        <div class="stat-row">
          <span class="stat-tag">03 · OBSERVATIONS</span>
          <Icon name="signal" :size="16" color="#8d8576" />
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
        <div class="feat-icon"><Icon name="scene" :size="22" color="#3a342c" /></div>
        <h3>三维海洋场景</h3>
        <p>基于 WebGL 与 Three.js，把 NDBC 浮标实测的有效波高、周期、波向，落地为可漫游的 3D 海面。</p>
        <router-link to="/user/scene-gallery" class="feat-link">
          <span>浏览场景</span>
          <Icon name="external" :size="13" />
        </router-link>
      </article>

      <article class="feature-card">
        <div class="feat-num">B</div>
        <div class="feat-icon"><Icon name="bars" :size="22" color="#3a342c" /></div>
        <h3>多维图表分析</h3>
        <p>ECharts 驱动的折线、热力、雷达组合，支持同站点跨时序与多站点同时序两种维度对比。</p>
        <router-link to="/user/chart-gallery" class="feat-link">
          <span>浏览图表</span>
          <Icon name="external" :size="13" />
        </router-link>
      </article>

      <article class="feature-card">
        <div class="feat-num">C</div>
        <div class="feat-icon"><Icon name="brain" :size="22" color="#3a342c" /></div>
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
@cream-base: #f1eadc;
@cream-tint: #e8e0cf;
@paper:      #fbf7ee;
@hairline:   #d8cebd;
@taupe-soft: #c2b8a6;
@taupe:      #8d8576;
@ink-1:      #1c1814;
@ink-2:      #3a342c;
@ink-3:      #6e6759;
@ink-4:      #a39c8c;

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
  padding: 70px 0 56px;
  margin-bottom: 48px;
}

.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 28px;
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
  font-size: 10.5px;
  letter-spacing: 3.2px;
  color: @ink-3;
  text-transform: uppercase;

  .dash {
    width: 28px;
    height: 1px;
    background: @taupe-soft;
  }
}

.hero-title {
  font-family: 'Cormorant Garamond', 'Noto Serif SC', 'Songti SC', 'STSong', serif;
  font-size: 92px;
  font-weight: 400;
  line-height: 0.95;
  color: @ink-1;
  letter-spacing: -1.5px;
  margin-bottom: 24px;

  .ht-line {
    display: block;
    animation: lineIn 0.9s cubic-bezier(0.2, 0.8, 0.2, 1) both;
  }
  .ht-line:nth-child(2) {
    animation-delay: 0.12s;
  }
  .italic {
    font-style: italic;
    color: @ink-3;
    letter-spacing: 1px;
  }
}

@keyframes lineIn {
  from { opacity: 0; transform: translateY(20px); }
  to   { opacity: 1; transform: translateY(0); }
}

.hero-subtitle {
  font-size: 14px;
  color: @ink-3;
  letter-spacing: 5px;
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
  text-transform: uppercase;
}

.hero-divider {
  width: 60px;
  height: 1px;
  background: @ink-2;
  margin: 36px auto 0;
}

// ============ STATS ============
.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0;
  margin-bottom: 80px;
  border-top: 1px solid @hairline;
  border-bottom: 1px solid @hairline;
}

.stat-card {
  padding: 32px 28px 36px;
  background: transparent;
  border-right: 1px solid @hairline;
  position: relative;
  transition: background 0.3s ease;

  &:last-child {
    border-right: none;
  }

  &:hover {
    background: rgba(232, 224, 207, 0.4);
  }

  &.highlight {
    background: @paper;

    &:hover {
      background: @paper;
    }
  }
}

.stat-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.stat-tag {
  font-family: 'JetBrains Mono', monospace;
  font-size: 10.5px;
  letter-spacing: 1.6px;
  color: @taupe;
  text-transform: uppercase;
}

.stat-value {
  font-family: 'Cormorant Garamond', 'Noto Serif SC', serif;
  font-size: 56px;
  font-weight: 400;
  color: @ink-1;
  line-height: 1;
  letter-spacing: -0.5px;
  margin-bottom: 10px;
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: 13px;
  color: @ink-3;
  letter-spacing: 0.5px;
}

.stat-pulse {
  position: absolute;
  top: 32px;
  right: 28px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #6f8a6a;
  box-shadow: 0 0 0 0 rgba(111, 138, 106, 0.6);
  animation: pulse 2.4s ease-out infinite;
}
@keyframes pulse {
  0%   { box-shadow: 0 0 0 0 rgba(111, 138, 106, 0.5); }
  70%  { box-shadow: 0 0 0 14px rgba(111, 138, 106, 0); }
  100% { box-shadow: 0 0 0 0 rgba(111, 138, 106, 0); }
}

// ============ FEATURES ============
.features {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 28px;
  margin-bottom: 80px;
}

.feature-card {
  position: relative;
  padding: 36px 30px 32px;
  background: @paper;
  border: 1px solid @hairline;
  border-radius: 4px;
  transition: transform 0.32s cubic-bezier(0.2, 0.8, 0.2, 1),
              box-shadow 0.32s cubic-bezier(0.2, 0.8, 0.2, 1),
              border-color 0.3s ease;

  &:hover {
    transform: translateY(-4px);
    border-color: @taupe-soft;
    box-shadow: 0 24px 48px -16px rgba(60, 50, 40, 0.18);

    .feat-num { color: @ink-1; }
    .feat-link { color: @ink-1; }
    .feat-link .icon { transform: translate(2px, -2px); }
  }
}

.feat-num {
  position: absolute;
  top: 22px;
  right: 26px;
  font-family: 'Cormorant Garamond', serif;
  font-style: italic;
  font-size: 28px;
  color: @taupe-soft;
  transition: color 0.3s ease;
}

.feat-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border: 1px solid @hairline;
  border-radius: 50%;
  margin-bottom: 22px;
}

.feature-card h3 {
  font-family: 'Cormorant Garamond', 'Noto Serif SC', serif;
  font-size: 26px;
  font-weight: 500;
  color: @ink-1;
  letter-spacing: 0.5px;
  margin-bottom: 14px;
}

.feature-card p {
  font-size: 13.5px;
  color: @ink-3;
  line-height: 1.75;
  margin-bottom: 24px;
  min-height: 70px;
}

.feat-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 11.5px;
  letter-spacing: 1.2px;
  text-transform: uppercase;
  color: @ink-2;
  text-decoration: none;
  padding: 8px 0;
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
  padding: 22px 0 8px;
  border-top: 1px solid @hairline;
  font-family: 'JetBrains Mono', monospace;
  font-size: 10.5px;
  letter-spacing: 1.6px;
  color: @taupe;
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
  background: @ink-2;
  display: inline-block;
}

// ============ RESPONSIVE ============
@media (max-width: 1024px) {
  .hero { padding: 50px 0 40px; }
  .hero-title { font-size: 68px; }
  .features { grid-template-columns: 1fr 1fr; }
}

@media (max-width: 768px) {
  .hero { padding: 36px 0 30px; }
  .hero-title { font-size: 48px; }
  .hero-subtitle { font-size: 12px; letter-spacing: 3px; }
  .stats { grid-template-columns: 1fr; }
  .stat-card { border-right: none; border-bottom: 1px solid @hairline; }
  .stat-card:last-child { border-bottom: none; }
  .features { grid-template-columns: 1fr; }
  .stat-value { font-size: 44px; }
  .page-foot { flex-direction: column; gap: 8px; align-items: flex-start; }
}
</style>
