package com.yupi.template.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * 仅当配置了真实腾讯云 COS 密钥时创建 COSClient
 */
public class CosConfiguredCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String accessKey = context.getEnvironment().getProperty("tencent.cos.access-key", "");
        String secretKey = context.getEnvironment().getProperty("tencent.cos.secret-key", "");
        return isReal(accessKey) && isReal(secretKey);
    }

    private boolean isReal(String value) {
        return StringUtils.hasText(value) && !"placeholder".equalsIgnoreCase(value.trim());
    }
}
