package com.wslhome.demo.component;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.*;
import com.wslhome.demo.config.AliOssProperties;;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 阿里云 OSS 工具类
 *
 * @author caibenhao
 */
@Component
@Slf4j
@Getter
public class OssComponent{

    @Resource
    private AliOssProperties aliOssProperties;


    /* -----------------对外功能---------------- */

    /**
     * 本地文件切片上传
     *
     * @param objectName：文件名
     * @param path           : 本地完整路径，xxx/xxx.txt
     * @return ：异常
     */
    public String uploadSlice(String objectName, String localPath,String path) throws IOException {
        OSS ossClient = new OSSClientBuilder().build(aliOssProperties.getEndpoint(), aliOssProperties.getAccessKeyId(), aliOssProperties.getAccessKeySecret());
        String keyPath = path+objectName;
        // 创建InitiateMultipartUploadRequest对象。
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(aliOssProperties.getBucketName(), keyPath);
        // 如果需要在初始化分片时设置请求头，请参考以下示例代码。
        ObjectMetadata metadata = new ObjectMetadata();
        // 指定该Object的网页缓存行为。
        metadata.setCacheControl("no-cache");
        // 指定该Object被下载时的名称。
        metadata.setContentDisposition("attachment;filename=" + objectName);
        // 指定初始化分片上传时是否覆盖同名Object。此处设置为true，表示禁止覆盖同名Object。
        metadata.setHeader("x-oss-forbid-overwrite", "true");

        // 初始化分片。
        InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(request);
        // 返回uploadId，它是分片上传事件的唯一标识。您可以根据该uploadId发起相关的操作，例如取消分片上传、查询分片上传等。
        String uploadId = result.getUploadId();

        List<PartETag> partETags = new ArrayList<>();
        // 每个分片的大小，用于计算文件有多少个分片。单位为字节。
        final long partSize = 5 * 1024 * 1024L;   //1 MB。

        // 填写本地文件的完整路径。如果未指定本地路径，则默认从示例程序所属项目对应本地路径中上传文件。
        final File sampleFile = new File(localPath);
        long fileLength = sampleFile.length();
        int partCount = (int) (fileLength / partSize);
        if (fileLength % partSize != 0) {
            partCount++;
        }

        // 遍历分片上传。
        for (int i = 0; i < partCount; i++) {
            long startPos = i * partSize;
            long curPartSize = (i + 1 == partCount) ? (fileLength - startPos) : partSize;

            try (InputStream inStream = new FileInputStream(sampleFile)) {
                // 跳过已经上传的分片。
                long skip = inStream.skip(startPos);
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(aliOssProperties.getBucketName());
                uploadPartRequest.setKey(keyPath);
                uploadPartRequest.setUploadId(uploadId);
                uploadPartRequest.setInputStream(inStream);
                // 设置分片大小。除了最后一个分片没有大小限制，其他的分片最小为100 KB。
                uploadPartRequest.setPartSize(curPartSize);
                // 设置分片号。每一个上传的分片都有一个分片号，取值范围是1~10000，如果超出此范围，OSS将返回InvalidArgument错误码。
                uploadPartRequest.setPartNumber(i + 1);
                // 每个分片不需要按顺序上传，甚至可以在不同客户端上传，OSS会按照分片号排序组成完整的文件。
                UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                // 每次上传分片之后，OSS的返回结果包含PartETag。PartETag将被保存在partETags中。
                partETags.add(uploadPartResult.getPartETag());
            }catch (Exception e){
                log.error("OSS切片上传异常,e:{}",e.getMessage());
            }

        }

        // 创建CompleteMultipartUploadRequest对象。
        // 在执行完成分片上传操作时，需要提供所有有效的partETags。OSS收到提交的partETags后，会逐一验证每个分片的有效性。当所有的数据分片验证通过后，OSS将把这些分片组合成一个完整的文件。
        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(aliOssProperties.getBucketName(),keyPath , uploadId, partETags);

        // 完成分片上传。
        CompleteMultipartUploadResult completeMultipartUploadResult = ossClient.completeMultipartUpload(completeMultipartUploadRequest);
        log.info(completeMultipartUploadResult.getETag());
        // 关闭OSSClient。
        ossClient.shutdown();
        return path+objectName;
    }

    /**
     * 单个文件上传
     *
     * @param file 文件
     * @return 返回完整URL地址
     */
    public String uploadFile(String fileDir, MultipartFile file) {
        String fileUrl = upload2Oss(fileDir, file);
        String str = getFileUrl(fileDir, fileUrl);
        return str.trim();
    }

