import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/userStore.ts'
import { ElMessage, ElMessageBox } from 'element-plus'
import { AUTH_ROLES, hasAnyRole, hasResourcePath } from '@/constants/authRoles'

// 导入组件
import Home from "@/views/Home/Home.vue";
import UserLogin from "@/views/Login/UserLogin.vue";
import PersonalCenter from "@/views/Personalcenter/PersonalCenter.vue";
import NursingHomeList from "@/views/NursingHome/NursingHomeList.vue";
import NursingHomeDetail from "@/views/NursingHome/NursingHomeDetail.vue";
import ElderInfo from "@/views/Elder/ElderInfo.vue";
import MontiorSearch from "@/views/Monitor/MontiorSearch.vue";

// 定义路由元信息接口
interface AppRouteMeta {
    showNavbar: boolean;
    // 页面访问权限，使用 sys_resource.request_path。
    resourcePaths?: readonly string[];
    roleLabels?: readonly string[];
}

// 扩展路由类型
declare module 'vue-router' {
    interface RouteMeta extends AppRouteMeta {}
}

const enablePromptConsole = import.meta.env.VITE_ENABLE_PROMPT_CONSOLE === 'true'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'home',
            component: Home,
            meta: {
                showNavbar: true
            }
        },
        {
            path: '/login',
            name: '登录页面',
            component: UserLogin,
            meta: {
                showNavbar: false
                // 登录页不需要角色权限
            }
        },
        {
            path: '/Personalcenter',
            name: '个人中心',
            component: PersonalCenter,
            meta: {
                showNavbar: true,
                resourcePaths: ['/web/user/getUserNavInfo']
            }
        },
        {
            path: '/nursingHomeList',
            name: 'NursingHomeList',
            component: NursingHomeList,
            meta: {
                showNavbar: true,
                resourcePaths: ['/web/sanatorium/page']
            }
        },
        {
            path: '/nursingHomeDetail',
            name: 'NursingHomeDetail',
            component: NursingHomeDetail,
            meta: {
                showNavbar: true,
                resourcePaths: ['/web/sanatorium/elderDistribution', '/web/sanatorium/pageSanaElderList']
            }
        },
        {
            path: '/elderInfo',
            name: 'ElderInfo',
            component: ElderInfo,
            meta: {
                showNavbar: true,
                resourcePaths: ['/web/elder/page']
            }
        },
        {
            path: '/userManage',
            name: 'UserManage',
            component: () => import('../views/UserManage/index.vue'),
            meta: {
                showNavbar: true,
                resourcePaths: ['/web/admin/pageUser']
            }
        },
        {
            path: '/permission',
            name: 'PermissionManage',
            component: () => import('../views/Permission/index.vue'),
            meta: {
                showNavbar: true,
                resourcePaths: ['/web/permission/roles']
            }
        },
        {
            path: '/CacheControl',
            name: '缓存监控',
            component: () => import('../views/Control/CacheControl.vue'),
            meta: {
                showNavbar: true,
                resourcePaths: ['/web/monitor/getRedisInfo']
            }
        },
        {
            path: '/ServiceControl',
            name: '服务监控',
            component: () => import('../views/Control/ServiceControl.vue'),
            meta: {
                showNavbar: true,
                resourcePaths: ['/web/monitor/serverMonitor']
            }
        },
        {
            path: '/LoginMonitor',
            name: '登录与在线监控',
            component: () => import('../views/Control/LoginMonitor.vue'),
            meta: {
                showNavbar: true,
                resourcePaths: ['/web/monitor/loginLogs', '/web/monitor/onlineUsers']
            }
        },
        {
            path: '/Monitor',
            name: 'Monitor',
            component: MontiorSearch,
            meta: {
                showNavbar: true,
                resourcePaths: ['/web/video/list']
            }
        },
        {
            path: '/agent',
            name: 'agent',
            component: () => import('../views/llm/index.vue'),
            meta: {
                showNavbar: true,
                resourcePaths: ['/web/agent/sence']
            }
        },
        {
            path: '/vectorStore',
            name: 'VectorStore',
            component: () => import('../views/VectorStore/index.vue'),
            meta: {
                showNavbar: true,
                resourcePaths: ['/web/vector-store/stats', '/web/vector-store/documents']
            }
        },
        {
            path: '/nursingTaskDispatch',
            name: 'NursingTaskDispatch',
            component: () => import('../views/NursingTask/Dispatch.vue'),
            meta: {
                showNavbar: true,
                resourcePaths: ['/web/nursing-task/page']
            }
        },
        {
            path: '/nursingLog',
            name: 'NursingLog',
            component: () => import('../views/NursingLog/Index.vue'),
            meta: {
                showNavbar: true,
                resourcePaths: ['/web/nursing-log/page']
            }
        },
        {
            path: '/nursingTaskTemplate',
            name: 'NursingTaskTemplate',
            component: () => import('../views/NursingTask/Template.vue'),
            meta: {
                showNavbar: true,
                resourcePaths: ['/web/nursing-task-template/page']
            }
        },
        {
            path: '/myNursingTask',
            name: 'MyNursingTask',
            component: () => import('../views/NurseTask/MyTasks.vue'),
            meta: {
                showNavbar: true,
                resourcePaths: ['/web/nursing-task/my/page']
            }
        },
        {
            path: '/writeNursingLog',
            name: 'WriteNursingLog',
            component: () => import('../views/NurseLog/WriteLog.vue'),
            meta: {
                showNavbar: true,
                resourcePaths: ['/web/nursing-log/my/page', '/web/nursing-log/add']
            }
        },
        ...(enablePromptConsole ? [{
            path: '/promptConsole',
            name: 'PromptConsole',
            component: () => import('../views/PromptConsole/index.vue'),
            meta: {
                showNavbar: true,
                resourcePaths: ['/web/agent/prompt/page'],
                roleLabels: [AUTH_ROLES.GOV_ADMIN]
            }
        }] : [])
    ],
})

