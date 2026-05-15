package com.king.pojo;

public class Result {
    Integer code;
    String message;
    Object data;

    public Result(Integer code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    public static Result success(Object data){
        return new Result(200, "success", data);
    }

    public static Result success(String message) {
        return new Result(200, message, null);
    }

    public static Result success() {
        return new Result(200, "success", null);
    }

    public static Result error(String message) {
        return new Result(500, message, null);
    }

    public static Result error(Integer code, String message) {
        return new Result(code, message, null);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
