package com.hhh.yunpicturebackend.api.imagesearch.sub;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.hhh.yunpicturebackend.exception.BusinessException;
import com.hhh.yunpicturebackend.exception.ErrorCode;
import com.hhh.yunpicturebackend.exception.ThrowUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 获取以图搜图页面地址（setp1）
 */
public class GetImagePageUrlApi {
    /**
     * 获取以图搜图页面地址
     * @param imageUrl 图片地址
     * @return 以图搜图页面地址
     */
    public static String getImagePageUrl(String imageUrl) {
        //1.准备请求参数
        Map<String,Object>formData = new HashMap<>();
        formData.put("image",imageUrl);
        formData.put("tn","pc");
        formData.put("from","pc");
        formData.put("image_source","PC_UPLOAD_URL");
        //获取当前时间戳
        long uptime = System.currentTimeMillis();
        //请求地址
        String url = "https://graph.baidu.com/upload?uptime="+uptime;
        String acsToken ="ZSLN1fsqIgt1pEzGiXdiT/wP/l9chwQYyDf07pS8NxL8MHl9Hv2xM+HXgcmdikytcaa1rv3KwuzcKAru/GqleteDwzjVkAmbBaAu2mBc8xTWfGvkHrAhhk6Pj+PxddkpLuK8njDckx3Dj8hULWZ/VKm7X+avSlznTBLt9yyiUt5Myefp+beXOfTr1BPxt/oDLf4p75vt+lUvCHFro+XEM9Fkw+dWBEX879EZPZ5ae3qXzzsJXB0iORoWo6eXoNsN0lWxKjfmZjWwLyYlAQufr/ElSDEZgWupWmhQPouNqdY89s6RJS+aUWYqkgw+8mmaADZVD3pB/vZPeH14Nh8Wst5fRlly52XSKReOlTt7wubqb8anYeqYCXaO4tNRLkRROi4WmRNE8w7kLT2DqPewLvvfdneLYoE4PVQOuAAPE8BvQmEc0d5LTAq2qr8KylMU";
        try {
            //2.发送请求
            HttpResponse httpResponse = HttpRequest.post(url).form(formData).header("Acs-token", acsToken).timeout(5000).execute();
            ThrowUtils.throwIf(httpResponse.getStatus() != HttpStatus.HTTP_OK, ErrorCode.OPERATION_ERROR, "接口调用失败！");
            //解析响应
            String body = httpResponse.body();
            Map<String,Object> result = JSONUtil.toBean(body, Map.class);
            ThrowUtils.throwIf(result==null || !Integer.valueOf(0).equals(result.get("status")), ErrorCode.OPERATION_ERROR, "接口调用失败！");
            //3.处理响应结果
            Map<String,Object> data = (Map<String, Object>) result.get("data");
            String rawUrl = (String) data.get("url");
            //对url解码
            String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            ThrowUtils.throwIf(StrUtil.isBlank(searchResultUrl), ErrorCode.OPERATION_ERROR, "未返回有效的结果地址！");
            return searchResultUrl;
        } catch (HttpException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败！");
        }
    }
    public static void main(String[] args) {
        String imageUrl = "https://www.codefather.cn/logo.png";
        String searchResultUrl = getImagePageUrl(imageUrl);
        System.out.println(searchResultUrl);
    }
}
