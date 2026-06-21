import httpInstance from "@/utils/http.ts";

export interface UserManageDTO {
    id?: number;
    username?: string;
    password?: string;
    email?: string;
    phoneNumber?: string;
    avatar?: string;
    roleId?: number;
    sanaId?: number | null;
    sanaScopeIds?: number[];
}

export interface UserManagePageQuery {
    page: number;
    pageSize: number;
    username?: string;
    sanaId?: number | null;
    roleId?: number;
}

// 添加用户
export const addUserAPI = (data: UserManageDTO) => {
    return httpInstance({
        url: "/api/v1/admin/add",
        method: "POST",
        data
    });
};

// 删除用户
export const deleteUserAPI = (username: string) => {
    return httpInstance({
        url: "/api/v1/admin/delete",
        method: "POST",
        params: { username }
    });
};

// 更新用户信息
export const updateUserInfoAPI = (data: UserManageDTO) => {
    return httpInstance({
        url: "/api/v1/admin/update",
        method: "POST",
        data
    });
};

// 重置用户密码
export const resetUserPasswordAPI = (username: string) => {
    return httpInstance({
        url: "/api/v1/admin/resetPassword",
        method: "POST",
        data: { username }
    });
};

// 分页查询用户列表
export const getUserPageAPI = (data: UserManagePageQuery) => {
    return httpInstance({
        url: "/api/v1/admin/pageUser",
        method: "POST",
        data
    });
};

