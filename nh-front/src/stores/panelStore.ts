// src/stores/panelStore.ts
import { defineStore } from 'pinia';
import httpInstance from '@/utils/http';

// 大屏数据接口
export interface PanelNavInfo {
    elderCount: number;
    sanaCount: number;
    nurseCount: number;
    medicineCount: number;
    affiliationCount: number;
    useRate: number;
}

export interface RegionSanaCount {
    regionName: string;
    sanaCount: number;
}

export const usePanelStore = defineStore('panel', {
    state: () => ({
        navInfo: {} as PanelNavInfo,
        regionSanaCountList: [] as RegionSanaCount[],
        loading: false
    }),

    actions: {
        // 获取大屏头部信息
        async fetchNavInfo() {
            this.loading = true;
            try {
                const response = await httpInstance.get<PanelNavInfo>('/api/v1/panel/getNavDistribution');
                this.navInfo = response.data;
                return response.data;
            } catch (error: any) {
                console.error('获取大屏数据失败:', error);
                throw new Error(error.response?.data?.message || '获取大屏数据失败');
            } finally {
                this.loading = false;
            }
        },

        // 获取区域养老院数量统计
        async fetchRegionSanaCount() {
            try {
                const response = await httpInstance.get<RegionSanaCount[]>('/api/v1/panel/regionSanaCount');
                this.regionSanaCountList = response.data || [];
                return this.regionSanaCountList;
            } catch (error: any) {
                console.error('获取区域统计失败:', error);
                this.regionSanaCountList = [];
                return [];
            }
        }
    }
});

