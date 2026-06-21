import httpInstance from "@/utils/http.ts";

//登录请求
export const loginAPI = (data) => {
    return httpInstance({
        url: "/api/v1/login",
        method: "POST",
        data
    });
};


//退出登录
export const logoutAPI=()=>{
    return httpInstance({
        url: "/api/v1/logout",
        method: "POST"
    });
}



//获取当前用户信息
export const getCurrentUserAPI=(config = {})=>{
    return httpInstance({
        url: "/api/v1/user/getUserNavInfo",
        method: "GET",
        ...config
    });
}

//更新用户信息
export const updateUserAPI=(data)=>{
    return httpInstance({
        url: "/api/v1/user/updateUserInfo",
        method: "POST",
        data
    });
}


//修改密码
export const updatePasswordAPI = (data) => {
    return httpInstance({
        url: "/api/v1/user/resetPassword",
        method: "POST",
        data
    });
};

/**
 * 获取验证码（JSON 格式）
 * 返回包含 base64 图片和 captchaKey 的数据
 */
export const getCaptchaAPI = (): Promise<{ captchaKey: string; captchaImage: string }> => {
    return httpInstance({
        url: "/api/v1/captcha",
        method: "GET"
    }).then((res: any) => {
        if (res.code === 200 && res.data) {
            return {
                captchaKey: res.data.captchaKey,
                captchaImage: res.data.captchaImage
            };
        }
        throw new Error(res?.message || "验证码获取失败");
    });
};

