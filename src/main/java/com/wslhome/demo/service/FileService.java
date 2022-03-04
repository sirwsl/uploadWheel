package com.wslhome.demo.service;

import cn.hutool.core.date.DateUtil;
import com.wslhome.demo.component.M3u8Component;
import com.wslhome.demo.component.MinioComponent;
import com.wslhome.demo.component.OssComponent;
import com.wslhome.demo.config.AliOssProperties;
import com.wslhome.demo.config.FilePath;
import com.wslhome.demo.util.FileUtil;
import io.minio.ObjectStat;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * 文件上传
 */
@Component
@Slf4j
public class FileService {

    @Resource
    private M3u8Component m3U8ComponentTemplate;

    @Resource
    private FilePath filePath;

    //@Resource(name = "minIOUploadTreadPool")
    @Resource(name = "ossUploadTreadPool")
    private ThreadPoolTaskExecutor poolTaskExecutor;

    @Resource
    private MinioComponent minioComponent;

    @Resource
    private AliOssProperties aliOssProperties;

    @Resource
    private OssComponent ossComponent;

    String projectUrl = System.getProperty("user.dir").replaceAll("\\\\", "/");


    /**
     * 视频上传并转m3u8，转存至oss或minIO
     *
     * @param file : 视频文件
     * @return 保存路径
     */
    public String uploadVideo2M3u8(MultipartFile file) throws Exception {
        String path = m3U8ComponentTemplate.mediaFileToM3u8(file);
        return upload2M3u8(path);
    }

    /**
     * 本地视频转m3u8后上传至OSS或minIO
     *
     * @param path
     * @return
     * @throws Exception
     */
    public String localVideo2M3u8(String path) throws Exception {
        String paths = m3U8ComponentTemplate.localFileToM3u8(path);
        return upload2M3u8(paths);
    }

    /**
     * 上传转码后得视频至OSS或minIOn
     * @param path
     * @return 路径
     * @throws Exception
     */
    public String upload2M3u8(String path) throws Exception {
        //存储转码后文件
        String realPath = path.substring(0, path.lastIndexOf("/"));
        log.info("视频解析后的 realPath {}", realPath);
        String name = path.substring(path.lastIndexOf("/") + 1);
        log.info("解析后视频 name {}", name);
        File allFile = new File(realPath);
        File[] files = allFile.listFiles();
        if (null == files || files.length == 0) {
            return null;
        }
        String patch = DateUtil.format(LocalDateTime.now(), "yyyy/MM/") + name.substring(0, name.lastIndexOf(".")) + "/";
        List<File> errorFile = new ArrayList<>();

        long start = System.currentTimeMillis();
        //替换m3u8文件中的路径
        FileUtil.replaceTextContent(path, name.substring(0, name.lastIndexOf(".")),
                aliOssProperties.getMyHostUrl() + filePath.getProxy() + patch +
                        name.substring(0, name.lastIndexOf(".")));
        //开始上传
        CountDownLatch countDownLatch = new CountDownLatch(files.length);
        Arrays.stream(files).forEach(li -> poolTaskExecutor.execute(() -> {
            try (FileInputStream fileInputStream = new FileInputStream(li)) {
                //minioComponent.FileUploaderExist("m3u8", patch + li.getName(), fileInputStream);
                ossComponent.uploadFile(filePath.getProxy() + patch, fileInputStream, li.getName());
                log.info("文件：{} 正在上传", li.getName());
            } catch (Exception e) {
                errorFile.add(li);
                e.printStackTrace();
            } finally {
                countDownLatch.countDown();
            }
        }));
        countDownLatch.await();
        long end = System.currentTimeMillis();
        log.info("解析文件上传成功,共计：{} 个文件,失败：{},共耗时： {}ms", files.length, errorFile.size(), end - start);
        //  try {
        //      minioComponent.mkBucket("m3u8");
        //  } catch (Exception e) {
        //      log.error("创建Bucket失败！");
        //  }

        //异步移除所有文件
        poolTaskExecutor.execute(() -> {
                FileUtil.deleteFile(projectUrl+filePath.getTempPath());
        });
        if (CollectionUtils.isEmpty(errorFile)) {
            return aliOssProperties.getMyHostUrl() + filePath.getProxy() + patch + name;
        }
        return "";
    }
    /**
     * 普通上传文件转存至Oss或MinIo
     *
     * @param file ： 文件
     * @return ： 文件路径
     */
    public String uploadFile(MultipartFile file) {
        //文件名字
        String fileName = file.getOriginalFilename();
        if (StringUtils.isBlank(fileName)) {
            return null;
        }
        String patch = "test/" +DateUtil.format(LocalDateTime.now(), "yyyy/MM/dd/");
        try {
            //String contentType = fileName.substring(fileName.lastIndexOf(".") + 1);
            //minioComponent.fileUploader(contentType, patch + fileName, file.getInputStream());
            ossComponent.uploadFile( patch, file.getInputStream(), fileName);
        } catch (Exception e) {
            log.error("文件上传失败");
            return "文件上传失败！";
        } finally {
            try {
                file.getInputStream().close();
            } catch (Exception e) {
                log.error("关闭文件流异常，异常原因e:{}", e.getMessage());
            }
        }
        return patch + fileName;
    }


