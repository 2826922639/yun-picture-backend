package com.hhh.yunpicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.hhh.yunpicturebackend.config.CosClientConfig;
import com.hhh.yunpicturebackend.exception.BusinessException;
import com.hhh.yunpicturebackend.exception.ErrorCode;
import com.hhh.yunpicturebackend.exception.ThrowUtils;
import com.hhh.yunpicturebackend.manager.CosManager;
import com.hhh.yunpicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    private CosClientConfig cosClientConfig;
    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     *
     * @param inputSource      文件
     * @param uploadPathPrefix 上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        //1.校验文件
        validPicture(inputSource);
        //2.图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginalFilename(inputSource);
        //自己拼接文件上传路径，而不是使用原始文件名称，可以增加安全性
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        String projectName = "yunPicture";
        String uploadPath = String.format(projectName + "/%s/%s", uploadPathPrefix, uploadFilename);
        String sKey=String.format(projectName + "/%s/%s", uploadPathPrefix, FileUtil.mainName(uploadFilename) + ".webp");
        //获取图片格式
        String picFormat = FileUtil.getSuffix(originalFilename);
        String Key=String.format(projectName + "/%s/%s", uploadPathPrefix, FileUtil.mainName(uploadFilename) + "."+picFormat);
        //3.上传文件解析结果并返回
        File file = null;
        try {
            //创建一个零时文件
            file = File.createTempFile(uploadPath, null);
            //处理文件来源
            processFile(inputSource, file);
            //上传文件到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //获取图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //获取图片压缩处理结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();

            if (CollUtil.isNotEmpty(objectList)){
                    CIObject compressedCiObject = objectList.get(0);
                    CIObject thumbnailCiObject = compressedCiObject;
                    String thumbnailKey = sKey;
                    if (objectList.size() > 1) {
                        thumbnailKey = String.format(projectName + "/%s/%s", uploadPathPrefix, FileUtil.mainName(uploadFilename) + "_thumbnail.webp");
                        thumbnailCiObject = objectList.get(1);
                    }
                    return buildResult(imageInfo,originalFilename, compressedCiObject, thumbnailCiObject, Key, thumbnailKey,sKey, file);
            }
            //封装返回结果
            return buildResult(imageInfo, Key, originalFilename, file);
        } catch (Exception e) {
            log.error("图像上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            //4.零时文件清理
            deleteTempFile(file);
        }

    }

    /**
     * 封装返回结果
     *
     * @return
     */
    private UploadPictureResult buildResult(ImageInfo imageInfo,String originalFilename, CIObject compressedCiObject, CIObject thumbnailCiObject, String Key,String thumbnailKey,String sKey, File file) {
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round((double) picWidth / picHeight, 2).doubleValue();

        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + Key);
        uploadPictureResult.setCosKey(Key);
        uploadPictureResult.setSUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        uploadPictureResult.setSKey(sKey);
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        uploadPictureResult.setThumbnailKey(thumbnailKey);
        uploadPictureResult.setPicColor(imageInfo.getAve());
        //返回可访问的url
        return uploadPictureResult;
    }

    /**
     * 封装返回结果
     *
     * @param imageInfo
     * @param Key
     * @param originalFilename
     * @param file
     * @return
     */
    private UploadPictureResult buildResult(ImageInfo imageInfo, String Key, String originalFilename, File file) {
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round((double) picWidth / picHeight, 2).doubleValue();

        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + Key);
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setCosKey(Key);
        uploadPictureResult.setPicColor(imageInfo.getAve());
        //返回可访问的url
        return uploadPictureResult;
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

    /**
     * 删除图片
     *
     * @param key
     */
    public void deletePicture(String key, String thumbnailKey, String sKey) {
        //先删原图
        cosManager.deletePictureObject(cosClientConfig.getBucket(), key);
        //再删缩略图
        cosManager.deletePictureObject(cosClientConfig.getBucket(), sKey);
        //再删缩缩略图
        cosManager.deletePictureObject(cosClientConfig.getBucket(), thumbnailKey);
    }

    /**
     * 处理文件来源并生成本地零时文件
     *
     * @param inputSource
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 获取输入源的原始文件名称
     *
     * @param inputSource
     * @return
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 校验图片
     *
     * @param inputSource
     */
    protected abstract void validPicture(Object inputSource);


}
