package com.wslhome.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ali.oss")
@Data
public class AliOssProperties {


    /**
     * OSS配置信息
     */

    private String endpoint;

    private String accessKeyId;

    private String accessKeySecret;

    private String bucketName;

    private String myHostUrl;

    private String url;

    private String aliUrl;

}
