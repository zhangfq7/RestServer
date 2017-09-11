package com.asiainfo.ocmanager.rest.resource.quotaUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import com.asiainfo.ocmanager.persistence.model.Quota;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Created by zhangfq on 2017/6/27.
 */
public class YarnUtil {

    private static Logger logger = Logger.getLogger(YarnUtil.class);

    /**
     * 获取yarn资源用量
     * @param queuename
     * @return yarnQuota
     */
    public static List<Quota> getYarnData(String queuename){

        String yarnurl = AmbariUtil.getUrl("yarn");
        String restresult = "";
        String yarnresturl = "http://"+yarnurl+"/ws/v1/cluster/scheduler";
        Quota memoryquota = new Quota("yarnQueueQuota","","","","queue memory quota");
        Quota vcoresquota = new Quota("queueVcoreQuota","","","","queue vcore qutoa");
        //通过yarn rest api获取资源用量信息
        HttpURLConnection conn = null;
        BufferedReader reader;
        try {
            conn = (HttpURLConnection)new URL(yarnresturl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            if(code == 200){
                reader = new BufferedReader(new InputStreamReader(
                        (conn.getInputStream())));
                String line = "";
                String fsResult = "";
                while ((line = reader.readLine()) != null) {
                    fsResult += line;
                }
                restresult = fsResult;
            }
        } catch (MalformedURLException e) {
            logger.error(e.getMessage());
            memoryquota.setName("yarnQueueQuota");
            memoryquota.setUsed("-1");
            vcoresquota.setName("queueVcoreQuota");
            vcoresquota.setUsed("-1");
            List<Quota> result = new ArrayList<Quota>();
            result.add(memoryquota);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            memoryquota.setName("yarnQueueQuota");
            memoryquota.setUsed("-1");
            vcoresquota.setName("queueVcoreQuota");
            vcoresquota.setUsed("-1");
            List<Quota> result = new ArrayList<Quota>();
            result.add(memoryquota);
            return result;
        } finally {
            conn.disconnect();
        }
        //解析返回的信息
        try {
            JSONObject json1 = new JSONObject(restresult);
            String scheduler = json1.getString("scheduler");
            JSONObject json2 = new JSONObject(scheduler);
            String schedulerInfo = json2.getString("schedulerInfo");
            JSONObject json3 = new JSONObject(schedulerInfo);
            String queues = json3.getString("queues");
            JSONObject json4 = new JSONObject(queues);
            String queue = json4.getString("queue");
            JSONArray json5 = new JSONArray(queue);
            for(int i = 0;i<json5.length();i++){
                String value = json5.getString(i);
                JSONObject json6 = new JSONObject(value);
                String tmpQueueName = json6.getString("queueName");
                if(queuename.equals(tmpQueueName)){
                    String resourcesUsed = json6.getString("resourcesUsed");
                    JSONObject json7 = new JSONObject(resourcesUsed);
                    String memory = json7.getString("memory");
                    logger.info(memory);
                    long memoryGb = Long.parseLong(memory)*1024*1024;
//                    long memoryGb = (long)(Integer.valueOf(memory)*1024*1024);
                    memoryquota.setName("queueMemoryQuota");
//                    memoryquota.setUsed(String.valueOf(memoryGb)+"(MB)");
                    memoryquota.setUsed(UnitConversion.unitConversion(memoryGb));
                    String vCores = json7.getString("vCores");
                    vcoresquota.setName("queueVcoreQuota");
                    vcoresquota.setUsed(vCores);
                }
            }
            logger.info("memoryquota.Used:"+memoryquota.getUsed());
            if(memoryquota.getUsed().equals("")){
                memoryquota.setUsed("-1");
            }
        } catch (JSONException e) {
            logger.error(e.getMessage());
            memoryquota.setName("yarnQueueQuota");
            memoryquota.setUsed("-1");
            vcoresquota.setName("queueVcoreQuota");
            vcoresquota.setUsed("-1");
            List<Quota> result = new ArrayList<Quota>();
            result.add(memoryquota);
            return result;
        }
        List<Quota> result = new ArrayList<Quota>();
        result.add(memoryquota);
        return result;
    }
}
