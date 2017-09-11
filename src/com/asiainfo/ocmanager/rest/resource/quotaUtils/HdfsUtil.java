package com.asiainfo.ocmanager.rest.resource.quotaUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.asiainfo.ocmanager.persistence.model.Quota;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.ContentSummary;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.log4j.Logger;

/**
 * Created by zhangfq on 2017/6/27.
 */
public class HdfsUtil {

    public static final Configuration conf = new Configuration();
    private static Properties prop = new Properties();
    private static Logger logger = Logger.getLogger(HdfsUtil.class);

    static {
        String classPath = new AmbariUtil().getClass().getResource("/").getPath();
        String currentClassesPath = classPath.substring(0, classPath.length() - 8)+ "conf/config.properties";
        try{
            InputStream inStream = new FileInputStream(new File(currentClassesPath ));
            prop.load(inStream);
        }catch(IOException e){
            logger.error(e.getMessage());
        }
    }

    /**
     * 获取hdfs资源用量信息
     * @param path
     * @return hdfsQuota
     */
    public static List<Quota> getHDFSData(String path){

        //进行kerberos认证
        String currentClassPath = new HdfsUtil().getClass().getResource("/").getPath();
        String  keytabPath= currentClassPath.substring(0, currentClassPath.length() - 8) + "conf/"+prop.getProperty("hdfs.kerberos.keytab.name");
        String  krbPath = currentClassPath.substring(0,currentClassPath.length() - 8) + "conf/"+prop.getProperty("kerberos.krb.name");
        String dfsurl =AmbariUtil.getUrl("hdfs");

        conf.set("hdfs.keytab.file", keytabPath);
        conf.set("fs.defaultFS",dfsurl);
        conf.set("hadoop.security.authentication","kerberos");
        System.setProperty("java.security.krb5.conf",krbPath);
        UserGroupInformation.setConfiguration(conf);

        Quota filesquota = new Quota("nameSpaceQuota","","","","hdfs file quota");
        Quota spacequota = new Quota("storageSpaceQuota","","","","hdfs space quota");
        try {
            UserGroupInformation.loginUserFromKeytab(prop.getProperty("hadoop.kerberos.principal"),keytabPath);
            System.out.println("----============-----");
            FileSystem fs = FileSystem.get(conf);
            MyContentSummary mcs = new MyContentSummary();
            ContentSummary contentSum = mcs.getContentSummary(new Path(path),fs);
//            ContentSummary contentSum = fs.getContentSummary(new Path(path));
            //获取资源信息并封装
            long Quota = contentSum.getQuota();
            long FileCount = contentSum.getFileCount();
            logger.info("quota:"+Quota+"-------   filecount:"+FileCount);
            if(Quota==-1){
                filesquota.setSize("");
                filesquota.setUsed(String.valueOf(FileCount));
                filesquota.setAvailable("");
            }else {
                filesquota.setSize(String.valueOf(Quota));
                filesquota.setUsed(String.valueOf(FileCount));
                filesquota.setAvailable(String.valueOf(Quota-FileCount));
            }
            long spaceQuota = contentSum.getSpaceQuota();
            long spaceConsumed = contentSum.getSpaceConsumed();
            logger.info("spacequota:"+spaceQuota+"---------  spaceconsumed:"+spaceConsumed);
            if(spaceQuota==-1){
                spacequota.setSize("");
//                spacequota.setUsed(String.valueOf(spaceConsumed)+"(B)");
                spacequota.setUsed(UnitConversion.unitConversion(spaceConsumed));
                spacequota.setAvailable("");
            }else {
//                spacequota.setSize(String.valueOf(spaceQuota)+"(B)");
//                spacequota.setUsed(String.valueOf(spaceConsumed)+"(B)");
//                spacequota.setAvailable(String.valueOf(spaceQuota-spaceConsumed)+"(B)");
                spacequota.setSize(UnitConversion.unitConversion(spaceQuota));
                spacequota.setUsed(UnitConversion.unitConversion(spaceConsumed));
                spacequota.setAvailable(UnitConversion.unitConversion((spaceQuota-spaceConsumed)));
            }
        } catch (IOException e) {
            logger.error("IOException :" +e);
            e.printStackTrace();
            filesquota.setSize("");
            filesquota.setUsed(String.valueOf("-1"));
            filesquota.setAvailable("");
            spacequota.setSize("");
            spacequota.setUsed(String.valueOf("-1"));
            spacequota.setAvailable("");
            List<Quota> result = new ArrayList<Quota>();
            result.add(filesquota);
            result.add(spacequota);
            return result;
        }
        List<Quota> result = new ArrayList<Quota>();
        result.add(filesquota);
        result.add(spacequota);
        return result;
    }
}
