package com.hhh.yunpicturebackend.controller;

import com.hhh.yunpicturebackend.annotation.AuthCheck;
import com.hhh.yunpicturebackend.common.BaseResponse;
import com.hhh.yunpicturebackend.common.ResultUtils;
import com.hhh.yunpicturebackend.constant.UserConstant;
import com.hhh.yunpicturebackend.exception.BusinessException;
import com.hhh.yunpicturebackend.exception.ErrorCode;
import com.hhh.yunpicturebackend.manager.CosManager;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;


@RestController
@RequestMapping("/file")
@Slf4j
public class FiileController {
    @Resource
    private CosManager cosManager;
    /**
     * 文件上传测试
     * @param mutipartFile 文件
     * @return
     */
    @PostMapping("/test/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> testUploadFile(@RequestPart ("file") MultipartFile mutipartFile){
        //文件目录
        String fileName = mutipartFile.getOriginalFilename();
        String filePath = String.format("/test/%s", fileName);
        File file = null;
        try {
            //上传文件
            file = File.createTempFile(filePath,null);
            mutipartFile.transferTo(file);
            cosManager.putObject(filePath,file);
            //返回可访问的url
            return ResultUtils.success(filePath);
        } catch (Exception e) {
            log.error("上传文件失败"+filePath,e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"上传失败");
        }finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("删除临时文件失败= {}",filePath);
                }
            }
        }
    }
    /**
     * 测试文件下载
     *
     * @param filepath 文件路径
     * @param response 响应对象
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download/")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;
        try {
            COSObject cosObject = cosManager.getObject(filepath);
            cosObjectInput = cosObject.getObjectContent();
            // 处理下载到的流
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("文件下载失败, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }

}
