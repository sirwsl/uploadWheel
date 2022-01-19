package com.wslhome.demo.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;

import java.io.*;
import java.time.LocalDateTime;

/**
 * @Description 工具类
 * @Author sirwsl
 * @Version 1.0
 */
public class m3u8Util {

    /**
    *@Description 根据基础路径，生成文件存储路径
    *@param basePath 基础路径（根路径）
    *@Return 
    */
    public static String generateFilePath(String basePath){
        String temp = basePath;
        if(StrUtil.isNotBlank(basePath)){
            if(basePath.endsWith("/")){
                temp = basePath.substring(0,basePath.lastIndexOf("/"));
            }
        }
        return temp+"/"+generateDateDir()+"/";
    }

    /**
     *@Description 根据当前时间，生成下级存储目录
     *@Return
     */
    public static String generateDateDir(){
        LocalDateTime now = LocalDateTime.now();
        return DateUtil.format(now, "yyyyMMdd/HH/mm/ss");
    }

    /**
     *@Description 根据文件全路径，获取文件主名称
     *@param fullPath 文件全路径（包含文件名）
     *@Return
     */
    public static String getFileMainName(String fullPath){
        String fileName = FileUtil.getName(fullPath);
        return fileName.substring(0,fileName.lastIndexOf("."));
    }


}