    /**
     * 大文件上传至本地
     *
     * @param request ：请求
     * @param guid    ： 编码文件名
     * @param chunk   ： 切片数
     * @param file    ： 切片文件
     * @return ： 是否成功
     */
    public boolean uploadSlice(HttpServletRequest request, String guid, Integer chunk, MultipartFile file) {
        try {


            boolean isMultipart = ServletFileUpload.isMultipartContent(request);
            if (isMultipart) {
                if (chunk == null) chunk = 0;
                // 临时目录用来存放所有分片文件
                String tempFileDir = projectUrl + filePath.getBigPath() + guid;
                File parentFileDir = new File(tempFileDir);
                if (!parentFileDir.exists()) {
                    parentFileDir.mkdirs();
                }
                // 分片处理时，前台会多次调用上传接口，每次都会上传文件的一部分到后台
                File tempPartFile = new File(parentFileDir, guid + "_" + chunk + ".part");
                FileUtils.copyInputStreamToFile(file.getInputStream(), tempPartFile);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 合并切片并上传至服务器
     * @param guid :
     * @param fileName :
     * @return :
     */
    public String uploadMerge(String guid, String fileName){
        String localPath = mergeFile(guid, fileName);
        //此处需要注意，OSS需要再次切片上传，但minIO是不用得，它默认5M超过就会自动切片
        String path = "";
       if (StringUtils.isNotBlank(localPath)){
           try {
               log.info("正在上传至OSS");
               path = ossComponent.uploadSlice(fileName, localPath,"file/bigfile/");
           }catch (Exception e){
               log.error("OSS切片上传失败！");
           }

       }
        //移除文件
        poolTaskExecutor.execute(() -> {
            FileUtil.deleteFiles(projectUrl+filePath.getBigPath());
        });
        return path;
    }

    /**
     * 合并切片文件至本地
     *
     * @param guid     ： 编码
     * @param fileName ： 文件名
     * @return ： 是否成功
     */
    public String mergeFile(String guid, String fileName) {
        try {
            String sName = fileName.substring(fileName.lastIndexOf("."));
            //时间格式化格式
            Date currentTime = new Date();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            //获取当前时间并作为时间戳
            String timeStamp = simpleDateFormat.format(currentTime);
            //拼接新的文件名
            String newName = timeStamp + sName;
            simpleDateFormat = new SimpleDateFormat("yyyyMM");
            String tempPath = projectUrl + filePath.getBigPath()+guid;
            String margePath = projectUrl + filePath.getBigPath()+simpleDateFormat.format(currentTime);
            File parentFileDir = new File(tempPath);
            if (parentFileDir.isDirectory()) {
                File destTempFile = new File(margePath, newName);
                if (!destTempFile.exists()) {
                    //先得到文件的上级目录，并创建上级目录，在创建文件
                    destTempFile.getParentFile().mkdir();
                    destTempFile.createNewFile();
                }
                for (int i = 0; i < Objects.requireNonNull(parentFileDir.listFiles()).length; i++) {
                    File partFile = new File(parentFileDir, guid + "_" + i + ".part");
                    FileOutputStream destTempfos = new FileOutputStream(destTempFile, true);
                    //遍历"所有分片文件"到"最终文件"中
                    FileUtils.copyFile(partFile, destTempfos);
                    destTempfos.close();
                }
                // 删除临时目录中的分片文件
                FileUtils.deleteDirectory(parentFileDir);
                return destTempFile.getAbsolutePath();
            }
        } catch (Exception e) {
            log.error("切片文件合并，失败原因e：{}", e.getMessage());
        }
        return null;

    }

    /**
     * minIO文件下载
     *
     * @param response ： 相应
     * @param bucket   ： bucket名称
     * @param fileName ： 文件名
     * @throws Exception ： 异常
     */
    public void downloadFileMinIO(HttpServletResponse response, String bucket, String fileName) throws Exception {
        ObjectStat objectStat = minioComponent.statObject(bucket, fileName);
        response.setContentType(objectStat.contentType());
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
        response.addHeader("Content-Length", String.valueOf(objectStat.length()));
        byte[] bytes = minioComponent.fileDownloader(bucket, fileName);
        OutputStream outputStream = response.getOutputStream();
        outputStream.write(bytes);
        outputStream.close();
    }


    /**
     * Oss文件下载
     *
     * @param response：相应
     * @param url         ： 路径
     * @param fileName    ： 文件名
     * @throws Exception ： 异常
     */
    public void downloadFileOss(HttpServletResponse response, String url, String fileName) throws Exception {
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
        File file = ossComponent.getFile(url);
        OutputStream outputStream = response.getOutputStream();
        outputStream.write(new BufferedInputStream(new FileInputStream(file)).read());
        outputStream.close();
    }


    public String uploadVideoMerge(String guid, String fileName) {
        String localPath = mergeFile(guid, fileName);
        //此处需要注意，OSS需要再次切片上传，但minIO是不用得，它默认5M超过就会自动切片
        String path = "";
        try {
            path = localVideo2M3u8(localPath);
        }catch (Exception e){
            log.error("OSS切片上传失败！");
        }
        //移除文件
        poolTaskExecutor.execute(() -> {
            String[] split = localPath.replaceAll("\\\\","/").split("/");
            if (split.length >= 1) {
                FileUtil.deleteFile(split[1]);
            } else {
                FileUtil.deleteFile(split[0]);
            }
        });
        return path;

    }
}
