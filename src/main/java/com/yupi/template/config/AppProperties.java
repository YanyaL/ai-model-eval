package com.yupi.template.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用级开关：Mock AI、本地存储等（先不接真实大模型 Key）
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Ai ai = new Ai();
    private Storage storage = new Storage();

@Data
public static class Ai {
    /**
     * 仅作文档占位；实际判定见 AiKeyUtils / AiModeService
     */
    private Boolean mockEnabled;
}

    @Data
    public static class Storage {
        /**
         * true：文件落到本地磁盘，不依赖腾讯云 COS
         */
        private boolean localEnabled = true;
        private String localDir = "./data/uploads";
        private String publicBaseUrl = "/api/files";
    }
}
