import httpInstance from '@/utils/http';

// 通用字典项结构，前端统一把 itemValue 视为展示/转换原始值。
export interface DictItem {
  dictTypeCode: string;
  itemValue: string;
  itemLabel: string;
  sortNo?: number;
}

// 查询单个字典类型的全部条目。
export const getDictItems = (dictTypeCode: string) => {
  return httpInstance({
    url: '/api/v1/dict/items',
    method: 'GET',
    params: { dictTypeCode }
  });
};

// 批量查询多个字典类型，减少页面初始化时的请求次数。
export const getDictItemsBatch = (dictTypeCodes: string[]) => {
  return httpInstance({
    url: '/api/v1/dict/items/batch',
    method: 'GET',
    params: { dictTypeCodes: dictTypeCodes.join(',') }
  });
};


