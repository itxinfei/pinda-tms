package com.itheima.pinda.controller;

import com.itheima.pinda.feign.AttachmentClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 快递员端文件上传。
 *
 * <p>依据《全局定案与待确认项清单》D-07：POD 电子签收的提货/签收照片
 * 须由快递员端独立上传、独立留证、可独立追溯，网关路径
 * {@code /api/web-courier/attachment/upload}，业务标识固定 courier。</p>
 */
@Slf4j
@Tag(name = "文件上传")
@RestController
@RequestMapping("attachment")
public class AttachmentController {

    private final AttachmentClient attachmentClient;

    public AttachmentController(AttachmentClient attachmentClient) {
        this.attachmentClient = attachmentClient;
    }

    @Operation(summary = "文件上传")
    @ResponseBody
    @PostMapping("upload")
    public Object upload(@RequestParam(value = "file") MultipartFile file) {
        log.info("快递员端上传附件");
        return attachmentClient.upload(file, false, null, null, "courier");
    }
}
