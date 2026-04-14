package com.hhh.yunpicturebackend.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.hhh.yunpicturebackend.exception.BusinessException;
import com.hhh.yunpicturebackend.exception.ErrorCode;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 截图工具类
 */
/*@Slf4j
public class WebScreenshotUtils {

    public static String saveWebPageScreenshot(String webUrl) {
        if (StrUtil.isBlank(webUrl)) {
            log.error("网页截图失败，url为空");
            return null;
        }

        WebDriver driver = null;
        try {
            int width = 1600;
            int height = 900;

            // 1. 初始化Chrome（完全复刻你成功的命令行参数）
            driver = initChromeDriver(width, height, webUrl);

            // 2. 访问页面
            driver.get(webUrl);
            driver.manage().window().setSize(new Dimension(width, height));

            // 3. 固定等待5秒（和命令行一样，简单粗暴但有效）
            log.info("页面加载中，等待5秒...");
            Thread.sleep(5000);

            // 4. 创建临时目录
            String rootPath = System.getProperty("user.dir") + "/tmp/screenshots/" + UUID.randomUUID().toString().substring(0, 8);
            FileUtil.mkdir(rootPath);

            // 5. 原生稳定截图（不用CDP，避免版本兼容问题）
            byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);

            // 6. 保存+压缩图片
            String originPath = rootPath + File.separator + RandomUtil.randomNumbers(5) + ".png";
            saveImage(screenshotBytes, originPath);
            String compressPath = rootPath + File.separator + RandomUtil.randomNumbers(5) + "_compressed.jpg";
            compressImage(originPath, compressPath);
            FileUtil.del(originPath);

            log.info("✅ 网页截图成功：{}", webUrl);
            return compressPath;

        } catch (Exception e) {
            log.error("❌ 网页截图失败：{}", webUrl, e);
            return null;
        } finally {
            // 用完必须关闭
            if (driver != null) {
                driver.quit();
            }
        }
    }

    *//**
     * 初始化Chrome（完全复刻你成功的命令行参数）
     *//*
    private static WebDriver initChromeDriver(int width, int height, String pageUrl) {
        try {
            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();

            // ================== 核心：完全复刻你命令行的参数 ==================
            options.addArguments("--headless=new");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-gpu");
            options.addArguments("--disable-web-security"); // 关键：关闭安全策略
            options.addArguments("--allow-running-insecure-content");
            options.addArguments("--referer=" + pageUrl); // 关键：设置Referer
            options.addArguments(String.format("--window-size=%d,%d", width, height));
            options.setBinary("/usr/bin/google-chrome");

            // 基础配置
            options.addArguments("--disable-extensions");
            options.addArguments("--remote-allow-origins=*");
            options.addArguments("--disable-dev-shm-usage");

            // 浏览器偏好
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("profile.default_content_setting_values.notifications", 2);
            prefs.put("profile.default_content_setting_values.images", 1); // 强制允许图片
            options.setExperimentalOption("prefs", prefs);

            ChromeDriver driver = new ChromeDriver(options);
            log.info("✅ Chrome初始化成功，参数已完全复刻命令行");
            return driver;

        } catch (Exception e) {
            log.error("❌ 初始化Chrome失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化Chrome失败");
        }
    }

    private static void saveImage(byte[] bytes, String path) {
        try {
            FileUtil.writeBytes(bytes, path);
        } catch (Exception e) {
            log.error("保存图片失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图片失败");
        }
    }

    private static void compressImage(String origin, String target) {
        try {
            ImgUtil.compress(FileUtil.file(origin), FileUtil.file(target), 0.3f);
        } catch (Exception e) {
            log.error("图片压缩失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片压缩失败");
        }
    }
}*/

@Slf4j
public class WebScreenshotUtils {

    private static final WebDriver webDriver;

