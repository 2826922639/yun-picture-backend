package com.hhh.yunpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import com.hhh.yunpicturebackend.exception.BusinessException;
import com.hhh.yunpicturebackend.exception.ErrorCode;
import com.hhh.yunpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * url文件上传实现类
 */
@Service
public class UrlPictureUploadImpl extends PictureUploadTemplate {
    /**
     * 处理文件来源并生成本地零时文件
     *
     * @param inputSource
     */
    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String filUrl = (String) inputSource;

       /* try {
            *//*HttpUtil.createRequest(Method.GET, filUrl)
                    .header("User-Agent", "Mozilla/5.0")
                    .execute()
                    .writeBody(file);*//*
            HttpUtil.downloadFile(filUrl, file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }*/
        try {
            // 尝试下载文件
            HttpUtil.downloadFile(filUrl, file);
        } catch (HttpException e) {
            // 捕获 HTTP 异常
            if (e.getMessage().contains("status code: [567]")) {
                // 记录自定义状态码的详细信息
                System.err.println("服务器返回非标准状态码 567，可能存在服务器端问题");
                System.err.println("URL: " + filUrl);
                System.err.println("详细错误: " + e.getMessage());
                // 可以在这里添加重试逻辑或者其他处理方式
            }
            throw e; // 重新抛出异常，保持原有逻辑
        }

    }
    /**
     * 获取输入源的原始文件名称
     *
     * @param inputSource
     * @return
     */
    @Override
    protected String getOriginalFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
// 针对ai扩图进行获取文件名称
        if (fileUrl.contains("result-") && fileUrl.contains("OSSAccessKeyId")) {
            int start = fileUrl.indexOf("result-");
            int end = fileUrl.indexOf("?OSSAccessKeyId");
            return fileUrl.substring(start, end);
        }
        return FileUtil.getName(fileUrl);
    }
    /**
     * 校验图片
     *
     * @param inputSource
     */
    @Override
    protected void validPicture(Object inputSource) {
        String filUrl = (String) inputSource;
        //校验非空
        ThrowUtils.throwIf(filUrl == null, ErrorCode.PARAMS_ERROR, "文件地址为空！");
        //校验UrL格式
        try {
            new URL(filUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式错误！");
        }
        //校验URL协议
        ThrowUtils.throwIf(!filUrl.startsWith("http://") && !filUrl.startsWith("https://"), ErrorCode.PARAMS_ERROR, "文件地址协议错误！");
        //发送HEAD请求验证文件是否存在
        HttpResponse httpResponse = null;
        try {
            httpResponse = HttpUtil.createRequest(Method.HEAD, filUrl).execute();
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            //文件存在，文件类型校验
            String contentType = httpResponse.header("Content-Type");
            //不为空，才校验是否合法
            if (StrUtil.isNotBlank(contentType)) {
                //允许上传的文件类型列表
                final List<String> ALLOW_CONTENT_TYPE_LIST = Arrays.asList("image/jpeg", "image/png", "image/webp", "image/jpg");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPE_LIST.contains(contentType), ErrorCode.PARAMS_ERROR, "文件类型错误！");
            }
            //文件存在，文件大小校验
            String contentLengthStr = httpResponse.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long ONE_M = 1024 * 1024;
                    ThrowUtils.throwIf(contentLength > 10 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过10M！");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误！");
                }
            }
        } finally {
            if (httpResponse != null) {
                httpResponse.close();
            }
        }
    }
}
