package com.hhh.yunpicturebackend;

import com.hhh.yunpicturebackend.ai.core.AiChatFacade;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

@Slf4j
@SpringBootTest
public class AiServiceTest {

    @Resource
    private AiChatFacade aiChatFacade;

  /*  @Test
    public void testRouteCodeGenType() throws InterruptedException {
        String userPrompt = "用户现在的情绪是开心";
        String imageUrl = "https://yun-picture-1351791618.cos.ap-shanghai.myqcloud.com/yunPicture/space/1945456890423787522/2025-08-26_axfmc5oMAEGoKJuW.png";

        // 使用一个新的 pictureId 避免 Redis 缓存干扰
        Flux<String> stringFlux = aiChatFacade.moodSearchImageStream(userPrompt, 40L, null);

        System.out.println("AI 正在响应中...");

        // 这种方式在测试中更稳定，能看到实时输出
        stringFlux.doOnNext(chunk -> {
            System.out.print(chunk); // 实时打印
            System.out.flush();
        }).blockLast(); // 等待流彻底结束
    }*/

}