let loginExpiredDialogPromise: Promise<void> | null = null

const showLoginExpiredDialog = (userStore: ReturnType<typeof useUserStore>) => {
    if (!loginExpiredDialogPromise) {
        loginExpiredDialogPromise = ElMessageBox.alert(
            '当前登录状态已过期，请重新登录。',
            '登录已过期',
            {
                confirmButtonText: '确定',
                type: 'warning',
                showClose: false,
                closeOnClickModal: false,
                closeOnPressEscape: false
            }
        )
            .then(() => {
                userStore.resetSessionExpired()
                router.replace('/login')
            })
            .finally(() => {
                loginExpiredDialogPromise = null
            })
    }

    return loginExpiredDialogPromise
}

// 路由守卫 - 移除了显式的类型声明，由TypeScript自动推断
router.beforeEach(async (to, from, next) => {
    const userStore = useUserStore()
    const hasExpiredSession = Boolean(userStore.sessionExpired) || userStore.isSessionExpired()
    const redirectTarget = to.fullPath && to.fullPath !== '/login' ? to.fullPath : '/'

    // 1. 处理登录页访问
    if (to.path === '/login') {
        // 如果已登录，访问登录页自动跳转到首页
        if (userStore.userInfo.token && !hasExpiredSession) {
            const loginRedirect = typeof to.query.redirect === 'string' && to.query.redirect.startsWith('/')
                ? to.query.redirect
                : '/'
            next(loginRedirect)
            return
        }
        next()
        return
    }

    // 2. 检查登录状态是否过期
    if (hasExpiredSession) {
        userStore.markSessionExpired()
        next(false)
        await showLoginExpiredDialog(userStore)
        return
    }

    // 3. 检查是否已登录
    if (!userStore.userInfo.token) {
        ElMessage.warning('请先登录')
        next({
            path: '/login',
            query: {
                redirect: redirectTarget
            }
        })
        return
    }

    // 4. 检查页面资源权限。进入受控页面前必须拉取后端实时权限，避免本地缓存放行已回收权限。
    if (to.meta.resourcePaths && to.meta.resourcePaths.length > 0) {
        try {
            await userStore.syncCurrentUserInfo()
        } catch (error) {
            console.warn('刷新当前用户权限失败', error)
            ElMessage.error('权限状态刷新失败，请重新登录后再试')
            next(false)
            return
        }
        if (to.meta.roleLabels && to.meta.roleLabels.length > 0) {
            const hasRolePermission = hasAnyRole(userStore.userInfo.roleLabels, to.meta.roleLabels);
            if (!hasRolePermission) {
                ElMessage.error('没有访问权限，请联系管理员')
                next(from.path || '/')
                return
            }
        }
        const hasPermission = hasResourcePath(userStore.userInfo.resourcePaths, to.meta.resourcePaths);
        if (!hasPermission) {
            ElMessage.error('没有访问权限，请联系管理员')
            // 如果是从其他页面跳转过来的，返回原页面，否则跳转到首页
            next(from.path || '/')
            return
        }
    }

    // 5. 刷新令牌过期时间
    userStore.refreshExpiration()

    // 6. 正常放行
    next()
})

// 默认跳转到首页
router.push('/');

export default router
