package com.hhh.yunpicturebackend.service;

public interface ScreenshotService {
    /**
     * 生成网页截图并上传
     *
     * @param webUrl 网页URL
     * @return 截图上传的URL
     */
    public abstract String generateAndUploadScreenshot(String webUrl);
}