    /**
     * 单个文件上传(指定文件名（带后缀）)
     *
     * @param inputStream 文件
     * @param fileName    文件名(带后缀)
     * @return 返回完整URL地址
     */
    public String uploadFile(String fileDir, InputStream inputStream, String fileName) {
        try {
            this.uploadFile2Oss(fileDir, inputStream, fileName);
            String url = getFileUrl(fileDir, fileName);
            if (url != null && url.length() > 0) {
                return url;
            }
        } catch (Exception e) {
            throw new RuntimeException("获取路径失败");
        }
        return "";
    }

    /**
     * 多文件上传
     *
     * @param fileList 文件列表
     * @return 返回完整URL，逗号分隔
     */
    public String uploadFile(String fileDir, List<MultipartFile> fileList) {
        String fileUrl;
        String str;
        StringBuilder photoUrl = new StringBuilder();
        for (int i = 0; i < fileList.size(); i++) {
            fileUrl = upload2Oss(fileDir, fileList.get(i));
            str = getFileUrl(fileDir, fileUrl);
            if (i == 0) {
                photoUrl = new StringBuilder(str);
            } else {
                photoUrl.append(",").append(str);
            }
        }
        return photoUrl.toString().trim();
    }

    public boolean deleteFile(String fileDir, String fileName) {
        OSS ossClient = new OSSClientBuilder().build(aliOssProperties.getEndpoint(), aliOssProperties.getAccessKeyId(), aliOssProperties.getAccessKeySecret());
        // 删除文件
        ossClient.deleteObject(aliOssProperties.getBucketName(), fileDir + fileName);
        // 判断文件是否存在
        boolean found = ossClient.doesObjectExist(aliOssProperties.getBucketName(), fileDir + fileName);
        // 如果文件存在则删除失败

        return !found;
    }

    /**
     * 通过文件名获取文完整件路径
     *
     * @param fileUrl 文件名
     * @return 完整URL路径
     */
    public String getFileUrl(String fileDir, String fileUrl) {
        if (fileUrl != null && fileUrl.length() > 0) {
            String[] split = fileUrl.replaceAll("\\\\","/").split("/");
            String url = aliOssProperties.getMyHostUrl() + fileDir + split[split.length - 1];
            return Objects.requireNonNull(url);
        }
        return null;
    }

