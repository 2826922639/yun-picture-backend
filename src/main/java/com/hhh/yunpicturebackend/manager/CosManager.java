package com.hhh.yunpicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import com.hhh.yunpicturebackend.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class CosManager {
    @Resource
    private CosClientConfig cosClientConfig;
    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        return cosClient.putObject(putObjectRequest);
    }
    /**
     * 下载对象
     *
     * @param key 唯一键
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传对象(解析图片信息)
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        //对图片进行处理（获取图片的基本信息）
        PicOperations picOperations = new PicOperations();
        //1表示返回原图信息
        picOperations.setIsPicInfo(1);
        //1.图片压缩（转成webp格式）
        List<PicOperations.Rule> rules=new ArrayList<>();
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule=new PicOperations.Rule();
        compressRule.setFileId(webpKey);
        compressRule.setBucket(cosClientConfig.getBucket()) ;
        compressRule.setRule("imageMogr2/format/webp");
        rules.add(compressRule);
        //2.缩略图处理
        if (file.length() > 20 * 1024) {
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            thumbnailRule.setFileId(FileUtil.mainName(key) + "_thumbnail.webp");
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s", 256, 256));
            rules.add(thumbnailRule);
        }
        //设置图片处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        PutObjectResult result = cosClient.putObject(putObjectRequest);

        // 上传后删除原始文件
        //cosClient.deleteObject(cosClientConfig.getBucket(), key);
        return result;
    }
    /**
     * 删除对象
     *
     * @param bucketName 存储桶名称
     * @param key 唯一键
     */
    public void deletePictureObject(String bucketName, String key){
        cosClient.deleteObject(bucketName, key);
    }
}
