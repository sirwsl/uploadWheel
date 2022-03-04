package com.wslhome.demo.api;

import com.alibaba.fastjson.JSONObject;
import com.wslhome.demo.config.FilePath;
import com.wslhome.demo.obj.FileDetail;
import com.wslhome.demo.obj.Result;
import com.wslhome.demo.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.annotation.Resource;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 大文件切片上传改用websocket
 */
@ServerEndpoint(value = "/file/{guid}")
@Controller
@Slf4j
public class FileSocket {
    //统计
    private static AtomicInteger count = new AtomicInteger(0);

    //链接信息
    private static Map<String, FileDetail> fileInfo = new ConcurrentHashMap<>();
    //切片计数
    private static Map<String,AtomicInteger> chunk = new ConcurrentHashMap<>();

    private static FilePath filePath;

    private static FileService fileService;

    @Autowired
    private void setFilePath(FilePath filePath){
        this.filePath = filePath;
    }

    @Autowired
    private void setFileService(FileService fileService){
        this.fileService = fileService;
    }

    String projectUrl = System.getProperty("user.dir").replaceAll("\\\\", "/");



    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session) {
        count.incrementAndGet();
        log.info("{} 开始建立大文件上传通道。",session.getId());
        log.info("当前链接数：{}",count.get());
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(Session session,@PathParam("guid") String guid) {
        count.decrementAndGet();
        fileInfo.remove(guid);
        chunk.remove(guid);
        log.info("{} 文件上传通道已关闭",session.getId());
    }

    /**
     * 收到客户端消息后调用的方法
     */
    @OnMessage
    public void onMessage(String message, @PathParam("guid") String guid, Session session){
        if(StringUtils.isBlank(message)){
            return;
        }
        FileDetail fileDetail = JSONObject.parseObject(message, FileDetail.class);
        fileInfo.put(guid,fileDetail);
        // 临时目录用来存放所有分片文件
        String tempFileDir = projectUrl + filePath.getBigPath() + guid;
        File parentFileDir = new File(tempFileDir);
        if (!parentFileDir.exists()) {
            parentFileDir.mkdirs();
        }
        chunk.put(guid,new AtomicInteger(0));
        sendMessage(session,Result.fileBuild());


    }
    @OnMessage
    public void onMessage(byte[] message, @PathParam("guid") String guid, Session session){
        int number = chunk.get(guid).incrementAndGet();
        log.info("当前切片序号：{}",number);
        String tempFileDir = projectUrl + filePath.getBigPath() + guid;
        File tempPartFile = new File(new File(tempFileDir), guid + "_" + (number-1) + ".part");
        try {
            FileUtils.copyInputStreamToFile(new ByteArrayInputStream(message), tempPartFile);
            FileDetail fileDetail = fileInfo.get(guid);
            if (String.valueOf(number).equals(fileDetail.getChunks().toString())){
                //合并文件
                sendMessage(session,Result.fileOver());
                String path;
                if ("video".equals(fileDetail.getType())){
                    path = fileService.uploadVideoMerge(guid, fileDetail.getName());
                }else{
                    path = fileService.uploadMerge(guid, fileDetail.getName());
                }
                sendMessage(session,Result.fileSuccess(path));
                onClose(session,guid);
            }else{
                sendMessage(session,Result.success(String.valueOf((int)((float) number/fileInfo.get(guid).getChunks()*100))));
            }

        }catch (Exception e){
            log.error("文件切片存储失败！当前序号：{},异常原因：{}",number,e.getMessage());
            sendMessage(session,Result.error("文件上传失败"));
            onError(session,guid,e);
        }
    }
    /**
     * 发生错误时调用
     */
    @OnError
    public void onError(Session session,@PathParam("guid") String guid, Throwable error) {
        fileInfo.remove(guid);
        chunk.remove(guid);
        log.error("socket异常：{}",error.getMessage());
    }

    /**
     * 发送消息
     * @param session:
     * @param message:
     */
    public static void sendMessage(Session session, Result message) {
        try {
            session.getBasicRemote().sendText(JSONObject.toJSONString(message));
        } catch (IOException e) {
            log.error("发送消息出错：{}", e.getMessage());
            e.printStackTrace();
        }
    }
}
