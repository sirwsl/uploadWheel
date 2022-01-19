package com.wslhome.demo.component;

import com.wslhome.demo.config.MinioProperties;
import io.minio.MinioClient;
import io.minio.ObjectStat;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.concurrent.*;

@Slf4j
@Component
public class MinioComponent {

    private static final ExecutorService executor = new ThreadPoolExecutor(10, 15, 0,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

    @Resource
    private MinioProperties minioProperties;

    private static MinioClient minioClient;


    public MinioComponent(MinioProperties properties) {
        try {
            minioClient = new MinioClient(properties.getUrl(), properties.getAccess(), properties.getSecret());
            reconnect(properties);
        }catch (Exception e){
            minioClient=null;
            reconnect(properties);
        }
    }

    public static void stop() {
        if (minioClient != null) minioClient = null;
    }

    private static void reconnect(MinioProperties properties) {
        executor.execute(() -> {
            try {
                if (minioClient == null) {
                    minioClient =  new MinioClient(properties.getUrl(), properties.getAccess(), properties.getSecret());
                }

                minioClient.bucketExists("system");
                Thread.sleep(10 * 1000L);
                reconnect(properties);
            } catch (Exception e) {
                log.error("Interrupted!", e);
                Thread.currentThread().interrupt();
            }
            log.info("minIO自动重连");
        });
    }

    /**
     * 文件上传 上传完成后请关闭文件流
     *
     * @param bucket   文件分类 建议文件后缀
     * @param fileName 带后缀的文件名 检验日期yyyyMMdd加16位随机码
     * @param stream   文件流 要上传文件的流
     */
    public void fileUploader(String bucket, String fileName, InputStream stream) throws Exception {
        // 检查存储桶是否已经存在
        boolean isExist = minioClient.bucketExists(bucket.toLowerCase());
        if (!isExist) {
            minioClient.makeBucket(bucket.toLowerCase());
        }
        // 使用putObject上传一个文件到文件分类
        minioClient.putObject(bucket.toLowerCase(), fileName, stream, null, null, null, "application/octet-stream");
    }

    /**
     * 检查存储桶是否已经存在不存在就创建
     *
     * @param bucket ； 名称
     * @throws Exception : 异常
     */
    public void mkBucket(String bucket) throws Exception {
        // 检查存储桶是否已经存在
        boolean isExist = minioClient.bucketExists(bucket.toLowerCase());
        if (!isExist) {
            minioClient.makeBucket(bucket.toLowerCase());
        }
    }

    /**
     * 判断存储桶是否存在
     *
     * @param bucket :
     * @return :
     */
    @SneakyThrows
    public boolean bucketExists(String bucket) {
        return minioClient.bucketExists(bucket);
    }


    /**
     * 使用putObject上传一个文件到文件分类
     *
     * @param bucket   ： bucket名称
     * @param fileName ： 文件名
     * @param stream   ： 文件流
     * @throws Exception ： 异常
     */
    public void FileUploaderExist(String bucket, String fileName, InputStream stream) throws Exception {
        minioClient.putObject(bucket.toLowerCase(), fileName, stream, null, null, null, "application/octet-stream");
    }

    /**
     * 文件下载
     *
     * @param bucket   文件分类 建议文件后缀
     * @param fileName 带后缀的文件名 检验日期yyyyMMdd加16位随机码
     */
    public byte[] fileDownloader(String bucket, String fileName) {
        InputStream stream = null;
        try {
            minioClient.statObject(bucket.toLowerCase(), fileName);
            stream = minioClient.getObject(bucket.toLowerCase(), fileName);
            return read(stream);
        } catch (Exception e) {
            return new byte[0];
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 得到文件路径
     *
     * @param bucket   文件分类 建议文件后缀
     * @param fileName 带后缀的文件名 检验日期yyyyMMdd加16位随机码
     */
    public String fileName(String bucket, String fileName) {
        try {
            String minIoUrl = "";
            String url = minioProperties.getUrl();
            if (url.endsWith("/")) {
                minIoUrl = url.substring(0, url.length() - 1);
            }
            minioClient.statObject(bucket.toLowerCase(), fileName);
            return minioClient.presignedGetObject(bucket.toLowerCase(), fileName, 24 * 60 * 60).replace(minIoUrl, "/minio");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 删除文件
     *
     * @param bucket   文件分类 建议文件后缀
     * @param fileName 带后缀的文件名 检验日期yyyyMMdd加16位随机码
     */
    public Boolean removeFile(String bucket, String fileName) {
        try {
            minioClient.removeObject(bucket.toLowerCase(), fileName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static byte[] read(InputStream inputStream) {

        try (ByteArrayOutputStream bas = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int num = inputStream.read(buffer);
            while (num != -1) {
                bas.write(buffer, 0, num);
                num = inputStream.read(buffer);
            }
            bas.flush();
            return bas.toByteArray();
        } catch (Exception e) {
            log.error("读取文件异常e:{}", e.getMessage());
        } finally {
            try {
                inputStream.close();
            } catch (Exception er) {
                log.error("关闭文件流失败，{}", er.getMessage());
            }
        }
        return new byte[0];
    }

    @SneakyThrows
    public ObjectStat statObject(String bucketName, String objectName) {
        boolean flag = minioClient.bucketExists(bucketName);
        if (flag) {
            return minioClient.statObject(bucketName, objectName);
        }
        return null;
    }

}

