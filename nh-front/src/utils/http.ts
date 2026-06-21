import axios from "axios";
import { ElMessage, ElMessageBox } from "element-plus";
import { useUserStore } from "@/stores/userStore";
import router from "@/router";

const httpInstance = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || "",
    timeout: 60000,
});

// 登录、退出和验证码接口不参与登录态续期，避免无意义刷新 token 有效期。
const shouldRefreshSession = (requestUrl: unknown) => {
    if (typeof requestUrl !== "string" || !requestUrl) {
        return false;
    }

    return !requestUrl.includes("/api/v1/login")
        && !requestUrl.includes("/api/v1/logout")
        && !requestUrl.includes("/api/v1/captcha");
};

const shouldSkipErrorMessage = (config: unknown) => {
    const requestConfig = config as { silentErrorMessage?: boolean } | undefined;
    return Boolean(requestConfig?.silentErrorMessage);
};

let authExpiredDialogPromise: Promise<void> | null = null;

const redirectToLogin = async () => {
    const userStore = useUserStore();
    if (!userStore.userInfo.token && router.currentRoute.value.path === "/login") {
        return;
    }

    userStore.markSessionExpired();
    if (!authExpiredDialogPromise) {
        authExpiredDialogPromise = ElMessageBox.alert(
            "当前登录状态已失效，请重新登录。",
            "登录已失效",
            {
                confirmButtonText: "确定",
                type: "warning",
                showClose: false,
                closeOnClickModal: false,
                closeOnPressEscape: false
            }
        )
            .then(() => {
                userStore.resetSessionExpired();
                router.replace({
                    path: "/login",
                    query: router.currentRoute.value.path === "/login"
                        ? {}
                        : { redirect: router.currentRoute.value.fullPath }
                });
            })
            .finally(() => {
                authExpiredDialogPromise = null;
            });
    }
    await authExpiredDialogPromise;
};

httpInstance.interceptors.request.use(
    (config) => {
        const userStore = useUserStore();
        const { token } = userStore.userInfo;
        // 所有业务请求统一透传当前登录 token，后端按约定从 Authorization 读取。
        if (token) {
            config.headers.Authorization = token;
        }
        // 上传文件时交给浏览器自动生成 multipart boundary，避免手动指定导致解析失败。
        if (config.data instanceof FormData) {
            config.headers.delete('Content-Type');
        } else {
            config.headers['Content-Type'] = 'application/json';
        }
        return config;
    },
    (error) => Promise.reject(error)
);


// 响应拦截器
httpInstance.interceptors.response.use(
    (response) => {
        const userStore = useUserStore();
        // 只要当前用户仍处于活跃请求中，就在前端同步刷新本地会话过期时间显示。
        if (
            userStore.userInfo.token
            && shouldRefreshSession(response?.config?.url)
            && response?.config?.headers?.["X-No-Sliding-Refresh"] !== "true"
        ) {
            userStore.refreshExpiration();
        }
        // 文件下载场景直接返回原始响应，交由调用方自行处理二进制流。
        if (response.config?.responseType === 'blob' || response.config?.responseType === 'arraybuffer') {
            return response;
        }
        return response.data;
    }, // 返回服务端响应数据
    (error) => {
        const requestUrl = error?.config?.url || "";
        const isLoginRequest = typeof requestUrl === "string" && requestUrl.includes("/api/v1/login");
        const status = error?.response?.status;

        if (!isLoginRequest && status === 401) {
            redirectToLogin();
            return Promise.reject(error);
        }

        if (!isLoginRequest && status === 403) {
            ElMessage.error("当前账号无权限访问该功能");
            return Promise.reject(error);
        }

        // 登录失败提示由登录页单点处理，避免重复弹窗
        if (!isLoginRequest && !shouldSkipErrorMessage(error?.config)) {
            ElMessage.error(error.response?.data?.message || "网络错误，请稍后再试。");
        }
        return Promise.reject(error);
    }
);


export default httpInstance;

