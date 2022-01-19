package com.wslhome.demo.component;

import cn.hutool.core.io.FileUtil;

import com.wslhome.demo.config.FilePath;
import com.wslhome.demo.util.m3u8Util;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @Author sirwsl
 * @Version 1.0
 */
@Slf4j
@Component
public class M3u8Component {

    @Resource
    private FFmpeg ffmpeg;

    @Resource
    private FFprobe ffprobe;

    @Resource
    private FilePath filePath;



    /**
     * 视频文件转 m3u8
     * 支持： .mp4 | .flv | .avi | .mov | .wmv | .wav
     * @param file 视频文件
     * @return 路径
     */
    public String mediaFileToM3u8(MultipartFile file){
        if (file.isEmpty()) {
            throw new RuntimeException("未发现文件");
        }
        log.info("开始解析视频");
        long start = System.currentTimeMillis();
        //临时目录创建
        String path = new File(System.getProperty("user.dir")).getAbsolutePath();
        String tempFilePath = path+ filePath.getTempPath();
        if (!FileUtil.exist(tempFilePath)) {
            FileUtil.mkdir(tempFilePath);
        }
        String filePathName = tempFilePath + file.getOriginalFilename();
        File dest = new File(filePathName);
        try {
            file.transferTo(dest);
        }catch (Exception e){
            log.error("视频转m3u8格式存在异常，异常原因e:{}",e.getMessage());
        }
        long end = System.currentTimeMillis();
        log.info("临时文件上传成功......耗时：{} ms", end - start);
        String m3u8FilePath = localFileToM3u8(filePathName);
        log.info("视频转换已完成 ！");
        return m3u8FilePath;
    }

    /**
     * 本地媒体资源转换
     * @param filePathName : 文件路径
     * @return :
     */
    @SneakyThrows
    public String localFileToM3u8(String filePathName) {
        long startTime = System.currentTimeMillis();
        final FFmpegProbeResult probe = ffprobe.probe(filePathName);
        final List<FFmpegStream> streams = probe.getStreams().stream().filter(fFmpegStream -> fFmpegStream.codec_type != null).collect(Collectors.toList());
        final Optional<FFmpegStream> audioStream = streams.stream().filter(fFmpegStream -> FFmpegStream.CodecType.AUDIO.equals(fFmpegStream.codec_type)).findFirst();
        final Optional<FFmpegStream> videoStream = streams.stream().filter(fFmpegStream -> FFmpegStream.CodecType.VIDEO.equals(fFmpegStream.codec_type)).findFirst();

        if (!audioStream.isPresent()) {
            log.error("未发现音频流");
        }
        if (!videoStream.isPresent()) {
            log.error("未发现视频流");
        }
        //m3u8文件 存储路径
        String filePath = m3u8Util.generateFilePath(this.filePath.getBasePath());
        if (!FileUtil.exist(filePath)) {
            FileUtil.mkdir(filePath);
        }
        String mainName = m3u8Util.getFileMainName(filePathName);
        String m3u8FileName = filePath + mainName + ".m3u8";

        //下面这一串参数别乱动，经过调优的，1G视频大概需要10秒左右，如果是大佬随意改
        //"-vsync", "2", "-c:v", "copy", "-c:a", "copy", "-tune", "fastdecode", "-hls_wrap", "0", "-hls_time", "10", "-hls_list_size", "0", "-threads", "12"
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(filePathName)
                .overrideOutputFiles(true)
                .addOutput(m3u8FileName)//输出文件
                .setFormat(probe.getFormat().format_name) //"mp4"
                .setAudioBitRate(audioStream.map(fFmpegStream -> fFmpegStream.bit_rate).orElse(0L))
                .setAudioChannels(1)
                .setAudioCodec("aac")        // using the aac codec
                .setAudioSampleRate(audioStream.get().sample_rate)
                .setAudioBitRate(audioStream.get().bit_rate)
                .setStrict(FFmpegBuilder.Strict.STRICT)
                .setFormat("hls")
                .setPreset("ultrafast")
                .addExtraArgs("-vsync", "2", "-c:v", "copy", "-c:a", "copy", "-tune", "fastdecode", "-hls_wrap", "0", "-hls_time", "10", "-hls_list_size", "0", "-threads", "12")
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        // Run a one-pass encode
        executor.createJob(builder).run();

        File dest = new File(filePathName);
        if (dest.isFile() && dest.exists()) {
            dest.delete();
            System.gc();
            log.warn("临时文件 {}已删除", dest.getName());
        }
        long endTime = System.currentTimeMillis();
        log.info("文件：{} 转换完成！共耗时{} ms", dest.getName(), (endTime - startTime));
        return m3u8FileName;
    }

}