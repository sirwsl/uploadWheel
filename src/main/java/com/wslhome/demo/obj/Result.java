package com.wslhome.demo.obj;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Result {
    public String code ;

    public String msg;

    public Object data;

    public static Result success(String msg,String data){
        return new Result().setCode("200").setData(data).setMsg(msg);
    }
    public static Result success(String msg){
        return new Result().setCode("200").setMsg(msg);
    }

    public static Result fileBuild(){
        return new Result().setCode("101");
    }

    public static Result fileSuccess(String data){
        return new Result().setCode("201").setData(data);
    }

    public static Result error(String msg){
        return new Result().setCode("500").setMsg(msg);
    }

    public static Result error(){
        return new Result().setCode("500").setMsg("服务器出错");
    }

    public static Result fileOver() {
        return new Result().setCode("202");
    }
}
