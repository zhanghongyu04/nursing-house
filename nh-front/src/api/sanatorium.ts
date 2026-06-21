import httpInstance from '@/utils/http';
import type {
    SanatoriumPageQueryDto,
    SanatoriumDetailPageQueryDto,
    PageResult
} from '@/stores/sanatoriumStore.ts';

export const pageSanatoriumAPI = (params: SanatoriumPageQueryDto) => {
    return httpInstance.post<PageResult>('/api/v1/sanatorium/page', params);
};
// 分页查询养老院信息
export const getSanatoriumPage = (params: SanatoriumPageQueryDto) => {
    return httpInstance.post<PageResult>('/api/v1/sanatorium/page', params);
};

// 添加养老院信息
export const addSanatorium = (data: any) => {
    return httpInstance.post<boolean>('/api/v1/sanatorium/add', data);
};

// 修改养老院信息
export const updateSanatorium = (data: any) => {
    return httpInstance.put<boolean>('/api/v1/sanatorium/update', data);
};

// 删除养老院信息
export const deleteSanatorium = (id: number) => {
    return httpInstance.delete<boolean>(`/api/v1/sanatorium/delete?id=${id}`);
};


// Excel导出（养老院信息）
export const exportToExcel = (data: Partial<SanatoriumPageQueryDto> = {}) => {
    return httpInstance({
        url: '/api/v1/sanatorium/export',  // 对应养老院Excel导出接口
        method: 'POST',
        data,
        responseType: 'blob',  // 二进制流响应
        headers: {
            'Accept': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'  // Excel的MIME类型
        }
    });
};


// 导入养老院信息Excel
export const importSanatoriumExcel = (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return httpInstance.post('/api/v1/sanatorium/import', formData);
};

// 获取不同自理能力老人分布
export const getElderDistribution = (sanaName: string) => {
    return httpInstance.get<Record<string, number>>(
        `/api/v1/sanatorium/elderDistribution?sanaName=${sanaName}`
    );
};

// 养老院详情老人分页查询
export const getSanaElderList = (params: SanatoriumDetailPageQueryDto) => {
    return httpInstance.post<PageResult>(
        '/api/v1/sanatorium/pageSanaElderList',
        params
    );
};

