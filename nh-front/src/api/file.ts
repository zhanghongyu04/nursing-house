import httpInstance from "@/utils/http.ts";
import axios from "axios";

//文件上传
export const upload=(data)=>{
    return httpInstance({
        url: '/api/v1/commonFile/upload',
        method:'POST',
        data
    })
}

//文件下载
export const downloadFile = (fileName) => {
    return httpInstance({
        url: '/api/v1/commonFile/download',
        method: 'POST',
        params: { fileName },
        responseType: 'blob', // 处理二进制流
    });
};

//文件删除
export const deleteFile = (fileName) => {
    return httpInstance({
        url: '/api/v1/commonFile/delete',
        method: 'DELETE',
        params: { fileName },
    });
};


// 获取上传预签名URL
export const getPresignedUploadUrl = (fileName) => {
    return httpInstance({
        url: '/api/v1/commonFile/presigned-upload-url',
        method: 'GET',
        params: { fileName },
    });
};

// 获取下载预签名URL
export const getPresignedDownloadUrl = (objectKey) => {
    return httpInstance({
        url: '/api/v1/commonFile/presigned-download-url',
        method: 'GET',
        params: { objectKey },
    });
};


// 单个文件直传RustFS，上传文件
// 返回 PresignedUploadResult，包含 presignedUrl、objectKey 和 fileUrl
export const uploadViaPresignedUrl = async (file: File) => {
    try {
        // 步骤1: 获取原始文件名
        const rawFileName = file.name;

        // 步骤2: 调用后端接口获取预签名URL和objectKey
        const res = await getPresignedUploadUrl(rawFileName);
        const result = res.data; // PresignedUploadResult

        // 调试输出：验证返回数据
        console.log('[DEBUG] 预签名上传结果:', {
            presignedUrl: result.presignedUrl,
            objectKey: result.objectKey,
            fileUrl: result.fileUrl
        });

        // 步骤3: 直接向RustFS发送PUT请求（绕过代理）
        const response = await axios.put(result.presignedUrl, file, {
            // 关键配置：禁用代理和默认请求头
            baseURL: '', // 清除默认baseURL
            headers: {
                'Content-Type': file.type || 'application/octet-stream'
            }
        });

        // 步骤4: 返回上传结果（包含 objectKey，后续下载需要使用）
        return {
            success: true,
            objectKey: result.objectKey,
            fileUrl: result.fileUrl
        };
    } catch (error) {
        console.error('[ERROR] 上传失败:', error);
        throw new Error(`上传失败: ${(error as any).response?.data || (error as any).message}`);
    }
};



// 使用预签名URL直连RustFS下载文件
// 注意：参数改为 objectKey（而不是原始文件名）
export const downloadViaPresignedUrl = async (objectKey: string) => {
    try {
        // 1. 获取预签名URL：调用后端接口生成临时有效的下载URL
        const res = await getPresignedDownloadUrl(objectKey);
        const presignedUrl = res.data;

        console.log('[DEBUG] 预签名下载URL:', presignedUrl);

        // 2. 创建隐藏链接触发下载
        const link = document.createElement('a');
        link.href = presignedUrl;       // 设置URL
        link.download = extractOriginalFileName(objectKey); // 提取原始文件名
        document.body.appendChild(link); // 将链接添加到DOM
        link.click();                    // 模拟点击触发下载
        document.body.removeChild(link); // 移除临时链接
        return true;                     // 表示下载已触发
    } catch (error) {
        console.error('[ERROR] 下载失败:', error);
        throw new Error('下载失败: ' + (error as any).message);
    }
};

/**
 * 从 objectKey 中提取原始文件名
 * 例如：20250312/550e8400-e29b-41d4-a716-446655440000-photo.jpg
 *      提取出：photo.jpg
 */
const extractOriginalFileName = (objectKey: string): string => {
    // 对象键格式：yyyyMMdd/UUID-filename
    const lastSlashIndex = objectKey.lastIndexOf('/');
    if (lastSlashIndex >= 0 && lastSlashIndex < objectKey.length - 1) {
        const fileNamePart = objectKey.substring(lastSlashIndex + 1);
        // 找到第一个 '-' 后的位置（UUID后面）
        const firstDashIndex = fileNamePart.indexOf('-');
        if (firstDashIndex >= 0 && firstDashIndex < fileNamePart.length - 1) {
            return fileNamePart.substring(firstDashIndex + 1);
        }
    }
    // 如果无法解析，返回原始对象键
    return objectKey;
};

/**
 * 从完整的文件 URL 中提取 objectKey
 * 例如：http://localhost:9000/nursing-home/20250312/uuid-xxx.jpg
 *      提取出：20250312/uuid-xxx.jpg
 */
export const extractObjectKeyFromUrl = (fileUrl: string): string => {
    try {
        const raw = (fileUrl || '').trim();
        if (!raw) {
            return '';
        }

        // 已是后端代理路径：/api/v1/commonFile/image/{objectKey}
        const proxyPrefix = '/api/v1/commonFile/image/';
        if (raw.startsWith(proxyPrefix)) {
            return decodeURIComponent(raw.substring(proxyPrefix.length));
        }

        // 已经是 objectKey（如 20260401/uuid-logo.png）
        if (!raw.startsWith('http://') && !raw.startsWith('https://')) {
            return raw.replace(/^\/+/, '');
        }

        // 完整 URL，仅按当前 bucket 规则提取 objectKey
        const parsedUrl = new URL(raw);
        const pathname = parsedUrl.pathname || '';
        const markerCandidates = ['/nursing-home/'];
        for (const marker of markerCandidates) {
            const idx = pathname.indexOf(marker);
            if (idx >= 0) {
                const key = pathname.substring(idx + marker.length);
                if (key) {
                    return decodeURIComponent(key);
                }
            }
        }

        // 非本系统对象存储URL，不做 objectKey 推断，交给原始地址回显
        // 避免将第三方外链（如 picsum）误转为 /api/v1/commonFile/image/** 导致 404
        return '';
    } catch (error) {
        console.error('[ERROR] 提取objectKey失败:', error);
        return '';
    }
};

/**
 * 统一构建前端可展示的图片地址
 * 优先走后端代理 /api/v1/commonFile/image/{objectKey}，避免跨域直连问题
 */
export const buildImageProxySrc = (rawUrl?: string): string => {
    const raw = (rawUrl || '').trim();
    if (!raw) {
        return '';
    }
    if (raw.startsWith('/api/v1/commonFile/image/')) {
        return raw;
    }
    const objectKey = extractObjectKeyFromUrl(raw);
    if (objectKey) {
        return `/api/v1/commonFile/image/${encodeURI(objectKey)}`;
    }
    return raw;
};

export const downloadFileByUrl = async (fileUrl: string) => {
    const objectKey = extractObjectKeyFromUrl(fileUrl);
    if (!objectKey) {
        throw new Error('无法解析附件地址');
    }

    const response = await downloadFile(objectKey);
    const blob = response.data instanceof Blob
        ? response.data
        : new Blob([response.data], {
            type: response.headers?.['content-type'] || 'application/octet-stream',
        });
    const downloadUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = extractOriginalFileName(objectKey);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(downloadUrl);
};


