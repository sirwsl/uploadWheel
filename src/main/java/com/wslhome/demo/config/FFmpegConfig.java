package com.wslhome.demo.config;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ffmpeg 工具路径设置
 * @Author sirwsl
 * @Version 1.0
 */
@Configuration
@Slf4j
public class FFmpegConfig {

    @SneakyThrows
    @Bean
    public FFmpeg fFmpeg() {
        String path = System.getProperty("user.dir");
        if (isLinux()){
            path+="ffmpeg/ffmpeg";
        }else if (isWindows()){
            path+="/ffmpeg-win/ffmpeg/bin/ffmpeg.exe";
        }
        log.info("ffmpeg.exe 路径为{}",path);
        return new FFmpeg(path);
    }

    @SneakyThrows
    @Bean
    public FFprobe fFprobe() {
        String path = System.getProperty("user.dir");
        if (isLinux()){
            path+="ffmpeg/ffprobe";
        }else if (isWindows()){
            path+="/ffmpeg-win/ffmpeg/bin/ffprobe.exe";
        }
        log.info("ffprobe.exe 路径为{}",path);
        return new FFprobe(path);
    }
    public static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }


}