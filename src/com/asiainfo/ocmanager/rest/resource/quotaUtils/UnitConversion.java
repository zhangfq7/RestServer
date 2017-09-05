package com.asiainfo.ocmanager.rest.resource.quotaUtils;

/**
 * Created by zhangfq on 2017/9/4.
 */
public class UnitConversion {

    /**
     * This method is used for conversion of resource usage units
     * @param olddata(B)
     * @return
     */
    public static String unitConversion(long olddata){

        String newdata = new String();
        if(olddata>0){
            if(olddata/1024>1){
                if(olddata/1024/1024>1){
                    if(olddata/1024/1024/1024>1){
                        newdata = String.valueOf(olddata/1024/1024/1024)+"(GB)";
                        return newdata;
                    }
                    newdata = String.valueOf(olddata/1024/1024)+"(MB)";
                    return newdata;
                }
                newdata = String.valueOf(olddata/1024)+"(KB)";
                return newdata;
            }
            newdata = String.valueOf(olddata)+"(B)";
            return newdata;
        }
        return String.valueOf(olddata)+"(B)";
    }

}
