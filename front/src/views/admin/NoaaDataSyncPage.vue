<template>
  <div class="noaa-data-sync-page">
    <PageHeader title="NOAA 数据同步">
      <template #extra>
        <button class="btn btn-default" @click="handleSyncAll" :disabled="syncingAll">
          {{ syncingAll ? '同步中...' : '同步所有站点' }}
        </button>
      </template>
    </PageHeader>

    <div class="content-wrapper">
      <!-- 同步模式选择 -->
      <div class="card">
        <div class="mode-tabs">
          <button
            class="mode-btn"
            :class="{ active: activeMode === 'upload' }"
            @click="activeMode = 'upload'"
          >
            文件上传
          </button>
          <button
            class="mode-btn"
            :class="{ active: activeMode === 'auto' }"
            @click="activeMode = 'auto'"
          >
            HTTP 自动采集
          </button>
        </div>
      </div>

      <!-- 文件上传模式 -->
      <template v-if="activeMode === 'upload'">
        <div class="card">
          <h3 class="card-title">基础配置</h3>
          <div class="form-grid">
            <div class="form-item">
              <label>数据源 *</label>
              <select v-model="form.dataSourceId" required>
                <option value="">请选择数据源</option>
                <option v-for="source in dataSources" :key="String(source.id)" :value="source.id != null ? String(source.id) : ''">
                  {{ source.sourceName }} ({{ source.sourceType }})
                </option>
              </select>
            </div>
            <div class="form-item">
              <label>观测数据类型 *</label>
              <select v-model="form.dataTypeCode" required>
                <option value="">请选择数据类型</option>
                <option v-for="t in dataTypes" :key="t.id" :value="t.typeCode">
                  {{ t.typeCode }} - {{ t.typeName }}
                </option>
              </select>
            </div>
          </div>
        </div>

        <div class="card">
          <h3 class="card-title">文件上传</h3>
          <div class="form-grid">
            <div class="form-item">
              <label>选择 NDBC 文本文件 *</label>
              <input type="file" accept=".txt,.csv" @change="handleFileChange" />
              <small>例如从 NDBC 下载的 realtime2 文本文件（如 32ST0.txt）。</small>
            </div>
            <div class="form-item">
              <label>文件中的变量列名 *</label>
              <input v-model="form.variable" type="text" placeholder="例如: WTMP" />
              <small>与文件首行中的列名一致，例如 WTMP 表示水温。</small>
            </div>
          </div>
          <div class="action-row">
            <button class="btn btn-primary" @click="handleUpload" :disabled="uploading || !form.dataSourceId || !form.file">
              {{ uploading ? '导入中...' : '开始导入' }}
            </button>
          </div>
        </div>

        <!-- 导入结果 -->
        <div v-if="uploadResult" class="card result-card">
          <h3 class="card-title">导入结果</h3>
          <div class="result-content">
            <div class="result-item">
              <span class="result-label">解析总数:</span>
              <span class="result-value">{{ uploadResult.totalCrawled }}</span>
            </div>
            <div class="result-item">
              <span class="result-label">成功保存:</span>
              <span class="result-value success">{{ uploadResult.savedCount }}</span>
            </div>
            <div class="result-item">
              <span class="result-label">重复数据:</span>
              <span class="result-value warning">{{ uploadResult.duplicateCount }}</span>
            </div>
            <div v-if="uploadResult.qualityStats" class="result-item">
              <span class="result-label">质量统计:</span>
              <span class="result-value">
                <span class="quality-good">优 {{ uploadResult.qualityStats.goodCount }}</span>
                <span class="quality-questionable"> / 可疑 {{ uploadResult.qualityStats.questionableCount }}</span>
                <span class="quality-bad"> / 异常 {{ uploadResult.qualityStats.badCount }}</span>
              </span>
            </div>
          </div>
        </div>
      </template>

      <!-- HTTP自动采集模式 -->
      <template v-if="activeMode === 'auto'">
        <!-- Module A：扫描 NDBC 所有站点 -->
        <div class="card">
          <h3 class="card-title">扫描 NDBC 所有站点</h3>
          <div style="display: flex; gap: 12px; margin-bottom: 12px;">
            <button class="btn btn-primary" :disabled="discovering" @click="handleDiscover">
              {{ discovering ? '扫描中...' : '扫描 NDBC realtime2 目录' }}
            </button>
            <button v-if="discoveredStations.length > 0" class="btn btn-default" :disabled="batchCreating" @click="handleBatchCreate">
              {{ batchCreating ? '创建中...' : '批量添加选中站点' }}
            </button>
            <span v-if="discoveredStations.length > 0" class="discover-count">共 {{ discoveredStations.length }} 个站点，{{ discoveredStations.filter(s => s.hasWaveData).length }} 个有波浪数据，已选 {{ selectedDiscover.length }}</span>
          </div>
          <!-- 批量进度面板 -->
          <div v-if="batchProgress.total > 0" class="batch-progress">
            <div class="bp-header">
              <span class="bp-title">批量进度</span>
              <span class="bp-status" :class="{ active: batchCreating, paused: batchPaused }">
                {{ batchCreating ? '处理中' : batchPaused ? '已暂停' : batchProgress.finished ? '已完成' : '等待恢复' }}
              </span>
            </div>
            <div class="bp-bar-wrap">
              <div class="bp-bar" :style="{ width: (batchProgress.done / batchProgress.total * 100) + '%' }"></div>
              <span class="bp-count">{{ batchProgress.done }} / {{ batchProgress.total }}</span>
            </div>
            <div class="bp-stats">
              <span class="bp-stat ok">创建 {{ batchProgress.created }}</span>
              <span class="bp-stat skip">跳过 {{ batchProgress.skipped }}</span>
              <span class="bp-stat chart">图表 {{ batchProgress.charts }}</span>
              <span class="bp-stat scene">场景 {{ batchProgress.scenes }}</span>
              <span v-if="batchProgress.failed.length" class="bp-stat fail">失败 {{ batchProgress.failed.length }}</span>
              <span v-if="batchProgress.current" class="bp-current">当前：{{ batchProgress.current }}</span>
            </div>
            <div class="bp-actions">
              <button v-if="batchCreating" class="btn btn-default" @click="handleBatchPause">暂停</button>
              <button
                v-if="!batchCreating && batchProgress.remaining.length > 0"
                class="btn btn-primary"
                @click="handleBatchResume"
              >
                继续批量 ({{ batchProgress.remaining.length }} 剩余)
              </button>
              <button
                v-if="!batchCreating && (batchProgress.finished || batchProgress.remaining.length === 0)"
                class="btn btn-default"
                @click="handleBatchClear"
              >
                清空记录
              </button>
            </div>
            <details v-if="batchProgress.failed.length" class="bp-failed">
              <summary>失败详情 ({{ batchProgress.failed.length }})</summary>
              <ul>
                <li v-for="f in batchProgress.failed" :key="f.stationId">
                  <strong>{{ f.stationId }}</strong>: {{ f.error }}
                </li>
              </ul>
            </details>
          </div>

          <div v-if="discoveredStations.length > 0" class="discover-table">
            <table>
              <thead>
                <tr>
                  <th style="width:40px"><input type="checkbox" :checked="selectedDiscover.length === discoveredStations.length" @change="toggleSelectAll($event)" /></th>
                  <th>站点ID</th>
                  <th>波浪</th>
                  <th>可用文件</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="s in discoveredStations" :key="s.stationId">
                  <td><input type="checkbox" :value="s.stationId" v-model="selectedDiscover" /></td>
                  <td>{{ s.stationId }}</td>
                  <td>
                    <span v-if="s.hasWaveData" class="wave-yes">有</span>
                    <span v-else class="wave-no">—</span>
                  </td>
                  <td class="suffixes">{{ s.availableSuffixes.join(', ') }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="card">
          <h3 class="card-title">站点列表</h3>
          <div class="station-list">
            <div v-if="ndbcStations.length === 0" class="empty-tip">
              暂无配置NDBC站点的数据源，请先在“数据源管理”中配置站点信息。
            </div>
            <div v-for="station in ndbcStations" :key="station.id" class="station-item">
              <div class="station-info">
                <div class="station-name">{{ station.sourceName }}</div>
                <div class="station-meta">
                  <span class="station-id">{{ station.stationId || '未配置站点ID' }}</span>
                  <span v-if="station.longitude && station.latitude" class="station-coords">
                    坐标 {{ station.longitude?.toFixed(4) }}, {{ station.latitude?.toFixed(4) }}
                  </span>
                  <span v-if="station.fileSuffixes" class="station-suffixes">
                    文件 {{ station.fileSuffixes }}
                  </span>
                </div>
                <div class="station-status">
                  <span :class="['status-badge', station.autoSync === 1 ? 'auto' : 'manual']">
                    {{ station.autoSync === 1 ? '自动同步' : '手动同步' }}
                  </span>
                  <span v-if="station.lastSyncTime" class="last-sync">
                    最后同步: {{ station.lastSyncTime }}
                  </span>
                </div>
              </div>
              <div class="station-actions">
                <button
                  class="btn btn-small btn-primary"
                  @click="handleSyncStation(station)"
                  :disabled="syncingStationId === station.id"
                >
                  {{ syncingStationId === station.id ? '同步中...' : '立即同步' }}
                </button>
                <button
                  class="btn btn-small btn-default"
                  @click="handlePreview(station)"
                  :disabled="previewingStationId === station.id"
                >
                  {{ previewingStationId === station.id ? '加载中...' : '预览数据' }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 同步结果 -->
        <div v-if="syncResult" class="card result-card">
          <h3 class="card-title">同步结果 - {{ syncResult.sourceName }}</h3>
          <div class="result-content">
            <div class="result-item">
              <span class="result-label">解析总数:</span>
              <span class="result-value">{{ syncResult.totalParsed }}</span>
            </div>
            <div class="result-item">
              <span class="result-label">成功保存:</span>
              <span class="result-value success">{{ syncResult.totalSaved }}</span>
            </div>
            <div class="result-item">
              <span class="result-label">重复条数 / 重复率:</span>
              <span class="result-value warning">{{ syncResult.totalDuplicate || 0 }} / {{ syncResult.duplicateRate || 0 }}%</span>
            </div>
            <div class="result-item">
              <span class="result-label">解析/保存吞吐:</span>
              <span class="result-value">
                {{ syncResult.parsePerSecond || 0 }}/s
                <span class="quality-questionable"> | </span>
                <span class="quality-good">{{ syncResult.savePerSecond || 0 }}/s</span>
              </span>
            </div>
            <div class="result-item">
              <span class="result-label">总耗时:</span>
              <span class="result-value">{{ syncResult.costMs || 0 }} ms</span>
            </div>
            <div v-if="syncResult.fileResults" class="file-results">
              <div v-for="file in syncResult.fileResults" :key="file.suffix" class="file-result-item">
                <span class="file-suffix">.{{ file.suffix }}.txt</span>
                <span :class="['file-status', file.status]">
                  {{ file.status === 'success' ? `成功 ${file.saved} 条` : file.status }}
                </span>
                <span class="file-metrics">
                  {{ file.fetchAttempts || 0 }}次 / {{ file.fetchCostMs || 0 }}ms / {{ file.costMs || 0 }}ms
                </span>
                <span v-if="file.errorType" class="file-error">{{ file.errorType }}</span>
                <span v-if="file.qualityStats" class="file-quality">
                  <span class="quality-good">优{{ file.qualityStats.goodCount }}</span>
                  <span class="quality-questionable">可疑{{ file.qualityStats.questionableCount }}</span>
                  <span class="quality-bad">异常{{ file.qualityStats.badCount }}</span>
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 预览数据 -->
        <div v-if="previewData" class="card">
          <h3 class="card-title">远程数据预览 - {{ previewData.stationId }}</h3>
          <div class="preview-files">
            <div v-for="file in previewData.files" :key="file.suffix" class="preview-file">
              <div class="preview-file-header">
                <span class="file-suffix">.{{ file.suffix }}.txt</span>
                <span :class="['file-status', file.available ? 'success' : 'unavailable']">
                  {{ file.available ? `可用 (${file.totalRecords}条)` : '不可用' }}
                </span>
                <span class="preview-fetch-metrics">{{ file.fetchAttempts || 0 }}次 / {{ file.fetchCostMs || 0 }}ms</span>
                <span v-if="file.errorType" class="preview-error">{{ file.errorType }}</span>
              </div>
              <div v-if="file.variables && file.variables.length > 0" class="preview-variables">
                <span>变量: </span>
                <span v-for="v in file.variables" :key="v" class="variable-tag">{{ v }}</span>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import PageHeader from '@/components/PageHeader.vue';
import {
  dataSourceApi,
  observationDataTypeApi,
  noaaDataUploadApi,
  noaaAutoSyncApi,
  type DataSource,
  type ObservationDataType,
  type NoaaUploadResult,
  type NoaaSyncResult,
} from '@/utils/api-admin';
import { message } from '@/utils/message';

const activeMode = ref<'upload' | 'auto'>('auto');
const dataSources = ref<DataSource[]>([]);
const dataTypes = ref<ObservationDataType[]>([]);
const uploading = ref(false);
const uploadResult = ref<NoaaUploadResult | null>(null);

// HTTP自动采集相关
const syncingAll = ref(false);
const syncingStationId = ref<string | number | null>(null);
const previewingStationId = ref<string | number | null>(null);
const syncResult = ref<NoaaSyncResult | null>(null);
const previewData = ref<{
  stationId: string;
  files: Array<{
    suffix: string;
    available: boolean;
    totalRecords?: number;
    variables?: string[];
  }>;
} | null>(null);

// Module A: 扫描发现的 NDBC 站点
interface DiscoveredStation {
  stationId: string;
  availableSuffixes: string[];
  hasWaveData: boolean;
}
const discovering = ref(false);
const batchCreating = ref(false);
const discoveredStations = ref<DiscoveredStation[]>([]);
const selectedDiscover = ref<string[]>([]);

const handleDiscover = async () => {
  discovering.value = true;
  try {
    const list = await dataSourceApi.discoverNdbcStations();
    discoveredStations.value = list || [];
    selectedDiscover.value = (list || []).filter(s => s.hasWaveData).map(s => s.stationId);
    message.success(`发现 ${list?.length || 0} 个站点`);
  } catch (error: any) {
    console.error('扫描NDBC失败:', error);
    message.error(error?.message || '扫描失败');
  } finally {
    discovering.value = false;
  }
};

const toggleSelectAll = (e: Event) => {
  const checked = (e.target as HTMLInputElement).checked;
  selectedDiscover.value = checked ? discoveredStations.value.map(s => s.stationId) : [];
};

/* ============== 批量进度/暂停/续传 (localStorage 持久化) ============== */
const BATCH_KEY = 'noaa_batch_progress_v1';
const batchPaused = ref(false);
const batchProgress = ref({
  total: 0,
  done: 0,
  created: 0,
  skipped: 0,
  charts: 0,
  scenes: 0,
  current: '',
  failed: [] as Array<{ stationId: string; error: string }>,
  remaining: [] as string[],
  finished: false,
});

const saveBatchState = () => {
  try {
    localStorage.setItem(BATCH_KEY, JSON.stringify(batchProgress.value));
  } catch (e) {
    void e;
  }
};
const loadBatchState = () => {
  try {
    const raw = localStorage.getItem(BATCH_KEY);
    if (!raw) return;
    const parsed = JSON.parse(raw);
    if (parsed && Array.isArray(parsed.remaining)) {
      batchProgress.value = { ...batchProgress.value, ...parsed };
    }
  } catch (e) {
    void e;
  }
};
const clearBatchState = () => {
  localStorage.removeItem(BATCH_KEY);
};

/** 单站点处理：抓meta + 创建数据源 + 同步 + 自动生成 */
async function processSingleStation(stationId: string) {
  // 用单元素数组调用批量接口（后端会做去重 + 生成）
  const res = await dataSourceApi.batchCreateNdbcSources([stationId], {
    autoGenerate: true,
    syncData: true,
  });
  return res;
}

async function runBatch() {
  batchCreating.value = true;
  batchPaused.value = false;
  while (batchProgress.value.remaining.length > 0 && !batchPaused.value) {
    const sid = batchProgress.value.remaining[0];
    batchProgress.value.current = sid;
    saveBatchState();
    try {
      const r = await processSingleStation(sid);
      batchProgress.value.created += r.created || 0;
      batchProgress.value.skipped += r.skipped || 0;
      batchProgress.value.charts += r.totalCharts || 0;
      batchProgress.value.scenes += r.totalScenes || 0;
    } catch (e: any) {
      batchProgress.value.failed.push({ stationId: sid, error: e?.message || String(e) });
    }
    batchProgress.value.remaining.shift();
    batchProgress.value.done += 1;
    saveBatchState();
    // 让出主线程，避免UI卡顿
    await new Promise((r) => setTimeout(r, 60));
  }
  batchCreating.value = false;
  batchProgress.value.current = '';

  if (batchProgress.value.remaining.length === 0) {
    batchProgress.value.finished = true;
    saveBatchState();
    message.success(
      `批量完成！创建 ${batchProgress.value.created} 个 / 跳过 ${batchProgress.value.skipped} 个，` +
        `生成 ${batchProgress.value.charts} 图表 + ${batchProgress.value.scenes} 场景` +
        (batchProgress.value.failed.length ? `，失败 ${batchProgress.value.failed.length} 个` : ''),
    );
    await loadDataSources();
  } else {
    message.info('已暂停。下次点击 "继续批量" 恢复。');
  }
}

const handleBatchCreate = async () => {
  if (selectedDiscover.value.length === 0) {
    message.warning('请先选择站点');
    return;
  }
  if (
    !confirm(
      `将批量添加 ${selectedDiscover.value.length} 个 NDBC 站点。\n\n` +
        `每个站点会:\n` +
        `1. 抓取经纬度/水深/名称\n` +
        `2. 同步一次远程数据\n` +
        `3. 自动生成图表 + 3D 海洋场景\n\n` +
        `处理过程中可点击 "暂停" 中断，下次进入此页面会自动恢复进度。\n\n继续？`,
    )
  )
    return;

  batchProgress.value = {
    total: selectedDiscover.value.length,
    done: 0,
    created: 0,
    skipped: 0,
    charts: 0,
    scenes: 0,
    current: '',
    failed: [],
    remaining: [...selectedDiscover.value],
    finished: false,
  };
  saveBatchState();
  selectedDiscover.value = [];
  await runBatch();
  return; /* 兼容下面原 try 结构 */
};

const handleBatchPause = () => {
  batchPaused.value = true;
};

const handleBatchResume = async () => {
  if (batchProgress.value.remaining.length === 0) return;
  await runBatch();
};

const handleBatchClear = () => {
  if (!confirm('清空批量进度记录？已创建的数据源会保留。')) return;
  clearBatchState();
  batchProgress.value = {
    total: 0, done: 0, created: 0, skipped: 0, charts: 0, scenes: 0,
    current: '', failed: [], remaining: [], finished: false,
  };
};

// 兼容旧 try / catch 块（其它入口未删，下方保留以防被引用）
const _legacyBatchCreate = async () => {
  try {
    const res = await dataSourceApi.batchCreateNdbcSources(selectedDiscover.value, {
      autoGenerate: true,
      syncData: true,
    });
    message.success(
      `创建 ${res.created} 个 (重复跳过 ${res.skipped})，` +
        `自动生成 ${res.totalCharts ?? 0} 个图表 + ${res.totalScenes ?? 0} 个 3D 场景`,
    );
    await loadDataSources();
    selectedDiscover.value = [];
  } catch (error: any) {
    console.error('批量创建失败:', error);
    message.error(error?.message || '批量创建失败');
  } finally {
    batchCreating.value = false;
  }
};

// NDBC站点列表（筛选出配置了stationId的数据源）
const ndbcStations = computed(() => {
  return dataSources.value.filter(
    (ds) => ds.sourceType === 'NOAA' && ds.status === 1 && ds.stationId
  );
});

const form = ref({
  dataSourceId: '' as number | string,
  dataTypeCode: '' as string,
  variable: 'WTMP',
  file: null as File | null,
});

const loadDataSources = async () => {
  try {
    const list = await dataSourceApi.list();
    // 只显示NOAA类型的数据源
    dataSources.value = list.filter((ds) => ds.sourceType === 'NOAA' && ds.status === 1);
  } catch (error: any) {
    console.error('加载数据源失败:', error);
    message.error(error?.message || '加载数据源失败，请稍后重试');
  }
};

const loadDataTypes = async () => {
  try {
    const list = await observationDataTypeApi.list();
    dataTypes.value = list;
  } catch (error: any) {
    console.error('加载观测数据类型失败:', error);
    message.error(error?.message || '加载观测数据类型失败，请稍后重试');
  }
};

const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  const files = target.files;
  if (files && files.length > 0) {
    form.value.file = files[0];
  } else {
    form.value.file = null;
  }
};

const handleUpload = async () => {
  if (!form.value.dataSourceId) {
    message.warning('请选择数据源');
    return;
  }
  if (!form.value.dataTypeCode) {
    message.warning('请选择观测数据类型');
    return;
  }
  if (!form.value.variable) {
    message.warning('请填写文件中的变量列名');
    return;
  }
  if (!form.value.file) {
    message.warning('请先选择要上传的文件');
    return;
  }

  uploading.value = true;
  uploadResult.value = null;

  try {
    const result = await noaaDataUploadApi.uploadNdbcText({
      dataSourceId: form.value.dataSourceId,
      variable: form.value.variable,
      dataTypeCode: form.value.dataTypeCode,
      file: form.value.file,
    });

    uploadResult.value = result;
    message.success(
      `导入完成！解析 ${result.totalCrawled} 条，成功保存 ${result.savedCount} 条，重复 ${result.duplicateCount} 条`
    );
  } catch (error: any) {
    console.error('文件导入失败:', error);
    message.error(error?.message || '文件导入失败，请稍后重试');
  } finally {
    uploading.value = false;
  }
};

const handleReset = () => {
  form.value.dataSourceId = '';
  form.value.dataTypeCode = '';
  form.value.variable = 'WTMP';
  form.value.file = null;
  uploadResult.value = null;
};

// HTTP自动采集相关方法
const handleSyncAll = async () => {
  syncingAll.value = true;
  syncResult.value = null;

  try {
    const result = await noaaAutoSyncApi.syncAll();
    message.success(
      `同步完成！站点: ${result.totalStations}，成功: ${result.successCount}，失败: ${result.failCount}`
    );
    // 刷新数据源列表以获取最新同步时间
    loadDataSources();
  } catch (error: any) {
    console.error('同步失败:', error);
    message.error(error?.message || '同步失败，请稍后重试');
  } finally {
    syncingAll.value = false;
  }
};

const handleSyncStation = async (station: DataSource) => {
  syncingStationId.value = station.id!;
  syncResult.value = null;

  try {
    const result = await noaaAutoSyncApi.syncStation(station.id!);
    syncResult.value = result;
    message.success(
      `同步完成！解析 ${result.totalParsed} 条，保存 ${result.totalSaved} 条`
    );
    // 刷新数据源列表
    loadDataSources();
  } catch (error: any) {
    console.error('同步失败:', error);
    message.error(error?.message || '同步失败，请稍后重试');
  } finally {
    syncingStationId.value = null;
  }
};

const handlePreview = async (station: DataSource) => {
  previewingStationId.value = station.id!;
  previewData.value = null;

  try {
    const result = await noaaAutoSyncApi.previewRemoteData(station.id!);
    previewData.value = result;
  } catch (error: any) {
    console.error('预览失败:', error);
    message.error(error?.message || '预览失败，请稍后重试');
  } finally {
    previewingStationId.value = null;
  }
};

onMounted(() => {
  loadDataSources();
  loadDataTypes();
  loadBatchState();
});
</script>

<style scoped lang="less">
.noaa-data-sync-page {
  width: 100%;
  animation: fadeIn 0.5s ease-out;
}

.content-wrapper {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.card {
  background: linear-gradient(135deg, rgba(30, 58, 95, 0.6), rgba(22, 33, 62, 0.6));
  backdrop-filter: blur(20px);
  border: 1px solid rgba(74, 144, 226, 0.3);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3), inset 0 1px 0 rgba(74, 144, 226, 0.1);
  animation: slideUp 0.5s ease-out both;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #e0f2ff;
  margin: 0 0 20px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(74, 144, 226, 0.3);
}

.crawl-modes {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.mode-btn {
  flex: 1;
  min-width: 150px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 16px 24px;
  background: rgba(74, 144, 226, 0.1);
  border: 2px solid rgba(74, 144, 226, 0.3);
  border-radius: 12px;
  color: rgba(224, 242, 255, 0.8);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    background: rgba(74, 144, 226, 0.2);
    border-color: rgba(74, 144, 226, 0.5);
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(74, 144, 226, 0.25);
  }

  &.active {
    background: linear-gradient(135deg, rgba(74, 144, 226, 0.3), rgba(0, 212, 255, 0.2));
    border-color: rgba(74, 144, 226, 0.6);
    color: #fff;
    box-shadow: 0 4px 16px rgba(74, 144, 226, 0.35);
  }
}

.mode-icon {
  font-size: 24px;
}

.mode-label {
  font-size: 14px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.form-item {
  label {
    display: block;
    margin-bottom: 8px;
    font-size: 14px;
    font-weight: 500;
    color: rgba(255, 255, 255, 0.9);
    letter-spacing: 0.2px;
  }

  input,
  select {
    width: 100%;
    padding: 12px 16px;
    background: rgba(15, 20, 45, 0.6);
    border: 1px solid rgba(74, 144, 226, 0.3);
    border-radius: 8px;
    color: #e0f2ff;
    font-size: 14px;
    transition: all 0.3s;

    &:focus {
      outline: none;
      border-color: rgba(74, 144, 226, 0.6);
      box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.15);
    }

    &::placeholder {
      color: rgba(224, 242, 255, 0.4);
    }
  }

  select {
    cursor: pointer;
    appearance: none;
    -webkit-appearance: none;
    -moz-appearance: none;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23fff' d='M6 9L1 4h10z'/%3E%3C/svg%3E");
    background-repeat: no-repeat;
    background-position: right 12px center;
    padding-right: 36px;
    color-scheme: dark;

    option {
      background: rgba(15, 20, 45, 0.98) !important;
      color: #fff !important;
    }
  }

  small {
    display: block;
    margin-top: 6px;
    font-size: 12px;
    color: rgba(224, 242, 255, 0.6);
  }
}

.action-buttons {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;

  &.btn-primary {
    background: linear-gradient(135deg, #4a90e2, #00d4ff);
    color: #fff;
    box-shadow: 0 4px 12px rgba(74, 144, 226, 0.3);

    &:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 6px 16px rgba(74, 144, 226, 0.4);
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }

  &.btn-default {
    background: rgba(74, 144, 226, 0.2);
    color: #e0f2ff;
    border: 1px solid rgba(74, 144, 226, 0.3);

    &:hover {
      background: rgba(74, 144, 226, 0.3);
      border-color: rgba(74, 144, 226, 0.5);
    }
  }

  &.btn-large {
    padding: 14px 32px;
    font-size: 16px;
  }
}

.result-card {
  animation: slideUp 0.5s ease-out 0.2s both;
}

.result-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: rgba(15, 20, 45, 0.4);
  border-radius: 8px;
  border: 1px solid rgba(74, 144, 226, 0.2);
}

.result-label {
  font-size: 14px;
  color: rgba(224, 242, 255, 0.8);
}

.result-value {
  font-size: 18px;
  font-weight: 600;
  color: #4a90e2;

  &.success {
    color: #20b2aa;
  }

  &.warning {
    color: #ffa500;
  }

  .quality-good {
    color: #20b2aa;
  }

  .quality-questionable {
    color: #ffa500;
  }

  .quality-bad {
    color: #ff6b6b;
  }
}

.mode-tabs {
  display: flex;
  gap: 12px;
}

.mode-btn {
  flex: 1;
  padding: 14px 20px;
  background: rgba(74, 144, 226, 0.1);
  border: 2px solid rgba(74, 144, 226, 0.3);
  border-radius: 10px;
  color: rgba(224, 242, 255, 0.8);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    background: rgba(74, 144, 226, 0.2);
    border-color: rgba(74, 144, 226, 0.5);
  }

  &.active {
    background: linear-gradient(135deg, rgba(74, 144, 226, 0.3), rgba(0, 212, 255, 0.2));
    border-color: rgba(74, 144, 226, 0.6);
    color: #fff;
    box-shadow: 0 4px 12px rgba(74, 144, 226, 0.25);
  }
}

