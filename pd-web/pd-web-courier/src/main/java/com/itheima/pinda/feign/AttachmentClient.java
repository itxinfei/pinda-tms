package com.itheima.pinda.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传 Feign 客户端（指向 pd-file-server）。
 *
 * <p>快递员端原本缺少附件上传通道，POD 签收照片无法独立留证；
 * 依据《全局定案与待确认项清单》D-07，在 pd-web-courier 独立补建，
 * 与 driver/customer 端保持一致，业务标识使用 courier。</p>
 */
@FeignClient(
        name = "${pinda.feign.authority-server:pd-file-server}",
        path = "/attachment"
)
public interface AttachmentClient {

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    Object upload(
            @RequestPart(value = "file") MultipartFile file,
            @RequestParam(value = "isSingle", required = false, defaultValue = "false") Boolean isSingle,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "bizId", required = false) String bizId,
            @RequestParam(value = "bizType", required = false) String bizType);

}
