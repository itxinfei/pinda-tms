package com.xinfei.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自定义异常
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class XfException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
    private String msg;
    private int code = 500;
    
    public XfException(String msg) {
		super(msg);
		this.msg = msg;
	}
	
	public XfException(String msg, Throwable e) {
		super(msg, e);
		this.msg = msg;
	}
	
	public XfException(String msg, int code) {
		super(msg);
		this.msg = msg;
		this.code = code;
	}
	
	public XfException(String msg, int code, Throwable e) {
		super(msg, e);
		this.msg = msg;
		this.code = code;
	}

	public static void main(String[] args) {
    	try{
			int i = 1/0;
		}catch (Exception e){
    		throw new XfException("除法中分母不能为空");
		}

	}
}
