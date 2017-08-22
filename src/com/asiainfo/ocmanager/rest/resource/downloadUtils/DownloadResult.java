package com.asiainfo.ocmanager.rest.resource.downloadUtils;

/**
 * Created by zhangfq on 2017/8/17.
 */
public class DownloadResult {
    private String result;
    private String msg;
    private String Code;

    public DownloadResult(){}
    public DownloadResult(String result,String msg,String errorCode){
        this.result=result;
        this.msg=msg;
        this.Code=errorCode;
    }

    public void setResult(String result){this.result = result;}
    public String getResult(){return this.result;}

    public void setMessage(String msg){this.msg = msg;}
    public String getMessage(){return this.msg;}

    public void setCode(String Code){this.Code = Code;}
    public String getCode(){return this.Code;}

}
