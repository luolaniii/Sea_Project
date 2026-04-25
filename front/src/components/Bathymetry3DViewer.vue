<template>
  <div ref="containerRef" class="bathymetry-3d-viewer"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue';
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls';

interface Props {
  data?: any[];
  region?: {
    minLon: number;
    maxLon: number;
    minLat: number;
    maxLat: number;
  };
  config?: any;
}

const props = withDefaults(defineProps<Props>(), {
  data: () => [],
  region: () => ({
    minLon: 100,
    maxLon: 130,
    minLat: 0,
    maxLat: 40,
  }),
});

const containerRef = ref<HTMLDivElement>();
let scene: THREE.Scene | null = null;
let camera: THREE.PerspectiveCamera | null = null;
let renderer: THREE.WebGLRenderer | null = null;
let controls: OrbitControls | null = null;
let animationId: number | null = null;
let terrainMesh: THREE.Mesh | null = null;

const initScene = () => {
  if (!containerRef.value) {
    console.warn('容器引用为空，无法初始化场景');
    return;
  }

  const width = containerRef.value.clientWidth;
  const height = containerRef.value.clientHeight;

  if (width === 0 || height === 0) {
    console.warn('容器尺寸为0，等待下一帧重试');
    setTimeout(initScene, 100);
    return;
  }

  // 创建场景
  scene = new THREE.Scene();
  scene.background = new THREE.Color(0x0a0e27);
  scene.fog = new THREE.Fog(0x0a0e27, 10, 200);

  // 创建相机
  camera = new THREE.PerspectiveCamera(75, width / height, 0.1, 1000);
  camera.position.set(0, 50, 100);
  camera.lookAt(0, 0, 0);

  // 创建渲染器
  renderer = new THREE.WebGLRenderer({ antialias: true });
  renderer.setSize(width, height);
  renderer.shadowMap.enabled = true;
  renderer.shadowMap.type = THREE.PCFSoftShadowMap;
  containerRef.value.appendChild(renderer.domElement);

  // 添加轨道控制器
  controls = new OrbitControls(camera, renderer.domElement);
  controls.enableDamping = true;
  controls.dampingFactor = 0.05;
  controls.minDistance = 20;
  controls.maxDistance = 500;

  // 添加光源
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.4);
  scene.add(ambientLight);

  const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8);
  directionalLight.position.set(50, 100, 50);
  directionalLight.castShadow = true;
  directionalLight.shadow.camera.left = -100;
  directionalLight.shadow.camera.right = 100;
  directionalLight.shadow.camera.top = 100;
  directionalLight.shadow.camera.bottom = -100;
  scene.add(directionalLight);

  // 添加辅助网格
  const gridHelper = new THREE.GridHelper(200, 20, 0x444444, 0x222222);
  scene.add(gridHelper);

  // 添加坐标轴辅助
  const axesHelper = new THREE.AxesHelper(50);
  scene.add(axesHelper);

  // 如果有数据，创建地形
  if (props.data && props.data.length > 0) {
    createTerrain(props.data);
  }

  // 动画循环
  const animate = () => {
    if (!scene || !camera || !renderer || !controls) return;

    controls.update();
    renderer.render(scene, camera);
    animationId = requestAnimationFrame(animate);
  };

  animate();

  // 窗口大小调整
  window.addEventListener('resize', handleResize);
};

