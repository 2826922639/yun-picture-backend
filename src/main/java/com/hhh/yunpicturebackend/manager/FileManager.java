package com.hhh.yunpicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import com.hhh.yunpicturebackend.common.ResultUtils;
import com.hhh.yunpicturebackend.config.CosClientConfig;
import com.hhh.yunpicturebackend.exception.BusinessException;
import com.hhh.yunpicturebackend.exception.ErrorCode;
import com.hhh.yunpicturebackend.exception.ThrowUtils;
import com.hhh.yunpicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
@Deprecated//弃用,改为使用upload包的模板方法优化
public class FileManager {
    @Resource
    private CosClientConfig cosClientConfig;
    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     *
     * @param multipartFile    文件
     * @param uploadPathPrefix 上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        //校验文件
        validPicture(multipartFile);
        //图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        //自己拼接文件上传路径，而不是使用原始文件名称，可以增加安全性
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        String projectName = "yunPicture";
        String uploadPath = String.format(projectName + "/%s/%s", uploadPathPrefix, uploadFilename);

        //解析结果并返回
        File file = null;
        try {
            //创建一个零时文件
            file = File.createTempFile(uploadPath, null);
            //将上传的文件 multipartFile 转存到临时文件 file 中，以便后续上传到 COS（腾讯云对象存储）
            multipartFile.transferTo(file);
            //上传文件
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //获取图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //封装返回结果
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round((double) picWidth / picHeight, 2).doubleValue();

            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            uploadPictureResult.setCosKey(uploadPath);
            //返回可访问的url
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图像上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            //零时文件清理
            deleteTempFile(file);
        }

    }

    /**
     * 根据URL上传图片
     *
     * @param filUrl           文件URL
     * @param uploadPathPrefix 上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPictureByUrl(String filUrl, String uploadPathPrefix) {
        //校验文件
        validPicture(filUrl);
        //图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = FileUtil.getName(filUrl);
        //自己拼接文件上传路径，而不是使用原始文件名称，可以增加安全性
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        String projectName = "yunPicture";
        String uploadPath = String.format(projectName + "/%s/%s", uploadPathPrefix, uploadFilename);

        //解析结果并返回
        File file = null;
        try {
            //创建一个零时文件
            file = File.createTempFile(uploadPath, null);
            //下载文件存到临时文件 file中,以便后续上传到 COS（腾讯云对象存储）
            HttpUtil.downloadFile(filUrl, file);
            //上传文件
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //获取图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //封装返回结果
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round((double) picWidth / picHeight, 2).doubleValue();

            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            uploadPictureResult.setCosKey(uploadPath);
            //返回可访问的url
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图像上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            //零时文件清理
            deleteTempFile(file);
        }

    }

    /**
     * 删除图片
     *
     * @param key
     */
    public void deletePicture(String key) {
        cosManager.deletePictureObject(cosClientConfig.getBucket(), key);
    }

    /**
     * 校验文件
     *
     * @param multipartFile
     */
    public void validPicture(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空！");
        //1.校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > 10 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过10M！");
        //2.校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        //允许上传的文件后缀列表
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpg", "jpeg", "png", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件格式错误！");
    }

    /**
     * 根据Url校验文件
     *
     * @param filUrl
     */
    private void validPicture(String filUrl) {
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

    /**
     * 删除零时文件
     *
     * @param file
     */
    public static void deleteTempFile(File file) {
        if (file != null) {
            // 删除临时文件
            boolean delete = file.delete();
            if (!delete) {
                log.error("删除临时文件失败= {}", file.getAbsolutePath());
            }
        }
    }

}