.action-row {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.station-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.discover-count {
  margin-left: auto;
  align-self: center;
  color: rgba(224, 242, 255, 0.7);
  font-size: 13px;
}

.discover-table {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid rgba(74, 144, 226, 0.2);
  border-radius: 8px;

  table {
    width: 100%;
    border-collapse: collapse;
    font-size: 13px;
    color: #e0f2ff;
  }

  th, td {
    padding: 8px 12px;
    text-align: left;
    border-bottom: 1px solid rgba(74, 144, 226, 0.12);
  }

  th {
    background: rgba(30, 58, 95, 0.6);
    position: sticky;
    top: 0;
  }

  .wave-yes { color: #4ade80; }
  .wave-no  { color: rgba(224, 242, 255, 0.4); }
  .suffixes { font-family: monospace; color: rgba(224, 242, 255, 0.7); font-size: 12px; }
}

.empty-tip {
  padding: 40px 20px;
  text-align: center;
  color: rgba(224, 242, 255, 0.6);
  font-size: 14px;
}

.station-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: rgba(15, 20, 45, 0.4);
  border: 1px solid rgba(74, 144, 226, 0.2);
  border-radius: 10px;
  transition: all 0.3s;

  &:hover {
    background: rgba(15, 20, 45, 0.6);
    border-color: rgba(74, 144, 226, 0.4);
  }
}

.station-info {
  flex: 1;
}

.station-name {
  font-size: 15px;
  font-weight: 600;
  color: #e0f2ff;
  margin-bottom: 6px;
}

.station-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 13px;
  color: rgba(224, 242, 255, 0.7);
  margin-bottom: 6px;

  .station-id {
    color: #4a90e2;
    font-weight: 500;
  }

  .station-coords,
  .station-suffixes {
    color: rgba(224, 242, 255, 0.6);
  }
}

