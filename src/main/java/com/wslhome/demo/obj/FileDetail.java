package com.wslhome.demo.obj;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class FileDetail {
    //编号
    private Integer code;

    /**
     * 类型
     */
    private String type;
    /**
     * 切片名
     */
    private String name;

    /**
     * 随机码
     */
    private String guid;

    /**
     * 总片数
     */
    private Integer chunks;

}
