package com.wslhome.demo.api;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.ResultSet;

@Data
@Accessors(chain = true)
public class Result {
    public String code ;

    public String msg;

    public Object data;

    public static Result success(String msg,String data){
        return new Result().setCode("200").setData(data).setMsg(msg);
    }

    public static Result error(String msg){
        return new Result().setCode("500").setMsg(msg);
    }

    public static Result error(){
        return new Result().setCode("500").setMsg("服务器出错");
    }
}