const createTerrain = (data: any[]) => {
  if (!scene) return;

  // 移除旧的地形
  if (terrainMesh) {
    scene.remove(terrainMesh);
    terrainMesh.geometry.dispose();
    if (terrainMesh.material instanceof THREE.Material) {
      terrainMesh.material.dispose();
    }
  }

  // 计算数据范围
  const longitudes = data.map((item) => item.longitude).filter((v) => v != null);
  const latitudes = data.map((item) => item.latitude).filter((v) => v != null);
  const depths = data.map((item) => item.dataValue || item.depth || 0);

  if (longitudes.length === 0 || latitudes.length === 0) {
    console.warn('缺少有效的经纬度数据');
    return;
  }

  const minLon = Math.min(...longitudes);
  const maxLon = Math.max(...longitudes);
  const minLat = Math.min(...latitudes);
  const maxLat = Math.max(...latitudes);
  const minDepth = Math.min(...depths);
  const maxDepth = Math.max(...depths);

  // 创建网格尺寸
  const gridSize = 50;
  const lonRange = maxLon - minLon || 0.01;
  const latRange = maxLat - minLat || 0.01;
  const depthRange = maxDepth - minDepth || 1;

  // 创建高度图数据
  const heights: number[][] = [];
  for (let i = 0; i <= gridSize; i++) {
    heights[i] = [];
    for (let j = 0; j <= gridSize; j++) {
      const lon = minLon + (lonRange * j) / gridSize;
      const lat = minLat + (latRange * i) / gridSize;

      // 找到最近的数据点
      let minDist = Infinity;
      let nearestDepth = 0;

      data.forEach((item) => {
        if (item.longitude != null && item.latitude != null) {
          const dist = Math.sqrt(
            Math.pow(item.longitude - lon, 2) + Math.pow(item.latitude - lat, 2)
          );
          if (dist < minDist) {
            minDist = dist;
            nearestDepth = item.dataValue || item.depth || 0;
          }
        }
      });

      // 归一化深度值（负值表示海底深度）
      const normalizedDepth = -((nearestDepth - minDepth) / depthRange) * 20;
      heights[i][j] = normalizedDepth;
    }
  }

  // 创建几何体
  const geometry = new THREE.PlaneGeometry(100, 100, gridSize, gridSize);

  // 设置顶点高度
  const positions = geometry.attributes.position;
  for (let i = 0; i <= gridSize; i++) {
    for (let j = 0; j <= gridSize; j++) {
      const index = i * (gridSize + 1) + j;
      const height = heights[i][j];
      positions.setZ(index, height);
    }
  }

  positions.needsUpdate = true;
  geometry.computeVertexNormals();

  // 创建材质（根据深度着色）
  const material = new THREE.MeshPhongMaterial({
    color: 0x006994,
    flatShading: false,
    vertexColors: false,
  });

  // 添加颜色渐变（深蓝到浅蓝）
  const colors: number[] = [];
  for (let i = 0; i <= gridSize; i++) {
    for (let j = 0; j <= gridSize; j++) {
      const height = heights[i][j];
      const normalizedHeight = (height + 20) / 20; // 归一化到0-1
      const color = new THREE.Color();
      color.setHSL(0.6 - normalizedHeight * 0.2, 0.8, 0.3 + normalizedHeight * 0.3);
      colors.push(color.r, color.g, color.b);
    }
  }

  geometry.setAttribute('color', new THREE.Float32BufferAttribute(colors, 3));
  material.vertexColors = true;

  // 创建网格
  terrainMesh = new THREE.Mesh(geometry, material);
  terrainMesh.rotation.x = -Math.PI / 2;
  terrainMesh.receiveShadow = true;
  terrainMesh.castShadow = true;
  scene.add(terrainMesh);

  // 调整相机位置
  if (camera) {
    camera.position.set(0, 60, 120);
    camera.lookAt(0, -10, 0);
  }
};

const handleResize = () => {
  if (!containerRef.value || !camera || !renderer) return;

  const width = containerRef.value.clientWidth;
  const height = containerRef.value.clientHeight;

  camera.aspect = width / height;
  camera.updateProjectionMatrix();
  renderer.setSize(width, height);
};

const cleanup = () => {
  if (animationId !== null) {
    cancelAnimationFrame(animationId);
    animationId = null;
  }

  window.removeEventListener('resize', handleResize);

  if (controls) {
    controls.dispose();
    controls = null;
  }

  if (renderer && containerRef.value) {
    containerRef.value.removeChild(renderer.domElement);
    renderer.dispose();
  }

  if (scene) {
    scene.traverse((object) => {
      if (object instanceof THREE.Mesh) {
        object.geometry.dispose();
        if (Array.isArray(object.material)) {
          object.material.forEach((m) => m.dispose());
        } else {
          object.material.dispose();
        }
      }
    });
  }

  scene = null;
  camera = null;
  renderer = null;
  terrainMesh = null;
};

watch(
  () => props.data,
  (newData) => {
    if (newData && newData.length > 0 && scene) {
      createTerrain(newData);
    }
  },
  { deep: true }
);

onMounted(async () => {
  await nextTick();
  setTimeout(() => {
    initScene();
  }, 100);
});

onUnmounted(() => {
  cleanup();
});
</script>

<style scoped lang="less">
.bathymetry-3d-viewer {
  width: 100%;
  height: 100%;
  min-height: 600px;
  position: relative;
  overflow: hidden;
  background: #0a0e27;
}
</style>

