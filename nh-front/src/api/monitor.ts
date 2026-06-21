import httpInstance from "@/utils/http";

export const getLoginLogsAPI = (params: {
  pageNum?: number;
  pageSize?: number;
  username?: string;
  successFlag?: number | "";
}) => {
  return httpInstance({
    url: "/api/v1/monitor/loginLogs",
    method: "GET",
    params,
  });
};

export const getOnlineUsersAPI = () => {
  return httpInstance({
    url: "/api/v1/monitor/onlineUsers",
    method: "GET",
  });
};

export const kickOutUserAPI = (username: string) => {
  return httpInstance({
    url: "/api/v1/monitor/kickOut",
    method: "POST",
    params: { username },
  });
};
