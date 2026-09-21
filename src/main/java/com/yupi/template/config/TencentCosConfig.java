package com.yupi.template.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConfigurationProperties(prefix = "tencent.cos")
@Data
public class TencentCosConfig {

    private String accessKey;
    private String secretKey;
    private String region;
    private String bucket;
    private String host;

    public boolean isConfigured() {
        return StringUtils.hasText(accessKey)
                && !"placeholder".equalsIgnoreCase(accessKey.trim())
                && StringUtils.hasText(secretKey)
                && !"placeholder".equalsIgnoreCase(secretKey.trim());
    }

    @Bean
    @Conditional(CosConfiguredCondition.class)
    public COSClient cosClient() {
        COSCredentials cred = new BasicCOSCredentials(accessKey, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        return new COSClient(cred, clientConfig);
    }
}
