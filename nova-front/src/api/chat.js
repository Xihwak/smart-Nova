import axios from "axios";

export const apiClient = axios.create({
  baseURL: "http://localhost:9999/api",
  timeout: 15000,
});

export function buildSseUrl(path, params = {}) {
  return apiClient.getUri({
    url: path,
    params,
  });
}