    public File getFile(String url) {
        //对本地文件命名
        String fileName = url.substring(url.lastIndexOf("."));
        File file = null;
        try {
            file = File.createTempFile("net_url", fileName);
        } catch (Exception e) {
            log.error("创建默认文件夹net_url失败！原因e:{}", e.getMessage());
        }
        if (file != null) {
            try (InputStream inStream = new URL(url).openStream();
                 OutputStream os = new FileOutputStream(file)) {
                int bytesRead;
                byte[] buffer = new byte[8192];
                while ((bytesRead = inStream.read(buffer, 0, 8192)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    /* -----------内部辅助功能------------------------ */

    /**
     * 获取去掉参数的完整路径
     *
     * @param url URL
     * @return 去掉参数的URL
     */
    private String getShortUrl(String url) {
        String[] imgUrls = url.split("\\?");
        return imgUrls[0].trim();
    }

    /**
     * 获得url真实外网链接
     * 不提供使用，因为会产生公网OOS流量下行费用
     *
     * @param key 文件名
     * @return URL
     */
    @Deprecated
    private String getUrl(String key) {
        OSS ossClient = new OSSClientBuilder().build(aliOssProperties.getEndpoint(), aliOssProperties.getAccessKeyId(), aliOssProperties.getAccessKeySecret());
        // 设置URL过期时间为20年  3600l* 1000*24*365*20
        Date expiration = new Date(System.currentTimeMillis() + 3600L * 1000 * 24 * 365 * 20);
        URL url = ossClient.generatePresignedUrl(aliOssProperties.getBucketName(), key, expiration);
        if (url != null) {
            String replaceUrl = url.toString()
                    .replace(aliOssProperties.getAliUrl(), aliOssProperties.getUrl());
            return getShortUrl(replaceUrl);
        }
        ossClient.shutdown();
        return null;
    }

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 文件名
     */
    private String upload2Oss(String fileDir, MultipartFile file) {
        // 2、重命名文件
        String fileName = Objects.requireNonNull(file.getOriginalFilename(), "文件名不能为空");
        // 文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf(".")).toLowerCase(Locale.ENGLISH);
        String uuid = UUID.randomUUID().toString();
        String name = uuid + suffix;
        try {
            InputStream inputStream = file.getInputStream();
            this.uploadFile2Oss(fileDir, inputStream, name);
            return name;
        } catch (Exception e) {
            throw new RuntimeException("上传失败");
        }
    }

    /**
     * 上传文件（指定文件名）
     *
     * @param inputStream 输入流
     * @param fileName    文件名
     */
    private void uploadFile2Oss(String fileDir, InputStream inputStream, String fileName) {
        OSS ossClient = new OSSClientBuilder().build(aliOssProperties.getEndpoint(), aliOssProperties.getAccessKeyId(), aliOssProperties.getAccessKeySecret());
        String ret;
        try {
            //创建上传Object的Metadata
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(inputStream.available());
            objectMetadata.setCacheControl("no-cache");
            objectMetadata.setHeader("Pragma", "no-cache");
            objectMetadata.setContentType(getContentType(fileName.substring(fileName.lastIndexOf("."))));
            objectMetadata.setContentDisposition("inline;filename=" + fileName);
            //上传文件
            PutObjectResult putResult = ossClient.putObject(aliOssProperties.getBucketName(), fileDir + fileName, inputStream, objectMetadata);
            ret = putResult.getETag();
            if (StringUtils.isEmpty(ret)) {
                log.error("上传失败，文件ETag为空");
            }
            ossClient.shutdown();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 请求类型
     *
     * @param filenameExtension ：
     * @return ：
     */
    private static String getContentType(String filenameExtension) {
        if (FileNameSuffixEnum.BMP.getSuffix().equalsIgnoreCase(filenameExtension)) {
            return "image/bmp";
        }
        if (FileNameSuffixEnum.GIF.getSuffix().equalsIgnoreCase(filenameExtension)) {
            return "image/gif";
        }
        if (FileNameSuffixEnum.JPEG.getSuffix().equalsIgnoreCase(filenameExtension) ||
                FileNameSuffixEnum.JPG.getSuffix().equalsIgnoreCase(filenameExtension) ||
                FileNameSuffixEnum.PNG.getSuffix().equalsIgnoreCase(filenameExtension)) {
            return "image/jpeg";
        }
        if (FileNameSuffixEnum.HTML.getSuffix().equalsIgnoreCase(filenameExtension)) {
            return "text/html";
        }
        if (FileNameSuffixEnum.TXT.getSuffix().equalsIgnoreCase(filenameExtension)) {
            return "text/plain";
        }
        if (FileNameSuffixEnum.VSD.getSuffix().equalsIgnoreCase(filenameExtension)) {
            return "application/vnd.visio";
        }
        if (FileNameSuffixEnum.PPTX.getSuffix().equalsIgnoreCase(filenameExtension) ||
                FileNameSuffixEnum.PPT.getSuffix().equalsIgnoreCase(filenameExtension)) {
            return "application/vnd.ms-powerpoint";
        }
        if (FileNameSuffixEnum.DOCX.getSuffix().equalsIgnoreCase(filenameExtension) ||
                FileNameSuffixEnum.DOC.getSuffix().equalsIgnoreCase(filenameExtension)) {
            return "application/msword";
        }
        if (FileNameSuffixEnum.XML.getSuffix().equalsIgnoreCase(filenameExtension)) {
            return "text/xml";
        }
        if (FileNameSuffixEnum.PDF.getSuffix().equalsIgnoreCase(filenameExtension)) {
            return "application/pdf";
        }
        return "image/jpeg";
    }



}

@Getter
enum FileNameSuffixEnum {

    /**
     * 文件后缀名
     */
    BMP(".bmp", "bmp文件"),
    GIF(".gif", "gif文件"),
    JPEG(".jpeg", "jpeg文件"),
    JPG(".jpg", "jpg文件"),
    PNG(".png", "png文件"),
    HTML(".html", "HTML文件"),
    TXT(".txt", "txt文件"),
    VSD(".vsd", "vsd文件"),
    PPTX(".pptx", "PPTX文件"),
    DOCX(".docx", "DOCX文件"),
    PPT(".ppt", "PPT文件"),
    DOC(".doc", "DOC文件"),
    XML(".xml", "XML文件"),
    PDF(".pdf", "PDF文件");

    /**
     * 后缀名
     */
    private final String suffix;

    /**
     * 描述
     */
    private final String description;

    FileNameSuffixEnum(String suffix, String description) {
        this.suffix = suffix;
        this.description = description;
    }
}





