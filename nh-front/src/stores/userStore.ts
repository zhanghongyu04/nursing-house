// import { ref } from "vue";
// import { defineStore } from "pinia";
// import { ElMessage } from "element-plus";
// import { getCurrentUserAPI, loginAPI, logoutAPI  } from '@/api/user'
//
//
// // 定义名为user的状态关联模块,useUserStore是调用这个存储的函数
// export const useUserStore = defineStore("user", () => {
//
//     //定义userInfo对象
//     const userInfo = ref({
//         token: "",
//         username:"",
//         email:"",
//         phoneNumber:"",
//         avatar:"",
//         userId: null, // 新增的用户ID字段
//         roleId: null, // 新增的角色ID字段
//     });
//
//
//     // 加载用户信息
//     const loadUserInfo = () => {
//         const storedUserInfo = localStorage.getItem("userInfo");
//         if (storedUserInfo) {
//             const parsedInfo = JSON.parse(storedUserInfo);
//             userInfo.value = {
//                 token: parsedInfo.token,
//                 userId: parsedInfo.userId, // 加载用户ID
//                 username:parsedInfo.username,
//                 email:parsedInfo.email,
//                 phoneNumber:parsedInfo.phoneNumber,
//                 avatar:parsedInfo.avatar,
//                 roleId:parsedInfo.roleId,
//             };
//         }
//     };
//
//
//
//     // 登录方法
//     const login = async (webUserLoginDto: { username: string; password: string }) => {
//         const res = await loginAPI(webUserLoginDto);
//         if (res.code === 200) {
//             const { token } = res.data;  // 从响应的data中获取相关字段
//             userInfo.value = {
//                 token,
//             };
//             // 登录成功后获取用户信息并更新 userInfo
//             const curUserInfo=await getCurrentUserAPI();
//             if(curUserInfo.code===200){
//                 userInfo.value.userId=curUserInfo.data.userId;
//                 userInfo.value.username=curUserInfo.data.username;
//                 userInfo.value.email=curUserInfo.data.email;
//                 userInfo.value.phoneNumber=curUserInfo.data.phoneNumber;
//                 userInfo.value.avatar=curUserInfo.data.avatar;
//                 userInfo.value.roleId=curUserInfo.data.roleId;
//             }
//
//             // 更新 localStorage 中的用户信息
//             localStorage.setItem("userInfo", JSON.stringify(userInfo.value));
//         }
//     }
//
//
//
//     // 清除用户信息
//     const clearUserInfo = () => {
//         userInfo.value = {
//             token: "",
//             username:"",
//             email:"",
//             phoneNumber:"",
//             avatar:"",
//             userId: null, // 新增的用户ID字段
//             roleId: null, // 新增的角色ID字段
//         };
//         // 清除 localStorage 中的用户信息
//         localStorage.removeItem("userInfo");
//     };
//
//
//
//     //退出登录
//     const logout = async () => {
//         // 调用退出登录API
//         await logoutAPI();
//         clearUserInfo(); // 清楚用户信息
//         ElMessage.success("用户退出登录成功");
//     }
//
//
//     // 加载用户信息
//     loadUserInfo(); // 页面加载时恢复用户信息
//
//     return { userInfo, login, clearUserInfo, logout,loadUserInfo};
// });
import { ref } from "vue";
import { defineStore } from "pinia";
import { ElNotification } from "element-plus";
import { getCurrentUserAPI, loginAPI, logoutAPI  } from '@/api/user'

