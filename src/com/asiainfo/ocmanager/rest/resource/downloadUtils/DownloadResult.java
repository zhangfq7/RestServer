package com.asiainfo.ocmanager.rest.resource.downloadUtils;

/**
 * Created by zhangfq on 2017/8/17.
 */
public class DownloadResult {
    private String result;
    private String msg;
    private String code;

    public DownloadResult(){}
    public DownloadResult(String result,String msg,String code){
        this.result=result;
        this.msg=msg;
        this.code=code;
    }

    public void setResult(String result){this.result = result;}
    public String getResult(){return this.result;}

    public void setMessage(String msg){this.msg = msg;}
    public String getMessage(){return this.msg;}

    public void setCode(String code){this.code = code;}
    public String getCode(){return this.code;}

}
