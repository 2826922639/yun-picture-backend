package com.hhh.yunpicturebackend.ai.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hhh.yunpicturebackend.ai.AiChatWithImageService;
import com.hhh.yunpicturebackend.ai.AiCodeGeneratorServiceFactory;
import com.hhh.yunpicturebackend.model.entity.Picture;
import com.hhh.yunpicturebackend.service.PictureService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.ImageContent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 情绪配图搜索工具 (RGB 欧几里得距离版)
 * 使用颜色空间距离算法，在数据库中查找视觉上最接近目标情绪颜色的图片。
 */
@Slf4j
@Component
public class MoodPictureSearchTool extends BaseTool {

    @Resource
    private PictureService pictureService;

    @Resource
    @Lazy
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    // 情绪与基准颜色(Hex)的映射表
    private static final Map<String, String> MOOD_COLOR_MAP = new HashMap<>();

    static {
        MOOD_COLOR_MAP.put("开心", "#602000"); // 暖棕红色 (示例)
        MOOD_COLOR_MAP.put("惊讶", "#6e5b48");
        MOOD_COLOR_MAP.put("平静", "#513b2b");
        MOOD_COLOR_MAP.put("悲伤", "#e0e0e0"); // 浅灰
        MOOD_COLOR_MAP.put("厌恶", "#413839");
        MOOD_COLOR_MAP.put("愤怒", "#200000"); // 深红/黑
        MOOD_COLOR_MAP.put("恐惧", "#000000"); // 纯黑
        MOOD_COLOR_MAP.put("轻蔑", "#FFFFFF"); // 纯白
    }

    @Tool("根据情绪搜索匹配色系的图片")
    public String searchPictureByMood(
            @P("用户的情绪关键词（开心、惊讶、平静、悲伤、厌恶、愤怒、恐惧、轻蔑）") String mood
    ) {
        if (StrUtil.isBlank(mood) || !MOOD_COLOR_MAP.containsKey(mood)) {
            return "支持的情绪：开心、惊讶、平静、悲伤、厌恶、愤怒、恐惧、轻蔑。";
        }

        String targetHex = MOOD_COLOR_MAP.get(mood);
        Color targetColor = Color.decode(targetHex);
        int r = targetColor.getRed();
        int g = targetColor.getGreen();
        int b = targetColor.getBlue();

        log.info("情绪搜图: {} -> RGB({}, {}, {})", mood, r, g, b);

        try {
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();

            // --- 核心算法 ---
            // 假设数据库字段 picColor 格式为 '0xRRGGBB' (长度8) 或 '0xRGB'
            // 使用 MySQL 函数提取 RGB 并计算欧几里得距离的平方
            // 距离公式: (R1-R2)^2 + (G1-G2)^2 + (B1-B2)^2
            // conv(substr(pic_color, 3, 2), 16, 10) -> 提取 R 分量并转十进制
            String sqlDistance = String.format(
                    "(POW(CONV(SUBSTR(picColor, 3, 2), 16, 10) - %d, 2) + " +
                            " POW(CONV(SUBSTR(picColor, 5, 2), 16, 10) - %d, 2) + " +
                            " POW(CONV(SUBSTR(picColor, 7, 2), 16, 10) - %d, 2))",
                    r, g, b
            );

            queryWrapper.select("id", "sUrl", "picColor"); // 只查有用字段
            // 过滤掉格式不对的数据 (必须是 0x 开头且长度足够)
            queryWrapper.likeRight("picColor", "0x");
            queryWrapper.apply("LENGTH(picColor) = 8");
            queryWrapper.eq("spaceId", 0);

            // 按颜色距离升序排序 (越小越接近)
            queryWrapper.orderByAsc(sqlDistance);

            // 取前 5 张
            queryWrapper.last("LIMIT 5");

            List<Picture> pictures = pictureService.list(queryWrapper);

            if (pictures.isEmpty()) {
                return "未找到相关配色的图片";
            }
            // 1. 准备收集结果的列表
            List<Map<String, Object>> resultList = new ArrayList<>();
            for (Picture picture : pictures) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", picture.getId());
                map.put("sUrl", picture.getSUrl());
                map.put("color", picture.getPicColor());

                try {
                    // 只处理当前循环的单张图片
                    ImageContent imageContent = ImageContent.from(picture.getSUrl());
                    AiChatWithImageService service = aiCodeGeneratorServiceFactory.createAiPictureUnderstandService();
                    String understandResult = service.chatWithImage("用一句话描述这张图片的氛围和内容", imageContent);
                    map.put("understandResult", understandResult);
                } catch (Exception e) {
                    log.warn("图片ID:{} 识别失败", picture.getId(), e);
                    map.put("understandResult", "这张图片很适合你当下的心情，愿它能给你带来温暖");
                }
                // 单张处理完立即加入结果集
                resultList.add(map);
            }
            return JSONUtil.toJsonStr(resultList);

        } catch (Exception e) {
            log.error("颜色相似度计算失败", e);
            // 降级方案：如果不使用 MySQL 或 SQL 报错，可以尝试简单的 Random 搜索（可选）
            return "搜索图片出错: " + e.getMessage();
        }
    }

    @Override
    public String getToolName() {
        return "searchPictureByMood";
    }

    @Override
    public String getDisplayName() {
        return "情绪配图搜索";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        return String.format("[工具调用] %s 情绪: %s", getDisplayName(), arguments.getStr("mood"));
    }
}