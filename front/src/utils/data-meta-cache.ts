import {
  dataSourceApi,
  observationDataTypeApi,
  type DataSource,
  type ObservationDataType,
} from './api-admin';

// 简单的前端内存缓存：整个前端会话内复用，避免重复请求
let dataSourceCache: DataSource[] | null = null;
let observationDataTypeCache: ObservationDataType[] | null = null;

// 处理并发请求：同一时间多次调用时复用同一个 Promise
let dataSourceLoading: Promise<DataSource[]> | null = null;
let dataTypeLoading: Promise<ObservationDataType[]> | null = null;

/**
 * 获取数据源列表（带内存缓存）
 * @param force 是否强制刷新缓存
 */
export const fetchDataSources = async (force = false): Promise<DataSource[]> => {
  if (!force && dataSourceCache) {
    return dataSourceCache;
  }
  if (!force && dataSourceLoading) {
    return dataSourceLoading;
  }

  dataSourceLoading = dataSourceApi
    .list()
    .then((res) => {
      dataSourceCache = res || [];
      return dataSourceCache;
    })
    .finally(() => {
      dataSourceLoading = null;
    });

  return dataSourceLoading;
};

/**
 * 使数据源缓存失效
 * <p>
 * 当数据源在管理页面被新增、修改或删除后，应调用此方法，
 * 这样下次调用 fetchDataSources() 时会重新从后端拉取最新列表，
 * 避免场景管理、图表配置等页面的下拉框仍然显示已删除的数据源。
 */
export const invalidateDataSourceCache = () => {
  dataSourceCache = null;
  dataSourceLoading = null;
};

/**
 * 获取观测数据类型列表（带内存缓存）
 * @param force 是否强制刷新缓存
 */
export const fetchObservationDataTypes = async (
  force = false,
): Promise<ObservationDataType[]> => {
  if (!force && observationDataTypeCache) {
    return observationDataTypeCache;
  }
  if (!force && dataTypeLoading) {
    return dataTypeLoading;
  }

  dataTypeLoading = observationDataTypeApi
    .list()
    .then((res) => {
      observationDataTypeCache = res || [];
      return observationDataTypeCache;
    })
    .finally(() => {
      dataTypeLoading = null;
    });

  return dataTypeLoading;
};


