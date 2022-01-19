package com.wslhome.demo.util;

import java.io.*;

public class FileUtil {

    public static void deleteFiles(String path) {
        File file = new File(path);
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] temp = file.listFiles(); //获取该文件夹下的所有文件
                for (File value : temp) {
                    deleteFile(value.getAbsolutePath());
                }
            } else {
                file.delete(); //删除子文件
            }
            file.delete(); //删除文件夹
        }
    }

    public static void deleteFile(String path){
        File dest = new File(path);
        if (dest.isFile() && dest.exists()) {
            dest.delete();
        }
    }

    public static void replaceTextContent(String path,String srcStr,String replaceStr) throws IOException {
        // 读
        File file = new File(path);
        FileReader in = new FileReader(file);
        BufferedReader bufIn = new BufferedReader(in);
        // 内存流, 作为临时流
        CharArrayWriter tempStream = new CharArrayWriter();
        // 替换
        String line = null;
        while ( (line = bufIn.readLine()) != null) {
            // 替换每行中, 符合条件的字符串
            line = line.replaceAll(srcStr, replaceStr);
            // 将该行写入内存
            tempStream.write(line);
            // 添加换行符
            tempStream.append(System.getProperty("line.separator"));
        }
        // 关闭 输入流
        bufIn.close();
        // 将内存中的流 写入 文件
        FileWriter out = new FileWriter(file);
        tempStream.writeTo(out);
        out.close();
        System.out.println("====path:"+path);

    }
}
