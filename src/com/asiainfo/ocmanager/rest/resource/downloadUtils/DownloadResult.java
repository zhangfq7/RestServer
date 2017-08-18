package com.asiainfo.ocmanager.rest.resource.downloadUtils;

/**
 * Created by zhangfq on 2017/8/17.
 */
public class DownloadResult {
    private String result;
    private String msg;
    private String errorCode;
    public DownloadResult(String result,String msg,String errorCode){
        this.result=result;
        this.msg=msg;
        this.errorCode=errorCode;
    }

    public void setResult(String result){this.result = result;}
    public String getResult(){return this.result;}

    public void setMessage(String msg){this.msg = msg;}
    public String getMessage(){return this.msg;}

    public void setErrorCode(String errorCode){this.errorCode = result;}
    public String getErrorCode(){return this.errorCode;}

}