.station-status {
  display: flex;
  gap: 12px;
  align-items: center;
  font-size: 12px;
}

.status-badge {
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 11px;

  &.auto {
    background: rgba(32, 178, 170, 0.2);
    color: #20b2aa;
  }

  &.manual {
    background: rgba(255, 165, 0, 0.2);
    color: #ffa500;
  }
}

.last-sync {
  color: rgba(224, 242, 255, 0.5);
}

.station-actions {
  display: flex;
  gap: 8px;
}

.btn-small {
  padding: 8px 14px;
  font-size: 13px;
}

.file-results {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: rgba(15, 20, 45, 0.3);
  border-radius: 6px;

  .file-suffix {
    font-family: monospace;
    color: #4a90e2;
  }

  .file-status {
    font-size: 13px;

    &.success {
      color: #20b2aa;
    }
  }

  .file-quality {
    margin-left: auto;
    font-size: 12px;

    .quality-good {
      color: #20b2aa;
      margin-right: 8px;
    }

    .quality-questionable {
      color: #ffa500;
      margin-right: 8px;
    }

    .quality-bad {
      color: #ff6b6b;
    }
  }
  .file-metrics {
    font-size: 12px;
    color: rgba(224, 242, 255, 0.6);
  }
  .file-error {
    font-size: 12px;
    color: #ff9f9f;
  }
}