// 定义名为user的状态关联模块,useUserStore是调用这个存储的函数
export const useUserStore = defineStore("user", () => {

    // 定义userInfo对象
    const userInfo = ref({
        token: "",
        username:"",
        email:"",
        phoneNumber:"",
        avatar:"",
        userId: null,
        roleLabels: [] as string[],
        primaryRole: "",
        sanaId: null as number | null,
        sanaScopeIds: [] as number[],
        resourcePaths: [] as string[],
    });
    const sessionExpired = ref(false);
    let onlineStatusTimer: ReturnType<typeof window.setInterval> | null = null;

    // 本地会话空闲过期时间：30分钟（单位：毫秒）
    const EXPIRATION_TIME = 30 * 60 * 1000;
    const ONLINE_STATUS_POLL_INTERVAL = 15 * 1000;

    const normalizeRoleLabels = (labels?: string[], primaryRole?: string): string[] => {
        const source = (labels && labels.length > 0) ? labels : (primaryRole ? [primaryRole] : []);
        return Array.from(new Set(source.filter(Boolean)));
    };

    const normalizeResourcePaths = (paths?: string[]): string[] => {
        return Array.from(new Set((paths || []).filter(Boolean)));
    };

    const persistCurrentUserInfo = () => {
        const storedSession = getStoredSession();
        const expiration = storedSession?.expiration || new Date().getTime() + EXPIRATION_TIME;
        localStorage.setItem("userInfo", JSON.stringify({
            data: userInfo.value,
            expiration
        }));
    };

    const getStoredSession = () => {
        const storedData = localStorage.getItem("userInfo");
        if (!storedData) {
            return null;
        }

        try {
            return JSON.parse(storedData) as {
                data?: typeof userInfo.value;
                expiration?: number;
            };
        } catch (error) {
            console.error("解析用户信息失败", error);
            clearUserInfo();
            return null;
        }
    };

    const isSessionExpired = () => {
        const storedSession = getStoredSession();
        if (!storedSession?.data?.token) {
            return false;
        }

        const expiration = Number(storedSession.expiration || 0);
        if (!expiration) {
            return false;
        }

        return Date.now() >= expiration;
    };

    // 加载用户信息（包含过期检查）
    const loadUserInfo = () => {
        const storedSession = getStoredSession();
        if (storedSession?.data) {
            if (isSessionExpired()) {
                sessionExpired.value = true;
                clearUserInfo();
                return;
            }

            const { data } = storedSession;
            userInfo.value = {
                token: data.token,
                userId: data.userId,
                username: data.username,
                email: data.email,
                phoneNumber: data.phoneNumber,
                avatar: data.avatar,
                roleLabels: data.roleLabels || [],
                primaryRole: data.primaryRole || "",
                sanaId: data.sanaId ?? null,
                sanaScopeIds: data.sanaScopeIds || [],
                resourcePaths: normalizeResourcePaths(data.resourcePaths),
            };
            startOnlineStatusPolling();
        }
    };

    // 登录方法
    const login = async (webUserLoginDto: { username: string; password: string; captchaKey?: string; captchaCode?: string }) => {
        const res = await loginAPI(webUserLoginDto);
        if (res.code === 200) {
            const { token, roleLabels, primaryRole, sanaId, sanaScopeIds, userId, username, email, phoneNumber, avatar, resourcePaths } = res.data || {};
            const loginRoleLabels = normalizeRoleLabels(roleLabels, primaryRole);
            userInfo.value = {
                token: token || "",
                username: username || "",
                email: email || "",
                phoneNumber: phoneNumber || "",
                avatar: avatar || "",
                userId: userId ?? null,
                roleLabels: loginRoleLabels,
                primaryRole: primaryRole || loginRoleLabels[0] || "",
                sanaId: sanaId ?? null,
                sanaScopeIds: sanaScopeIds || [],
                resourcePaths: normalizeResourcePaths(resourcePaths),
            };

            // 登录成功后获取用户信息并更新 userInfo（失败时保留登录返回信息兜底）
            try {
                const curUserInfo = await getCurrentUserAPI();
                if (curUserInfo.code === 200 && curUserInfo.data) {
                    userInfo.value.userId = curUserInfo.data.userId ?? userInfo.value.userId;
                    userInfo.value.username = curUserInfo.data.username ?? userInfo.value.username;
                    userInfo.value.email = curUserInfo.data.email ?? userInfo.value.email;
                    userInfo.value.phoneNumber = curUserInfo.data.phoneNumber ?? userInfo.value.phoneNumber;
                    userInfo.value.avatar = curUserInfo.data.avatar ?? userInfo.value.avatar;
                    userInfo.value.roleLabels = normalizeRoleLabels(curUserInfo.data.roleLabels, curUserInfo.data.primaryRole);
                    userInfo.value.primaryRole = curUserInfo.data.primaryRole || userInfo.value.roleLabels[0] || "";
                    userInfo.value.sanaId = curUserInfo.data.sanaId ?? userInfo.value.sanaId;
                    userInfo.value.sanaScopeIds = curUserInfo.data.sanaScopeIds || userInfo.value.sanaScopeIds;
                    userInfo.value.resourcePaths = normalizeResourcePaths(curUserInfo.data.resourcePaths || userInfo.value.resourcePaths);
                }
            } catch (e) {
                console.warn("获取用户中心信息失败，使用登录返回数据兜底", e);
            }

            if (!userInfo.value.token || userInfo.value.roleLabels.length === 0) {
                clearUserInfo();
                throw new Error("登录态不完整，未获取到有效角色权限");
            }

            // 计算过期时间（当前时间 + 过期时长）
            const expiration = new Date().getTime() + EXPIRATION_TIME;

            // 存储用户信息和过期时间
            localStorage.setItem("userInfo", JSON.stringify({
                data: userInfo.value,
                expiration: expiration
            }));
            sessionExpired.value = false;
            startOnlineStatusPolling();
            return;
        }

        throw new Error(res?.message || "登录失败");
    }

    // 清除用户信息
    const clearUserInfo = () => {
        stopOnlineStatusPolling();
        userInfo.value = {
            token: "",
            username:"",
            email:"",
            phoneNumber:"",
            avatar:"",
            userId: null,
            roleLabels: [],
            primaryRole: "",
            sanaId: null,
            sanaScopeIds: [],
            resourcePaths: [],
        };
        // 清除 localStorage 中的用户信息
        localStorage.removeItem("userInfo");
    };

    const markSessionExpired = () => {
        sessionExpired.value = true;
        clearUserInfo();
    };

    const resetSessionExpired = () => {
        sessionExpired.value = false;
    };

    const pollOnlineStatus = async () => {
        if (!userInfo.value.token || sessionExpired.value) {
            stopOnlineStatusPolling();
            return;
        }
        try {
            const res: any = await getCurrentUserAPI({
                headers: {
                    "X-No-Sliding-Refresh": "true"
                }
            } as any);
            if (res?.code === 200 && res?.data) {
                userInfo.value.roleLabels = normalizeRoleLabels(res.data.roleLabels, res.data.primaryRole);
                userInfo.value.primaryRole = res.data.primaryRole || userInfo.value.roleLabels[0] || "";
                userInfo.value.resourcePaths = normalizeResourcePaths(res.data.resourcePaths);
                persistCurrentUserInfo();
            }
        } catch (error) {
            // 401/403 由 axios 响应拦截器统一清理登录态并跳转登录页。
            console.warn("在线状态轮询失败", error);
        }
    };

    const syncCurrentUserInfo = async () => {
        if (!userInfo.value.token) {
            return;
        }
        const res: any = await getCurrentUserAPI();
        if (res?.code === 200 && res?.data) {
            userInfo.value.userId = res.data.userId ?? userInfo.value.userId;
            userInfo.value.username = res.data.username ?? userInfo.value.username;
            userInfo.value.email = res.data.email ?? userInfo.value.email;
            userInfo.value.phoneNumber = res.data.phoneNumber ?? userInfo.value.phoneNumber;
            userInfo.value.avatar = res.data.avatar ?? userInfo.value.avatar;
            userInfo.value.roleLabels = normalizeRoleLabels(res.data.roleLabels, res.data.primaryRole);
            userInfo.value.primaryRole = res.data.primaryRole || userInfo.value.roleLabels[0] || "";
            userInfo.value.sanaId = res.data.sanaId ?? userInfo.value.sanaId;
            userInfo.value.sanaScopeIds = res.data.sanaScopeIds || userInfo.value.sanaScopeIds;
            userInfo.value.resourcePaths = normalizeResourcePaths(res.data.resourcePaths);
            persistCurrentUserInfo();
        }
    };

    const startOnlineStatusPolling = () => {
        if (!userInfo.value.token || onlineStatusTimer) {
            return;
        }
        onlineStatusTimer = window.setInterval(() => {
            pollOnlineStatus();
        }, ONLINE_STATUS_POLL_INTERVAL);
    };

    const stopOnlineStatusPolling = () => {
        if (onlineStatusTimer) {
            window.clearInterval(onlineStatusTimer);
            onlineStatusTimer = null;
        }
    };

    // 退出登录
    const logout = async () => {
        const username = userInfo.value.username || "用户";
        // 调用退出登录API
        await logoutAPI();
        clearUserInfo();

        // 退出成功通知 - 使用官方推荐方式
        ElNotification.success({
            title: "退出成功",
            message: `${username}，您已安全退出系统`,
            duration: 3000,
        });
    }

    // 刷新过期时间（可以在用户活动时调用，延长有效期）
    const refreshExpiration = () => {
        const storedSession = getStoredSession();
        if (storedSession?.data) {
            const newExpiration = new Date().getTime() + EXPIRATION_TIME;
            localStorage.setItem("userInfo", JSON.stringify({
                data: storedSession.data,
                expiration: newExpiration
            }));
        }
    };

    // 页面加载时恢复用户信息
    loadUserInfo();

    return {
        userInfo,
        login,
        clearUserInfo,
        logout,
        loadUserInfo,
        refreshExpiration,
        isSessionExpired,
        sessionExpired,
        markSessionExpired,
        resetSessionExpired,
        startOnlineStatusPolling,
        stopOnlineStatusPolling,
        syncCurrentUserInfo
    };
});
