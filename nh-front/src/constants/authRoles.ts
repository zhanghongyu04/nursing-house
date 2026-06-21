// 统一维护前端使用的角色编码，避免页面中散落硬编码字符串。
export const AUTH_ROLES = {
  GOV_ADMIN: 'GOV_ADMIN',
  PARENT_ORG_ADMIN: 'PARENT_ORG_ADMIN',
  ORG_ADMIN: 'ORG_ADMIN',
  NURSE: 'NURSE',
} as const;

export type AuthRole = typeof AUTH_ROLES[keyof typeof AUTH_ROLES];

// 常用角色组合，供路由守卫、按钮显隐和页面能力判断复用。
export const ROLE_GROUPS = {
  ALL: [AUTH_ROLES.GOV_ADMIN, AUTH_ROLES.PARENT_ORG_ADMIN, AUTH_ROLES.ORG_ADMIN, AUTH_ROLES.NURSE] as AuthRole[],
  GOV_ONLY: [AUTH_ROLES.GOV_ADMIN] as AuthRole[],
  GOV_OR_PARENT: [AUTH_ROLES.GOV_ADMIN, AUTH_ROLES.PARENT_ORG_ADMIN] as AuthRole[],
  USER_MANAGE: [AUTH_ROLES.GOV_ADMIN, AUTH_ROLES.PARENT_ORG_ADMIN, AUTH_ROLES.ORG_ADMIN] as AuthRole[],
  GOV_PARENT_OR_ORG: [AUTH_ROLES.GOV_ADMIN, AUTH_ROLES.PARENT_ORG_ADMIN, AUTH_ROLES.ORG_ADMIN] as AuthRole[],
  ORG_OR_PARENT: [AUTH_ROLES.ORG_ADMIN, AUTH_ROLES.PARENT_ORG_ADMIN] as AuthRole[],
  NURSE_ONLY: [AUTH_ROLES.NURSE] as AuthRole[],
} as const;

// 判断当前用户是否命中任一允许角色。
export const hasAnyRole = (roles: string[] | undefined, required: readonly string[]) => {
  if (!roles || roles.length === 0) return false;
  return required.some((role) => roles.includes(role));
};

const normalizePath = (path: string) => path.replace(/^\/api\/v1/, '').replace(/\/+$/, '');

const matchPermissionPath = (pattern: string, target: string) => {
  const normalizedPattern = normalizePath(pattern);
  const normalizedTarget = normalizePath(target);
  if (normalizedPattern === normalizedTarget) {
    return true;
  }
  if (normalizedPattern.endsWith('/**')) {
    const prefix = normalizedPattern.slice(0, -3);
    return normalizedTarget === prefix || normalizedTarget.startsWith(`${prefix}/`);
  }
  const regexpText = normalizedPattern
    .replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    .replace(/\\\*/g, '[^/]*')
    .replace(/\\\{[^/]+\\\}/g, '[^/]+');
  return new RegExp(`^${regexpText}$`).test(normalizedTarget);
};

export const hasResourcePath = (resourcePaths: string[] | undefined, required: string | readonly string[]) => {
  const requiredPaths = Array.isArray(required) ? required : [required];
  if (!resourcePaths || resourcePaths.length === 0 || requiredPaths.length === 0) return false;
  return requiredPaths.some((target) => resourcePaths.some((pattern) => matchPermissionPath(pattern, target)));
};
