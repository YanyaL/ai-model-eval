package com.yupi.template.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.common.ImageProcessRequest;
import com.qcloud.cos.model.ciModel.persistence.CIUploadResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.yupi.template.config.AppProperties;
import com.yupi.template.config.TencentCosConfig;
import com.yupi.template.exception.BusinessException;
import com.yupi.template.exception.ErrorCode;
import com.yupi.template.model.dto.file.WaterMarkParam;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * 文件存储：优先腾讯云 COS；未配置时写入本地目录（配合 /api/files 访问）
 */
@Slf4j
@Component
public class TencentCosUtil {

    @Resource
    private TencentCosConfig tencentCosConfig;

    @Resource
    private ObjectProvider<COSClient> cosClientProvider;

    @Resource
    private AppProperties appProperties;

    final long ONE_K = 1024L;

    private final static HashSet<String> SUPPORT_THUMBNAIL_EXT = new HashSet<String>() {{
        add("jpg");
        add("jpeg");
        add("png");
        add("bmp");
        add("webp");
        add("tiff");
        add("gif");
    }};

    public boolean useLocalStorage() {
        return appProperties.getStorage().isLocalEnabled()
                || cosClientProvider.getIfAvailable() == null
                || !tencentCosConfig.isConfigured();
    }

    private COSClient requireCosClient() {
        COSClient client = cosClientProvider.getIfAvailable();
        if (client == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "未配置腾讯云 COS");
        }
        return client;
    }

    private void saveLocal(String key, File file) {
        try {
            String normalized = StrUtil.removePrefix(key, "/");
            Path target = Paths.get(appProperties.getStorage().getLocalDir(), normalized)
                    .toAbsolutePath()
                    .normalize();
            Files.createDirectories(target.getParent());
            Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("本地存储上传成功: {}", target);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "本地文件保存失败: " + e.getMessage());
        }
    }

    public PutObjectResult putObject(String key, String localFilePath) {
        File file = new File(localFilePath);
        if (useLocalStorage()) {
            saveLocal(key, file);
            return null;
        }
        PutObjectRequest putObjectRequest = new PutObjectRequest(tencentCosConfig.getBucket(), key, file);
        return requireCosClient().putObject(putObjectRequest);
    }

    public void deleteObject(String key) {
        if (useLocalStorage()) {
            Path target = Paths.get(appProperties.getStorage().getLocalDir(), StrUtil.removePrefix(key, "/"))
                    .toAbsolutePath().normalize();
            FileUtil.del(target.toFile());
            return;
        }
        requireCosClient().deleteObject(tencentCosConfig.getBucket(), key);
    }

    public PutObjectResult putObject(String key, File file) {
        if (useLocalStorage()) {
            saveLocal(key, file);
            return null;
        }
        PutObjectRequest putObjectRequest = new PutObjectRequest(tencentCosConfig.getBucket(), key, file);
        return requireCosClient().putObject(putObjectRequest);
    }

    private @NotNull PicOperations getThumbnailPicOperations(String thumbnailKey, int width, int height) {
        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(1);

        List<PicOperations.Rule> ruleList = new LinkedList<>();
        PicOperations.Rule rule1 = new PicOperations.Rule();
        rule1.setBucket(tencentCosConfig.getBucket());
        rule1.setFileId(thumbnailKey);
        rule1.setRule(String.format("imageMogr2/thumbnail/%sx%s!", width, height));
        ruleList.add(rule1);
        picOperations.setRules(ruleList);
        return picOperations;
    }

    public String putObjectWithWaterMarkOnProcessImage(String key, WaterMarkParam waterMarkParam) {
        if (StrUtil.isBlank(key)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件的 key 不能为空");
        }
        if (useLocalStorage()) {
            log.warn("本地存储模式不支持云端水印处理，原样返回 key={}", key);
            return key;
        }
        key = key.replace("https://pic.code-nav.cn", "");
        ImageProcessRequest imageReq = new ImageProcessRequest(tencentCosConfig.getBucket(), key);

        String ruleStr = "watermark/2/text/{encodedText}/font/{encodedFont}/fontsize/{fontSize}/fill/{encodedColor}/dissolve/{dissolve}/gravity/{gravity}/dx/{dx}/dy/{dy}/shadow/{shadow}";
        String formatRule = ruleStr.replace("{encodedText}", waterMarkParam.getEncodedText())
                .replace("{encodedFont}", waterMarkParam.getEncodedFont())
                .replace("{fontSize}", waterMarkParam.getFontSize())
                .replace("{encodedColor}", waterMarkParam.getEncodedColor())
                .replace("{dissolve}", waterMarkParam.getDissolve())
                .replace("{gravity}", waterMarkParam.getGravity())
                .replace("{dx}", waterMarkParam.getDx())
                .replace("{dy}", waterMarkParam.getDy())
                .replace("{shadow}", waterMarkParam.getShadow());

        String extName = FileUtil.extName(key);
        key = key.replace("." + extName, "_mianshiya." + extName);
        key = replace(key);
        List<PicOperations.Rule> ruleList = new LinkedList<>();
        PicOperations.Rule rule = new PicOperations.Rule();
        rule.setBucket(tencentCosConfig.getBucket());
        rule.setFileId(key);
        rule.setRule(formatRule);
        ruleList.add(rule);
        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(0);
        picOperations.setRules(ruleList);
        imageReq.setPicOperations(picOperations);
        CIUploadResult result = requireCosClient().processImage(imageReq);
        log.info("result {}", JSONUtil.toJsonStr(result));

        return key;
    }

    public String uploadImage2Cos(String key, File file) {
        WaterMarkParam waterMarkParam = new WaterMarkParam(0.3d);
        BufferedImage bufferedImage = ImgUtil.read(file);
        if (bufferedImage.getHeight() < 200 || bufferedImage.getWidth() < 200) {
            log.info("图片宽高[{},{}]，不加水印", bufferedImage.getWidth(), bufferedImage.getHeight());
            putObject(key, file);
            return key;
        } else {
            log.info("图片宽高[{},{}]，加水印", bufferedImage.getWidth(), bufferedImage.getHeight());
            return putObjectWithWaterMark(key, file, waterMarkParam);
        }
    }

    public String putObjectWithWaterMark(String key, File file, WaterMarkParam waterMarkParam) {
        if (StrUtil.isBlank(key)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件的 key 不能为空");
        }
        if (useLocalStorage()) {
            saveLocal(key, file);
            return key;
        }
        key = StrUtil.prependIfMissing(key, "/");
        PutObjectRequest putObjectRequest = new PutObjectRequest(tencentCosConfig.getBucket(), key, file);

        String extName = FileUtil.extName(key);
        key = key.replace("." + extName, "_mianshiya." + extName);
        List<PicOperations.Rule> ruleList = new LinkedList<>();
        PicOperations.Rule rule = new PicOperations.Rule();
        rule.setBucket(tencentCosConfig.getBucket());
        rule.setFileId(key);
        rule.setRule(getWaterMarkRuleStr(waterMarkParam));
        ruleList.add(rule);
        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(0);
        picOperations.setRules(ruleList);
        putObjectRequest.setPicOperations(picOperations);
        requireCosClient().putObject(putObjectRequest);

        return key;
    }

    private static String replace(String url) {
        String ch = "[\u2E80-\u2EFF\u2F00-\u2FDF\u31C0-\u31EF\u3400-\u4DBF\u4E00-\u9FFF\uF900-\uFAFF\uD840\uDC00-\uD869\uDEDF\uD869\uDF00-\uD86D\uDF3F\uD86D\uDF40-\uD86E\uDC1F\uD86E\uDC20-\uD873\uDEAF\uD87E\uDC00-\uD87E\uDE1F]+";
        String uuid = RandomStringUtils.randomAlphanumeric(8);
        List<String> list = ReUtil.findAll(ch, url, 0);
        if (CollUtil.isNotEmpty(list)) {
            for (String c : list) {
                url = url.replace(c, uuid);
            }
        }
        return url;
    }

    public String putObject(String key, File file, boolean compress, boolean withWaterMark) {
        if (useLocalStorage()) {
            saveLocal(key, file);
            return key;
        }
        PutObjectRequest putObjectRequest = new PutObjectRequest(tencentCosConfig.getBucket(), key, file);
        String ext = CharSequenceUtil.subAfter(key, ".", true);
        String ruleStr = "";

        if (withWaterMark) {
            double rate = .3d;
            if (file != null && file.length() < 100 * ONE_K) {
                rate = .15d;
            }
            WaterMarkParam waterMarkParam = new WaterMarkParam(rate);
            ruleStr += this.getWaterMarkRuleStr(waterMarkParam);
            String extName = FileUtil.extName(key);
            key = key.replace("." + extName, "_mianshiya." + extName);
        }

        if (compress && SUPPORT_THUMBNAIL_EXT.contains(ext)) {
            ruleStr += "|" + "imageMogr2/format/webp";
        }
        if (StringUtils.isNotBlank(ruleStr)) {
            PicOperations picOperations = new PicOperations();
            List<PicOperations.Rule> ruleList = new LinkedList<>();
            PicOperations.Rule rule = new PicOperations.Rule();
            rule.setBucket(tencentCosConfig.getBucket());
            rule.setFileId(key);
            rule.setRule(ruleStr);
            ruleList.add(rule);
            picOperations.setRules(ruleList);
            putObjectRequest.setPicOperations(picOperations);
        }
        requireCosClient().putObject(putObjectRequest);
        return key;
    }

    /**
     * 本地：/api/files/...；COS：host + key
     */
    public String toPublicUrl(String key) {
        if (key == null) {
            return null;
        }
        if (useLocalStorage()) {
            String base = appProperties.getStorage().getPublicBaseUrl();
            if (base == null || base.isBlank()) {
                base = "/api/files";
            }
            if (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            String path = key.startsWith("/") ? key : "/" + key;
            return base + path;
        }
        String host = tencentCosConfig.getHost();
        return (host == null ? "" : host) + key;
    }

    private String getWaterMarkRuleStr(WaterMarkParam waterMarkParam) {
        String ruleStr = "watermark/2/text/{encodedText}/font/{encodedFont}/fontsize/{fontSize}/fill/{encodedColor}/dissolve/{dissolve}/gravity/{gravity}/dx/{dx}/dy/{dy}/shadow/{shadow}";
        return ruleStr.replace("{encodedText}", waterMarkParam.getEncodedText())
                .replace("{encodedFont}", waterMarkParam.getEncodedFont())
                .replace("{fontSize}", waterMarkParam.getFontSize())
                .replace("{encodedColor}", waterMarkParam.getEncodedColor())
                .replace("{dissolve}", waterMarkParam.getDissolve())
                .replace("{gravity}", waterMarkParam.getGravity())
                .replace("{dx}", waterMarkParam.getDx())
                .replace("{dy}", waterMarkParam.getDy())
                .replace("{shadow}", waterMarkParam.getShadow());
    }
}
