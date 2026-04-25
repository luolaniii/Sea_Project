<template>
  <div class="time-replay-bar">
    <span class="replay-title">历史回放</span>
    <span class="time-display">{{ formattedTime }}</span>
    <input
      type="range"
      class="time-slider"
      :min="minMs"
      :max="maxMs"
      :step="step"
      :value="modelValue"
      @input="onSliderInput"
    />
    <div class="replay-actions">
      <button type="button" class="btn-icon" :title="playing ? '暂停' : '播放'" @click="togglePlay">
        {{ playing ? '⏸' : '▶' }}
      </button>
      <button type="button" class="btn-text" @click="goLatest">最新</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue';

const props = defineProps<{
  minMs: number;
  maxMs: number;
  modelValue: number;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: number];
}>();

const playing = ref(false);
let playTimer: ReturnType<typeof setInterval> | null = null;

const step = computed(() => {
  const span = props.maxMs - props.minMs;
  if (span <= 0) return 1;
  return Math.max(1, Math.floor(span / 500));
});

const formattedTime = computed(() => formatMs(props.modelValue));

function formatMs(ms: number): string {
  const d = new Date(ms);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

function onSliderInput(e: Event) {
  const v = Number((e.target as HTMLInputElement).value);
  emit('update:modelValue', v);
}

function goLatest() {
  stopPlay();
  emit('update:modelValue', props.maxMs);
}

function stopPlay() {
  playing.value = false;
  if (playTimer != null) {
    clearInterval(playTimer);
    playTimer = null;
  }
}

function togglePlay() {
  if (playing.value) {
    stopPlay();
    return;
  }
  if (props.maxMs <= props.minMs) return;
  playing.value = true;
  playTimer = setInterval(() => {
    let next = props.modelValue + step.value;
    if (next >= props.maxMs) {
      next = props.maxMs;
      emit('update:modelValue', next);
      stopPlay();
      return;
    }
    emit('update:modelValue', next);
  }, 120);
}

watch(
  () => [props.minMs, props.maxMs] as const,
  () => {
    stopPlay();
  }
);

onUnmounted(() => stopPlay());
</script>

<style scoped lang="less">
.time-replay-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  padding: 12px 16px;
  background: rgba(15, 20, 45, 0.92);
  backdrop-filter: blur(12px);
}

.replay-title {
  font-size: 13px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
  white-space: nowrap;
}

.time-display {
  font-size: 13px;
  font-variant-numeric: tabular-nums;
  color: #a8b4ff;
  min-width: 155px;
}

.time-slider {
  flex: 1;
  min-width: 120px;
  height: 6px;
  accent-color: #667eea;
  cursor: pointer;
}

.replay-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-icon {
  width: 36px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid rgba(102, 126, 234, 0.45);
  background: rgba(102, 126, 234, 0.2);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  line-height: 1;
}

.btn-icon:hover {
  background: rgba(102, 126, 234, 0.35);
}

.btn-text {
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: transparent;
  color: rgba(255, 255, 255, 0.85);
  font-size: 12px;
  cursor: pointer;
}

.btn-text:hover {
  border-color: rgba(102, 126, 234, 0.5);
  color: #fff;
}
</style>
