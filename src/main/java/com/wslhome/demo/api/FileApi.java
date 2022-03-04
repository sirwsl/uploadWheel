package com.wslhome.demo.api;


import com.wslhome.demo.obj.Result;
import com.wslhome.demo.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * @Author sirwsl
 * @Version 1.0
 */
@RestController
@RequestMapping("/")
@Slf4j
public class FileApi {


    @Resource
    private FileService fileService;



    /**
     * 测试文件上传
     * @param file ： 上传文件
     * @return ： 路径
     */
    @PostMapping("/uploadFile")
    public Result uploadImg(@RequestParam("file") MultipartFile file) {
        String path = fileService.uploadFile(file);
        if (StringUtils.isNotBlank(path)) {
            return Result.success("上传成功",path);
        }
        return Result.error("上传失败");
    }


    /**
     * 整个文件上传
    * 上传视频文件转m3u8
    *@param file 文件
    *@return  String: 路径
    */
    @PostMapping("/uploadVideo")
    public Result uploadVideo(@RequestPart("file") MultipartFile file) { ;
        try {
            String path = fileService.uploadVideo2M3u8(file);
            if (StringUtils.isNotBlank(path)) {
                return Result.success("上传成功",path);
            }
        }catch (Exception e){
            log.error("视频上传转码异常,异常原因e:{}",e.getMessage());
        }
        return Result.error("上传失败");
    }


    /**
     * 大文件上传至本地
     * @param request ： 请求
     * @param guid ： 编码
     * @param chunk ： 切片数
     * @param file ： 文件
     * @return ： 返回结果
     */
    @PostMapping("/uploadSlice")
    public Result uploadSlice(HttpServletRequest request, @RequestParam("guid") String guid,
                              @RequestParam("chunk") Integer chunk,
                              @RequestParam("file") MultipartFile file) {
        if (fileService.uploadSlice(request, guid, chunk, file)){
            return Result.success("上传成功","");
        }else{
            return Result.error();
        }
    }

    /**
     * 大文件上传后合并
     * @param guid：
     * @param fileName ：
     * @return ：
     */
    @RequestMapping("/uploadMerge")
    public Result uploadMerge(@RequestParam("guid") String guid, @RequestParam("fileName") String fileName) {
        // 得到 destTempFile 就是最终的文件
            String path = fileService.uploadMerge(guid, fileName);
            if (StringUtils.isNotBlank(path)){
                return Result.success("合并成功",path);
            }else{
                return Result.error("合并文件失败");
            }


    }

    /**
     * 切片上传后合并转M3U8格式 ：
     * @param fileName ：文件名
     * @param guid: 随机id
     * @return ：
     */
    @PostMapping("/uploadVideoMerge")
    public Result uploadVideoMerge(@RequestParam("guid") String guid, @RequestParam("fileName") String fileName) {
        try {
            String path = fileService.uploadVideoMerge(guid, fileName);
            if (StringUtils.isNotBlank(path)) {
                return Result.success("上传成功",path);
            }
        }catch (Exception e){
            log.error("视频上传转码异常,异常原因e:{}",e.getMessage());
        }
        return Result.error("上传失败");
    }

    /**
     * 下载文件
     *
     * @param response response
     * @param url   bucket名称
     * @param fileName 文件名
     */
    @GetMapping("/downloadFile")
    public void downloadFile(HttpServletResponse response, String url, String fileName) throws Exception {
        fileService.downloadFileOss(response,url,fileName);
    }
}