.preview-files {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.preview-file {
  padding: 14px;
  background: rgba(15, 20, 45, 0.4);
  border-radius: 8px;
}

.preview-file-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;

  .file-suffix {
    font-family: monospace;
    color: #4a90e2;
    font-weight: 500;
  }

  .file-status {
    font-size: 13px;

    &.success {
      color: #20b2aa;
    }

    &.unavailable {
      color: rgba(224, 242, 255, 0.5);
    }
  }
  .preview-fetch-metrics {
    margin-left: auto;
    font-size: 12px;
    color: rgba(224, 242, 255, 0.6);
  }
  .preview-error {
    font-size: 12px;
    color: #ff9f9f;
  }
}

.preview-variables {
  font-size: 12px;
  color: rgba(224, 242, 255, 0.7);
}

.variable-tag {
  display: inline-block;
  padding: 2px 6px;
  margin: 2px 4px;
  background: rgba(74, 144, 226, 0.2);
  border-radius: 4px;
  font-family: monospace;
  font-size: 11px;
}

// 批量进度面板
.batch-progress {
  margin: 14px 0;
  padding: 14px 16px;
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.06), rgba(74, 144, 226, 0.06));
  border: 1px solid rgba(74, 144, 226, 0.3);
  border-radius: 10px;
}
.bp-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.bp-title { font-weight: 600; color: #4a90e2; font-size: 15px; }
.bp-status {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 10px;
  background: rgba(125, 125, 125, 0.2);
  color: #888;
}
.bp-status.active { background: rgba(0, 212, 255, 0.2); color: #00d4ff; animation: bpPulse 1.5s ease-in-out infinite; }
.bp-status.paused { background: rgba(250, 173, 20, 0.2); color: #faad14; }
@keyframes bpPulse { 0%,100% { opacity: 1; } 50% { opacity: 0.5; } }
.bp-bar-wrap {
  position: relative;
  height: 22px;
  background: rgba(74, 144, 226, 0.1);
  border-radius: 11px;
  overflow: hidden;
  margin-bottom: 10px;
}
.bp-bar {
  height: 100%;
  background: linear-gradient(90deg, #4a90e2, #00d4ff);
  transition: width 0.3s;
  box-shadow: 0 0 8px rgba(0, 212, 255, 0.5);
}
.bp-count {
  position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 600; color: #fff;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.5);
}
.bp-stats {
  display: flex; flex-wrap: wrap; gap: 10px;
  font-size: 12px; margin-bottom: 10px;
}
.bp-stat {
  padding: 3px 9px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255,255,255,0.8);
}
.bp-stat.ok    { background: rgba(82, 196, 26, 0.18); color: #95de64; }
.bp-stat.skip  { background: rgba(125, 125, 125, 0.18); color: #999; }
.bp-stat.chart { background: rgba(24, 144, 255, 0.18); color: #69c0ff; }
.bp-stat.scene { background: rgba(0, 212, 255, 0.18); color: #00d4ff; }
.bp-stat.fail  { background: rgba(255, 77, 79, 0.18); color: #ff7875; }
.bp-current    { color: #faad14; font-style: italic; }
.bp-actions { display: flex; gap: 8px; }
.bp-failed {
  margin-top: 10px;
  font-size: 12px;
  color: rgba(255, 120, 117, 0.85);
  summary { cursor: pointer; padding: 4px 0; }
  ul { padding-left: 22px; margin: 6px 0 0; }
  li { margin: 2px 0; }
}

/* 浅色主色覆盖 */
.card {
  background: #ffffff;
  border: 1px solid #dbe8f4;
  box-shadow: 0 12px 26px -20px rgba(15, 23, 42, 0.35);
}

.card-title {
  color: #0f172a;
  border-bottom-color: #dbe8f4;
}

.mode-btn {
  background: #f8fbff;
  border-color: #dbe8f4;
  color: #475569;

  &:hover {
    background: #f0f9ff;
    border-color: #7dd3fc;
    box-shadow: none;
  }

  &.active {
    background: linear-gradient(135deg, rgba(2, 132, 199, 0.18), rgba(6, 182, 212, 0.14));
    border-color: rgba(2, 132, 199, 0.35);
    color: #0f172a;
    box-shadow: 0 8px 16px -12px rgba(2, 132, 199, 0.5);
  }
}

.form-item {
  label {
    color: #334155;
  }

  input,
  select {
    background: #ffffff;
    border: 1px solid #dbe8f4;
    color: #334155;

    &:focus {
      border-color: rgba(2, 132, 199, 0.55);
      box-shadow: 0 0 0 4px rgba(2, 132, 199, 0.12);
    }

    &::placeholder {
      color: #94a3b8;
    }
  }

  select {
    color-scheme: light;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2364758b' d='M6 9L1 4h10z'/%3E%3C/svg%3E");

    option {
      background: #ffffff !important;
      color: #0f172a !important;
    }
  }

  small {
    color: #64748b;
  }
}

.result-item,
.station-item,
.preview-file,
.file-result-item {
  background: #f8fbff;
  border-color: #dbe8f4;
}

.station-item:hover {
  background: #f0f9ff;
  border-color: #bae6fd;
}

.station-name {
  color: #0f172a;
}

.station-meta,
.last-sync,
.empty-tip,
.discover-count {
  color: #64748b;
}

.station-meta .station-id,
.file-result-item .file-suffix,
.preview-file-header .file-suffix,
.result-value,
.bp-title {
  color: #0284c7;
}

.discover-table {
  border-color: #dbe8f4;

  table {
    color: #334155;
  }

  th, td {
    border-bottom-color: #e2e8f0;
  }

  th {
    background: #f4f9ff;
  }

  .wave-no {
    color: #94a3b8;
  }

  .suffixes {
    color: #64748b;
  }
}

.batch-progress {
  background: linear-gradient(135deg, rgba(2, 132, 199, 0.08), rgba(6, 182, 212, 0.07));
  border-color: rgba(2, 132, 199, 0.24);
}

.bp-bar-wrap {
  background: rgba(2, 132, 199, 0.12);
}

.bp-bar {
  background: linear-gradient(90deg, #0284c7, #06b6d4);
  box-shadow: 0 0 8px rgba(2, 132, 199, 0.35);
}

.bp-stat {
  background: #f1f5f9;
  color: #475569;
}

/* NOAA 同步页面：进一步提高文字对比度 */
.noaa-data-sync-page {
  color: #334155;
}

.noaa-data-sync-page .mode-btn,
.noaa-data-sync-page .btn.btn-default,
.noaa-data-sync-page .result-label,
.noaa-data-sync-page .station-meta,
.noaa-data-sync-page .last-sync,
.noaa-data-sync-page .discover-count,
.noaa-data-sync-page .empty-tip,
.noaa-data-sync-page .file-metrics,
.noaa-data-sync-page .preview-fetch-metrics,
.noaa-data-sync-page .preview-variables,
.noaa-data-sync-page .bp-current,
.noaa-data-sync-page .bp-failed {
  color: #334155;
}

.noaa-data-sync-page .bp-status {
  color: #334155;
  background: #e2e8f0;
}

.noaa-data-sync-page .bp-status.active {
  color: #0369a1;
  background: #e0f2fe;
}

.noaa-data-sync-page .bp-status.paused {
  color: #b45309;
  background: #fef3c7;
}

.noaa-data-sync-page .station-meta .station-id,
.noaa-data-sync-page .preview-file-header .file-suffix,
.noaa-data-sync-page .file-result-item .file-suffix,
.noaa-data-sync-page .quality-good {
  color: #0369a1;
}

.noaa-data-sync-page .card-title,
.noaa-data-sync-page .station-name,
.noaa-data-sync-page .preview-file-header .file-status,
.noaa-data-sync-page .discover-table th,
.noaa-data-sync-page .discover-table td,
.noaa-data-sync-page .file-status,
.noaa-data-sync-page .result-value {
  color: #0f172a;
}

.noaa-data-sync-page .discover-table table,
.noaa-data-sync-page .station-meta .station-coords,
.noaa-data-sync-page .station-meta .station-suffixes,
.noaa-data-sync-page .preview-variables,
.noaa-data-sync-page .preview-fetch-metrics,
.noaa-data-sync-page .file-metrics,
.noaa-data-sync-page .empty-tip,
.noaa-data-sync-page .discover-count,
.noaa-data-sync-page .last-sync,
.noaa-data-sync-page .bp-stat {
  color: #475569;
}

.noaa-data-sync-page .quality-questionable {
  color: #b45309;
}

.noaa-data-sync-page .quality-bad,
.noaa-data-sync-page .preview-error,
.noaa-data-sync-page .file-error {
  color: #dc2626;
}

.noaa-data-sync-page .discover-table .wave-no,
.noaa-data-sync-page .preview-file-header .file-status.unavailable,
.noaa-data-sync-page .bp-stat.skip {
  color: #64748b;
}

.noaa-data-sync-page .discover-table .wave-yes {
  color: #166534;
  font-weight: 700;
}

/* 修复部分浏览器/样式叠加导致的下拉框重复箭头 */
.noaa-data-sync-page select {
  appearance: none !important;
  -webkit-appearance: none !important;
  -moz-appearance: none !important;
  background-color: #ffffff !important;
  background-image: linear-gradient(45deg, transparent 50%, #475569 50%),
    linear-gradient(135deg, #475569 50%, transparent 50%) !important;
  background-position: calc(100% - 17px) 50%, calc(100% - 11px) 50% !important;
  background-size: 6px 6px, 6px 6px !important;
  background-repeat: no-repeat !important;
  padding-right: 34px !important;
  color: #0f172a !important;
  border-color: #dbe8f4 !important;
}

/* 提升浅色主题下 NOAA 页面文字可读性 */
.noaa-data-sync-page .card-title,
.noaa-data-sync-page .station-name,
.noaa-data-sync-page .bp-title,
.noaa-data-sync-page .preview-file-name {
  color: #0f172a !important;
}

.noaa-data-sync-page .station-meta,
.noaa-data-sync-page .last-sync,
.noaa-data-sync-page .empty-tip,
.noaa-data-sync-page .discover-count,
.noaa-data-sync-page .bp-count,
.noaa-data-sync-page .bp-current,
.noaa-data-sync-page .preview-file-time,
.noaa-data-sync-page .preview-file-size,
.noaa-data-sync-page .result-label {
  color: #334155 !important;
}

// 响应式设计
@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .crawl-modes {
    flex-direction: column;
  }

  .mode-btn {
    min-width: auto;
  }

  .action-buttons {
    flex-direction: column;
  }
}
</style>

