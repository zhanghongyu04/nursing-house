package com.zhiling.common.exception;

import com.zhiling.common.enums.IBasicEnum;

 /**
 * 自定义异常
 *
 * @author zhanghongyu
 */
public class ProjectException extends RuntimeException {

    //错误编码
    private int code;

    //提示信息
    private String message;

    //异常接口
    private IBasicEnum basicEnumIntface;

    /**
     * 构造器：ProjectException
     *
     * @author zhanghongyu
     */
    public ProjectException() {

    }
    /**
     * 构造器：ProjectException
     *
     * @author zhanghongyu
     */
    public ProjectException(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 构造器：ProjectException
     *
     * @author zhanghongyu
     */
    public ProjectException(IBasicEnum errorCode) {
        setBasicMsg(errorCode);
    }


    /**
     * 构造器：ProjectException
     *
     * @author zhanghongyu
     */
    public ProjectException(IBasicEnum errorCode, String throwMsg) {
        super(throwMsg);
        setBasicMsg(errorCode);
    }

    /**
     * 构造器：ProjectException
     *
     * @author zhanghongyu
     */
    public ProjectException(IBasicEnum errorCode, Throwable throwable) {
        super(throwable);
        setBasicMsg(errorCode);
    }


    /**
     * 方法：setBasicMsg
     *
     * @author zhanghongyu
     */
    private void setBasicMsg(IBasicEnum basicEnumIntface) {
        this.code = basicEnumIntface.getCode();
        this.message = basicEnumIntface.getMsg();
        this.basicEnumIntface = basicEnumIntface;
    }

    /**
     * 方法：getCode
     *
     * @author zhanghongyu
     */
    public int getCode() {
        return code;
    }

    /**
     * 方法：setCode
     *
     * @author zhanghongyu
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * 方法：getMessage
     *
     * @author zhanghongyu
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 方法：setMessage
     *
     * @author zhanghongyu
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 方法：getBasicEnumIntface
     *
     * @author zhanghongyu
     */
    public IBasicEnum getBasicEnumIntface() {
        return basicEnumIntface;
    }

    /**
     * 方法：setBasicEnumIntface
     *
     * @author zhanghongyu
     */
    public void setBasicEnumIntface(IBasicEnum basicEnumIntface) {
        this.basicEnumIntface = basicEnumIntface;
    }

    /**
     * 方法：toString
     *
     * @author zhanghongyu
     */
    @Override
    public String toString() {
        return "ProjectException{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", basicEnumIntface=" + basicEnumIntface +
                '}';
    }
}