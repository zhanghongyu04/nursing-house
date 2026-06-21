import { defineStore } from 'pinia';
import httpInstance from '@/utils/http';
import type { AxiosResponse } from 'axios';

// ===================== 类型定义 =====================
export interface SanatoriumPageQueryDto {
    page?: number;
    pageSize?: number;
    sanaName?: string;
    sanaAffiliation?: string;
    sanaAddress?: string;
    status?: number;
}

export interface SanatoriumDetailPageQueryDto {
    page?: number;
    pageSize?: number;
    sanaName?: string;
    selfCare?: number;
}

export interface SanatoriumFormData {
    id?: number;
    sanaName: string;
    sanaAffiliation: string;
    sanaAddress: string;
    status: number;
    uscc: string;
    legalPersons: string;
    legalPhone: string;
    bedCount: number;
    bedInUse: number;
    elderCount: number;
    nursingCount: number;
    medicalCount: number;
}

export interface PageResult<T = any> {
    records: T[];
    total: number;
    size: number;
    current: number;
    pages: number;
}


const sanatoriumApi = {
    // 分页查询养老院信息
    getPage: (params: SanatoriumPageQueryDto) => {
        return httpInstance.post<PageResult>('/api/v1/sanatorium/page', params);
    },

    // 添加养老院信息
    add: (data: SanatoriumFormData) => {
        return httpInstance.post<number>('/api/v1/sanatorium/add', data);
    },

    // 修改养老院信息
    update: (data: SanatoriumFormData) => {
        return httpInstance.put<boolean>('/api/v1/sanatorium/update', data);
    },

    // 删除养老院信息
    delete: (id: number) => {
        return httpInstance.delete<boolean>(`/api/v1/sanatorium/delete?id=${id}`);
    },

    // 导出养老院信息Excel
    exportExcel: () => {
        return httpInstance.get('/api/v1/sanatorium/export', {
            responseType: 'blob'
        });
    },

    exportTemplate: () => {
        return httpInstance.get('/api/v1/sanatorium/import', {
            responseType: 'blob'
        });
    },

    // 导入养老院信息Excel
    importExcel: (file: File) => {
        const formData = new FormData();
        formData.append('file', file);
        return httpInstance.post('/api/v1/sanatorium/import', formData);
    }
};

// ===================== Pinia Store =====================
interface SanatoriumState {
    sanatoriumList: any[];
    sanaElderList: any[];
    elderDistribution: Record<string, number>;
    total: number;
    sanaElderTotal: number;
    currentPage: number;
    pageSize: number;
    searchParams: SanatoriumPageQueryDto;
    sanaElderSearchParams: SanatoriumDetailPageQueryDto;
    loading: boolean;
}

export const useSanatoriumStore = defineStore('sanatorium', {
    state: (): SanatoriumState => ({
        sanatoriumList: [],
        sanaElderList: [],
        elderDistribution: {},
        total: 0,
        sanaElderTotal: 0,
        currentPage: 1,
        pageSize: 10,
        searchParams: {
            page: 1,
            pageSize: 10,
            sanaName: '',
            sanaAffiliation: '',
            sanaAddress: '',
            status: undefined
        },
        sanaElderSearchParams: {
            page: 1,
            pageSize: 10,
            sanaName: '',
            selfCare: undefined
        },
        loading: false
    }),

    actions: {
        // 分页查询养老院信息
        async fetchSanatoriumPage(params?: Partial<SanatoriumPageQueryDto>) {
            this.loading = true;
            try {
                if (params?.page !== undefined) {
                    this.currentPage = params.page;
                }
                if (params?.pageSize !== undefined) {
                    this.pageSize = params.pageSize;
                }

                this.searchParams = {
                    ...this.searchParams,
                    ...params,
                    page: this.currentPage,
                    pageSize: this.pageSize
                };

                const response = await sanatoriumApi.getPage(this.searchParams);

                this.sanatoriumList = response.data.records;
                this.total = response.data.total;
                return response.data;
            } catch (error) {
                console.error('获取养老院列表失败:', error);
                throw error;
            } finally {
                this.loading = false;
            }
        },

        // 添加养老院
        async createSanatorium(data: SanatoriumFormData) {
            try {
                const response = await sanatoriumApi.add(data);
                if (response.data) {
                    await this.fetchSanatoriumPage();
                    return true;
                }
                return false;
            } catch (error) {
                console.error('添加养老院失败:', error);
                throw error;
            }
        },

        // 更新养老院
        async updateSanatorium(data: SanatoriumFormData) {
            try {
                if (!data.id) {
                    throw new Error('更新养老院需要提供ID');
                }

                const response = await sanatoriumApi.update(data);
                if (response.data) {
                    await this.fetchSanatoriumPage();
                    return true;
                }
                return false;
            } catch (error) {
                console.error('更新养老院失败:', error);
                throw error;
            }
        },

        // 删除养老院
        async removeSanatorium(id: number) {
            try {
                const response = await sanatoriumApi.delete(id);
                if (response.data) {
                    await this.fetchSanatoriumPage();
                    return true;
                }
                return false;
            } catch (error) {
                console.error('删除养老院失败:', error);
                throw error;
            }
        },

        // 导出Excel
        async exportExcel() {
            try {
                const response = await sanatoriumApi.exportExcel();
                this.downloadFile(response, '养老院信息列表.xlsx');
                return true;
            } catch (error) {
                console.error('导出Excel失败:', error);
                throw error;
            }
        },

        // 导出模板
        async exportTemplate() {
            try {
                const response = await sanatoriumApi.exportTemplate();
                this.downloadFile(response, '养老院导入模板.xlsx');
                return true;
            } catch (error) {
                console.error('导出模板失败:', error);
                throw error;
            }
        },

        // 下载文件通用方法
        downloadFile(response: AxiosResponse, fileName: string) {
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', fileName);
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);
        },

        async importExcel(file: File) {
            try {
                const response = await sanatoriumApi.importExcel(file);
                await this.fetchSanatoriumPage();
                return response;
            } catch (error) {
                console.error('导入Excel失败:', error);
                throw error;
            }
        },

        // 重置搜索参数
        resetSearchParams() {
            this.searchParams = {
                page: 1,
                pageSize: this.pageSize,
                sanaName: '',
                sanaAffiliation: '',
                sanaAddress: '',
                status: undefined
            };
        }
    }
});

