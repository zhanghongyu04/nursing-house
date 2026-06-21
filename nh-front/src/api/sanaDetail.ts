import httpInstance from "@/utils/http.ts";

// 获取不同自理能力老人数量分布
export const getElderDistributionAPI = (sanaName: string) => {
    return httpInstance({
        url: "/api/v1/sanatorium/elderDistribution",
        method: "GET",
        params: {
            sanaName,
        },
    });
};

// 养老院分页详情老人信息分页查询
export const pageSanaElderListAPI = (data: any) => {
    return httpInstance({
        url: "/api/v1/sanatorium/pageSanaElderList",
        method: "POST",
        data,
    });
};

// 分页查询养老院信息
export const pageSanatoriumAPI = (data: any) => {
    return httpInstance({
        url: "/api/v1/sanatorium/page",
        method: "POST",
        data,
    });
};

// 新增养老院图片信息
export const addSanaImageAPI = (data: any) => {
    return httpInstance({
        url: "/api/v1/sanaImage/add",
        method: "POST",
        data,
    });
};

// 删除养老院图片信息
export const deleteSanaImageAPI = (data: any) => {
    return httpInstance({
        url: "/api/v1/sanaImage/delete",
        method: "DELETE",
        data,
    });
};

// 分页查询养老院图片信息
export const pageSanaImageAPI = (data: any) => {
    return httpInstance({
        url: "/api/v1/sanaImage/page",
        method: "POST",
        data,
    });
};

