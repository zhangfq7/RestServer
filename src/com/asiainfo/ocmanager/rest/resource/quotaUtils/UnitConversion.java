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
        if(olddata>1024){
            long kbdata = olddata/1024;
            if(kbdata>1024){
                long mbdata = kbdata/1024;
                if(mbdata>1024){
                    long gbdata = mbdata/1024;
                    newdata = String.valueOf(gbdata)+"(GB)";
                    return newdata;
                }
                newdata = String.valueOf(mbdata)+"(MB)";
                return newdata;
            }
            newdata = String.valueOf(kbdata)+"(KB)";
            return newdata;
        }
        return String.valueOf(olddata)+"(B)";
    }

}
