package com.hhh.yunpicturebackend.api.imagesearch;

import com.hhh.yunpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.hhh.yunpicturebackend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.hhh.yunpicturebackend.api.imagesearch.sub.GetImageListApi;
import com.hhh.yunpicturebackend.api.imagesearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ImagSearchApiFacade {

    /**
     * 图片搜索
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl){
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        return GetImageListApi.getImageList(imageFirstUrl);
    }
    public static void main(String[] args) {
        List<ImageSearchResult> imageSearchResults = searchImage("https://www.codefather.cn/logo.png");
        System.out.println("搜索成功" + imageSearchResults);
    }
}
