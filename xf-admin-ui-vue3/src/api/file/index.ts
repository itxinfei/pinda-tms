import request from "@/utils/request";
import type { FileInfo } from "./types";

/**
 * 统一文件上传（对接 xf-file-server）。
 *
 * 网关路由 /api/file/** → xf-file-server（StripPrefix=2），真实端点
 * POST /attachment/upload，响应 {code:0, msg, data:文件URL}（拦截器解包为 URL 字符串）。
 * 此处包装为 FileInfo{name,url} 供上传组件统一消费。
 */
const FILE_UPLOAD_URL = "/api/file/attachment/upload";

function toFileInfo(name: string, url: unknown): FileInfo {
  return {
    name: name || String(url ?? "").split("/").pop() || "file",
    url: String(url ?? ""),
  };
}

const FileAPI = {
  /**
   * 上传文件 （传入 FormData，上传进度回调）
   */
  upload(formData: FormData, onProgress?: (percent: number) => void) {
    const file = formData.get("file");
    const name = file instanceof File ? file.name : "";
    return request<unknown, string>({
      url: FILE_UPLOAD_URL,
      method: "post",
      data: formData,
      headers: { "Content-Type": "multipart/form-data" },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          onProgress?.(percent);
        }
      },
    }).then((url: string) => toFileInfo(name, url));
  },

  /**
   * 上传文件（传入 File）
   */
  uploadFile(file: File) {
    const formData = new FormData();
    formData.append("file", file);
    return request<unknown, string>({
      url: FILE_UPLOAD_URL,
      method: "post",
      data: formData,
      headers: { "Content-Type": "multipart/form-data" },
    }).then((url: string) => toFileInfo(file.name, url));
  },

  /**
   * 删除文件。
   * xf-file-server 暂无删除端点（统一文件能力含保留期治理，属后续迭代），
   * 保留方法签名兼容 Upload 组件；调用失败由拦截器统一提示。
   */
  delete(filePath?: string) {
    return request({
      url: FILE_UPLOAD_URL,
      method: "delete",
      params: { filePath },
    });
  },

  /**
   * 下载文件
   */
  download(url: string, fileName?: string) {
    return request({
      url,
      method: "get",
      responseType: "blob",
    }).then((res) => {
      const blob = new Blob([res.data]);
      const a = document.createElement("a");
      const urlObject = window.URL.createObjectURL(blob);
      a.href = urlObject;
      a.download = fileName || "下载文件";
      a.click();
      window.URL.revokeObjectURL(urlObject);
    });
  },
};

export default FileAPI;

// 重导出类型
export * from "./types";
