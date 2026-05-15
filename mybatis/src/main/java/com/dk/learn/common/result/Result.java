package com.dk.learn.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

	/** 0 表示成功，非 0 为业务或错误码 */
	private int code;
	private String message;
	private T data;
	private Long timestamp;

	public static <T> Result<T> ok(T data) {
		Result<T> r = new Result<>();
		r.setCode(0);
		r.setMessage("success");
		r.setData(data);
		r.setTimestamp(System.currentTimeMillis());
		return r;
	}

	public static <T> Result<T> fail(int code, String message) {
		Result<T> r = new Result<>();
		r.setCode(code);
		r.setMessage(message);
		r.setData(null);
		r.setTimestamp(System.currentTimeMillis());
		return r;
	}
	
	public static <T> Result<T> error(String message) {
        return fail(500, message);
	}
}
