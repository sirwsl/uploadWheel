package com.wslhome.demo.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author wsl
 * @Version 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "m3u8.convertor")
public class FilePath {
    /**
    * 文件上传临时路径 (本地文件转换不需要)
    */
    private String tempPath = "/file/tmp/";

    /**
     * m3u8文件转换后，储存的根路径
     */
    private String basePath = "/file/m3u8/";

    /**
     * m3u8文件转换后，储存的根路径
     */
    private String bigPath = "/file/big/";

    private String proxy = "m3u8/";
}