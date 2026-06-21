import httpInstance from "@/utils/http.ts";

export interface PermissionRole {
  id?: number | string;
  roleId?: number | string;
  roleCode?: string;
  code?: string;
  roleName?: string;
  name?: string;
  description?: string;
}

export interface PermissionResourceNode {
  resourceNo: string;
  resourceName?: string;
  name?: string;
  resourceType?: string | number;
  type?: string | number;
  requestPath?: string;
  path?: string;
  label?: string;
  children?: PermissionResourceNode[];
}

export const getPermissionRolesAPI = () => {
  return httpInstance({
    url: "/api/v1/permission/roles",
    method: "GET",
  });
};

export const getPermissionResourceTreeAPI = () => {
  return httpInstance({
    url: "/api/v1/permission/resources/tree",
    method: "GET",
  });
};

export const getRoleResourceNosAPI = (roleId: number | string) => {
  return httpInstance({
    url: `/api/v1/permission/roles/${roleId}/resources`,
    method: "GET",
  });
};

export const updateRoleResourceNosAPI = (roleId: number | string, resourceNos: string[]) => {
  return httpInstance({
    url: `/api/v1/permission/roles/${roleId}/resources`,
    method: "PUT",
    data: { resourceNos },
  });
};
