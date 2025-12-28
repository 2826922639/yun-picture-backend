package com.hhh.yunpicturebackend.ai.core;

import cn.hutool.json.JSONUtil;
import com.hhh.yunpicturebackend.ai.AiChatWithImageService;
import com.hhh.yunpicturebackend.ai.AiChatWithImageServiceFactory;
import com.hhh.yunpicturebackend.ai.AiCodeGeneratorService;
import com.hhh.yunpicturebackend.ai.AiCodeGeneratorServiceFactory;
import com.hhh.yunpicturebackend.ai.core.builder.VueProjectBuilder;
import com.hhh.yunpicturebackend.ai.core.parser.CodeParserExecutor;
import com.hhh.yunpicturebackend.ai.core.saver.CodeFileSaverExecutor;
import com.hhh.yunpicturebackend.ai.enums.CodeGenTypeEnum;
import com.hhh.yunpicturebackend.ai.model.HtmlCodeResult;
import com.hhh.yunpicturebackend.ai.model.MultiFileCodeResult;
import com.hhh.yunpicturebackend.ai.model.message.AiResponseMessage;
import com.hhh.yunpicturebackend.ai.model.message.ToolExecutedMessage;
import com.hhh.yunpicturebackend.ai.model.message.ToolRequestMessage;
import com.hhh.yunpicturebackend.constant.AppConstant;
import com.hhh.yunpicturebackend.exception.BusinessException;
import com.hhh.yunpicturebackend.exception.ErrorCode;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.BeforeToolExecution;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.api.ErrorMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;
    @Resource
    private VueProjectBuilder vueProjectBuilder;


    /**
     * 通用流式代码处理方法
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream
                // 过滤掉null值以防止NPE
                .filter(chunk -> chunk != null)
                .doOnNext(chunk -> {
                    // 实时收集代码片段
                    if (chunk != null) {
                        codeBuilder.append(chunk);
                    }
                }).doOnComplete(() -> {
                    // 流式返回完成后保存代码
                    try {
                        String completeCode = codeBuilder.toString();
                        // 使用执行器解析代码
                        Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                        // 使用执行器保存代码
                        File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType,appId);
                        log.info("保存成功，路径为：" + savedDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("保存失败: {}", e.getMessage());
                    }
                });
    }
    /**
     * 通用流式响应处理方法
     *
     * @param codeStream  代码流
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream) {
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream
                // 过滤掉null值以防止NPE
                .filter(chunk -> chunk != null)
                .doOnNext(chunk -> {
                    // 实时收集代码片段
                    if (chunk != null) {
                        codeBuilder.append(chunk);
                    }
                });
    }
    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        // 根据 appId 获取对应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML,appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE,appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, List<String> imageUrls , CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        // 根据 appId 获取对应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage,imageUrls);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML,appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage,imageUrls);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE,appId);
            }
            case VUE_PROJECT -> {
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage,imageUrls);
                yield processTokenStream(tokenStream,appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }
    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
/*    private Flux<String> processTokenStream(TokenStream tokenStream) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    .start();
        });
    }*/

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream,Long appId) {
        return Flux.create(sink -> {
            tokenStream
                    // 1. 处理流式文本响应（过滤空/纯空格文本块，避免报错）
                    .onPartialResponse((String partialResponse) -> {
                        // 过滤空、纯空格、全空白字符的文本块
                        if (partialResponse == null || partialResponse.trim().isEmpty()) {
                            return; // 跳过无效文本，避免后续序列化/业务逻辑报错
                        }
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    // 2. 适配1.9.1：替换onPartialToolExecutionRequest为beforeToolExecution
                    .beforeToolExecution((BeforeToolExecution beforeToolExecution) -> {
                        // 直接获取单个ToolExecutionRequest（因为request()返回的是单个对象）
                        ToolExecutionRequest toolRequest = beforeToolExecution.request();
                        // 空值校验
                        if (toolRequest == null) {
                            return;
                        }
                        // 封装并推送消息
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    // 3. 工具执行完成回调（逻辑不变，增加空值校验）
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        if (toolExecution == null) {
                            return;
                        }
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    // 4. 响应完成回调
                    .onCompleteResponse((ChatResponse response) -> {
                        // 执行 Vue 项目构建（同步执行，确保预览时项目已就绪）
                        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "vue_project_" + appId;
                        vueProjectBuilder.buildProject(projectPath,appId);
                        sink.complete();
                    })

                    // 5. 异常处理（增强：记录日志+标准化错误响应）
                    .onError((Throwable error) -> {
                        // 建议替换为日志框架（如logback/log4j）
                        error.printStackTrace();
                        // 推送标准化错误消息（可选，前端可识别）
                        ErrorMessage errorMessage = new ErrorMessage("流式处理异常：" + error.getMessage());
                        sink.next(JSONUtil.toJsonStr(errorMessage));
                        sink.error(error); // 终止流式
                    })
                    // 6. 启动流式处理（1.9.1必须调用start）
                    .start();
        });
    }
}
