package com.asiainfo.ocmanager.rest.resource.quotaUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.asiainfo.ocmanager.rest.utils.DFPropertiesFoundry;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

/**
 * Created by zhangfq on 2017/6/27.
 */
public class AmbariUtil {

    private static final String HDFS_PATH = "dfs.namenode.rpc-address";
    private static final String YARN_PATH = "yarn.resourcemanager.webapp.address";
    private static final String HBASE_PATH = "hbase.zookeeper.quorum";
    private static Properties prop = new Properties();
    private static Logger logger = Logger.getLogger(AmbariUtil.class);

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
     * 通过ambari rest api 获取不同服务所在的主机名
     * @param servicename
     * @return host name
     */
    public static String getUrl(String servicename){

        String result = "";
        if(servicename.equals("hdfs")){
            result = getHdfsUrl();
        }else{
            String clustername = getClustername();
            String url = "";

            if(servicename.equals("yarn")){
                url = "http://"+(prop.getProperty("ambari.host"))+"/api/v1/clusters/"+clustername+"/configurations?type="
                    +servicename+"-"+prop.getProperty("ambari.type")+"&tag="+getTags("yarn");
            }
            if(servicename.equals("hbase")){
                url = "http://"+(prop.getProperty("ambari.host"))+"/api/v1/clusters/"+clustername+"/configurations?type="
                    +servicename+"-"+prop.getProperty("ambari.type")+"&tag="+getTags("hbase");
            }

            logger.info("url is :"+url);
            HttpURLConnection conn = null ;
            try {
                conn = (HttpURLConnection)new URL(url).openConnection();
                Base64 base64 = new Base64();
                String encoded = base64.encodeToString((prop.getProperty("ambari.username") + ":" + prop.getProperty("ambari.password")).getBytes("UTF-8"));
                conn.setRequestProperty("Authorization", "Basic " + encoded);
                int resultCode = conn.getResponseCode();
                logger.info("response code is :" + resultCode);
                if (resultCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line = "";
                    String fsResult = "";
                    while ((line = reader.readLine()) != null) {
                        fsResult += line;
                    }
                    JSONObject json = new JSONObject(fsResult);
                    JSONArray items = json.getJSONArray("items");
                    String itemStr = items.toString().replace("[","").replace("]","");
                    JSONObject newItem = new JSONObject(itemStr);
                    JSONObject properties = newItem.getJSONObject("properties");
                    logger.info("servicename is :" +servicename);
                    result = properties.getString(getparametername(servicename));
                }

            } catch (Exception e) {
                logger.error(e.getMessage());
            } finally {
                conn.disconnect();
            }
        }

        return result;

    }

    /**
     * 获取hdfs服务主机信息
     * @return hdfs host name
     */
    public static String getHdfsUrl(){

        String clustername = getClustername();
        String url = "http://"+(prop.getProperty("ambari.host"))+"/api/v1/clusters/"+clustername+"/host_components?HostRoles/component_name=NAMENODE&metrics/dfs/FSNamesystem/HAState=active";
        logger.info("url is :"+url);
        HttpURLConnection conn = null ;
        String result = "";
        try {
            conn = (HttpURLConnection)new URL(url).openConnection();
            Base64 base64 = new Base64();
            String encoded = base64.encodeToString((prop.getProperty("ambari.username") + ":" + prop.getProperty("ambari.password")).getBytes("UTF-8"));
            conn.setRequestProperty("Authorization", "Basic " + encoded);
            int resultCode = conn.getResponseCode();
            logger.info("response code is :" + resultCode);
            if (resultCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line = "";
                String fsResult = "";
                while ((line = reader.readLine()) != null) {
                    fsResult += line;
                }
                JSONObject json = new JSONObject(fsResult);
                JSONArray items = json.getJSONArray("items");
                String itemStr = items.toString().replace("[","").replace("]","");
                JSONObject newItem = new JSONObject(itemStr);
                JSONObject hostRoles = newItem.getJSONObject("HostRoles");
                String hostname = hostRoles.getString("host_name");
                result = hostname;
                logger.info("hostname is:"+result);
                logger.info("servicename is :hdfs");
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            conn.disconnect();
        }
        return result;
    }

    /**
     * 通过服务名称获取查询所需的key值
     * @param servicename
     * @return parametername
     */
    public static String getparametername(String servicename){

        String parametername = null;
        if(servicename=="hdfs"){
            parametername = HDFS_PATH;
        }else if(servicename=="yarn"){
            parametername = YARN_PATH;
        }else if(servicename=="hbase"){
            parametername = HBASE_PATH;
        }

        return parametername;
    }

    /**
     * 获取version信息
     * @param servicename
     * @return version
     */
    public static String getTags(String servicename){

        String result = "";
        String clustername = getClustername();
        String url = "http://"+prop.getProperty("ambari.host")+"/api/v1/clusters/"+clustername+"/configurations?type=";
        String assurl = url+servicename+"-"+prop.getProperty("ambari.type");
        logger.info("url is :"+assurl);
        HttpURLConnection conn = null ;
        try {
            conn = (HttpURLConnection)new URL(assurl).openConnection();
            Base64 base64 = new Base64();
            String encoded = base64.encodeToString((prop.getProperty("ambari.username") + ":" + prop.getProperty("ambari.password")).getBytes("UTF-8"));
            conn.setRequestProperty("Authorization", "Basic " + encoded);
            int resultCode = conn.getResponseCode();
            logger.info("response code is :" + resultCode);
            if (resultCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line = "";
                String fsResult = "";
                while ((line = reader.readLine()) != null) {
                    fsResult += line;
                }
                List list = new ArrayList<>();
                JSONObject json = new JSONObject(fsResult);
                JSONArray items = json.getJSONArray("items");
                String lastObject = items.getString(items.length()-1);
                JSONObject lastversion = new JSONObject(lastObject);
                result = lastversion.getString("tag");
                logger.info("servicename is :hdfs");
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            conn.disconnect();
        }
        return result;
    }

    /**
     * 获取集群名称
     * @return clustername
     */
    public static String getClustername(){

        String result="";
        String url = "http://"+(prop.getProperty("ambari.host"))+"/api/v1/clusters";
        logger.info("get cluster name ,url is :"+url);
        HttpURLConnection conn = null ;

        try {
            conn = (HttpURLConnection)new URL(url).openConnection();
            Base64 base64 = new Base64();
            String encoded = base64.encodeToString((prop.getProperty("ambari.username") + ":" + prop.getProperty("ambari.password")).getBytes("UTF-8"));
            conn.setRequestProperty("Authorization", "Basic " + encoded);
            int resultCode = conn.getResponseCode();
            logger.info("result code is:"+resultCode);
            if (resultCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line = "";
                String fsResult = "";
                while ((line = reader.readLine()) != null) {
                    fsResult += line;
                }
                logger.info("cluster info is :"+fsResult);
                JSONObject json = new JSONObject(fsResult);
                JSONArray items = json.getJSONArray("items");
                logger.info("items is:"+items.toString());
                String item = items.getString(0);
                JSONObject json1 = new JSONObject(item);
                String clusters = json1.getString("Clusters");
                logger.info("clusters is :"+clusters);
                JSONObject json2 = new JSONObject(clusters);
                String clustername = json2.getString("cluster_name");
                logger.info("cluster_name is:"+clustername);
                result = clustername;
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            conn.disconnect();
        }
        return result;
    }

}
