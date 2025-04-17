package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/common")
@Slf4j
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;

    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file){
        try {
            log.info("上传文件:{}", file.getOriginalFilename());
            String url = aliOssUtil.upload(file.getBytes(), file.getOriginalFilename());
            return Result.success(url);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上传文件失败:{}", e.getMessage());
            return Result.error("上传失败");
        }
    }
}