    // 全局静态初始化，避免重复初始化驱动程序：
    static {
        final int DEFAULT_WIDTH = 1600;
        final int DEFAULT_HEIGHT = 900;
        webDriver = initChromeDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

/**
     * 退出时销毁
     */

    @PreDestroy
    public void destroy() {
        webDriver.quit();
    }

/**
     * 生成网页截图
     *
     * @param webUrl 要截图的网址
     * @return 压缩后的截图文件路径，失败返回 null
     */

    public static String saveWebPageScreenshot(String webUrl) {
        // 非空校验
        if (StrUtil.isBlank(webUrl)) {
            log.error("网页截图失败，url为空");
            return null;
        }
        // 创建临时目录
        try {
            String rootPath = System.getProperty("user.dir") + "/tmp/screenshots/" + UUID.randomUUID().toString().substring(0, 8);
            FileUtil.mkdir(rootPath);
            // 图片后缀
            final String IMAGE_SUFFIX = ".png";
            // 原始图片保存路径
            String imageSavePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + IMAGE_SUFFIX;
            // 访问网页
            webDriver.get(webUrl);
            // 等待网页加载
            waitForPageLoad(webDriver);
            // 截图
            byte[] screenshotBytes = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
            // 保存原始图片
            saveImage(screenshotBytes, imageSavePath);
            log.info("原始截图保存成功：{}", imageSavePath);
            // 压缩图片
            final String COMPRESS_SUFFIX = "_compressed.jpg";
            String compressedImagePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + COMPRESS_SUFFIX;
            compressImage(imageSavePath, compressedImagePath);
            log.info("压缩图片保存成功：{}", compressedImagePath);
            // 删除原始图片
            FileUtil.del(imageSavePath);
            return compressedImagePath;
        } catch (Exception e) {
            log.error("网页截图失败：{}", webUrl, e);
            return null;
        }
    }

/**
     * 初始化 Chrome 浏览器驱动
     */

    private static WebDriver initChromeDriver(int width, int height) {
        try {
            // 自动管理 ChromeDriver
            System.setProperty("wdm.chromeDriverMirrorUrl", "https://registry.npmmirror.com/binary.html?path=chromedriver");
            WebDriverManager.chromedriver().useMirror().setup();
            // 配置 Chrome 选项
            ChromeOptions options = new ChromeOptions();
            // 无头模式
            options.addArguments("--headless");
            // 禁用GPU（在某些环境下避免问题）
            options.addArguments("--disable-gpu");
            // 禁用沙盒模式（Docker环境需要）
            options.addArguments("--no-sandbox");
            // 禁用开发者shm使用
            options.addArguments("--disable-dev-shm-usage");
            // 设置窗口大小
            options.addArguments(String.format("--window-size=%d,%d", width, height));
            // 禁用扩展
            options.addArguments("--disable-extensions");
            // 设置用户代理
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            // 创建驱动
            WebDriver driver = new ChromeDriver(options);
            // 设置页面加载超时
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            // 设置隐式等待
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            return driver;
        } catch (Exception e) {
            log.error("初始化 Chrome 浏览器失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化 Chrome 浏览器失败");
        }
    }

/**
     * 保存图片到文件
     *
     * @param imageBytes
     * @param imagePath
     */

    private static void saveImage(byte[] imageBytes, String imagePath) {
        try {
            FileUtil.writeBytes(imageBytes, imagePath);
        } catch (Exception e) {
            log.error("保存图片失败：{}", imagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图片失败");
        }
    }

/**
     * 压缩图片
     *
     * @param originImagePath
     * @param compressedImagePath
     */

    private static void compressImage(String originImagePath, String compressedImagePath) {
        // 压缩图片质量（0.1 = 10% 质量）
        final float COMPRESSION_QUALITY = 0.3f;
        try {
            ImgUtil.compress(
                    FileUtil.file(originImagePath),
                    FileUtil.file(compressedImagePath),
                    COMPRESSION_QUALITY
            );
        } catch (Exception e) {
            log.error("压缩图片失败：{} -> {}", originImagePath, compressedImagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "压缩图片失败");
        }
    }

/**
     * 等待页面加载完成
     *
     * @param webDriver
     */

    private static void waitForPageLoad(WebDriver webDriver) {
        try {
            // 创建等待页面加载对象
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
            // 等待 document.readyState 为 complete
            wait.until(driver -> ((JavascriptExecutor) driver)
                    .executeScript("return document.readyState").
                    equals("complete")
            );
            // 额外等待一段时间，确保动态内容加载完成
            Thread.sleep(2000);
            log.info("页面加载完成");
        } catch (Exception e) {
            log.error("等待页面加载时出现异常，继续执行截图", e);
        }
    }
}
