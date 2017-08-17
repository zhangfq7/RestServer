package com.asiainfo.ocmanager.rest.resource.downloadUtils;

import com.asiainfo.ocmanager.dacp.DFDataQuery;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;


import java.io.*;
import java.util.*;

/**
 * Created by zhangfq on 2017/8/2.
 */
public class GetFile {

    private static Properties prop = new Properties();
    private static Logger logger = Logger.getLogger(GetFile.class);

    static {
        String classPath = new GetFile().getClass().getResource("/").getPath();
        String currentClassesPath = classPath.substring(0, classPath.length() - 8)+ "conf/config.properties";
        try{
            InputStream inStream = new FileInputStream(new File(currentClassesPath ));
            prop.load(inStream);
        }catch(IOException e){
            logger.error(e.getMessage());
        }
    }

    /**
     * 获取keytab文件
     * @param tanantid
     * @param username
     * @return keytab file
     */
    public static Map getFile(String tanantid,String username){
        boolean isexists = GetFile.judgeFileExists(username);
        String filepaths = prop.getProperty("keytab.path") + username + prop.getProperty("keytab.type");
        Map results = null;
        try {
            results = createFile(tanantid,username);
            results.put("filepaths",filepaths);
            results.put("status","");
            if(isexists==true){
                //用户非首次下载，将本地保存的该用户keytab文件返回给用户
                File file = new File(filepaths);
                results.put("file",file);
                return results;
            }
            //判断返回状态
            if(results.get("file").equals("")){
                results.put("status","false");
            }else{
                results.put("status","true");
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return results;
    }

    /**
     * 生产keytab文件
     * @param tanantid
     * @param username
     * @return keytab file
     * @throws IOException
     */
    public static Map createFile(String tanantid,String username) throws IOException {
        String filepaths = prop.getProperty("keytab.path") + username + prop.getProperty("keytab.type");
        Map results = getKeytabData(tanantid,username);
        results.put("file","");
        results.put("msg","");
        List<String> userlist = (List)results.get("userlist");
        DownloadResult dr = new DownloadResult();
        if(results.get("result").equals("")){
            //当keytab信息为""时，判断错误原因
            if(!userlist.contains(username)){
                dr.setResult("failed");
                dr.setMessage("The user is not exist");
                results.put("msg",dr);
                return results;
            }else {
                dr.setResult("failed");
                dr.setMessage("Failed to obtain keytab information for this user,The information is empty");
                results.put("msg",dr);
                return results;
            }
        }else{
            //生成keytab文件
            Base64 base64 = new Base64();
            String decodekeytab = new String(base64.decode((String)results.get("result")));
            File dir = new File(prop.getProperty("keytab.path"));
            File file = new File(filepaths);
            try {
                dir.mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
            FileWriter fw = null;
            try {
                fw = new FileWriter(filepaths);
                fw.write(decodekeytab);
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (fw != null) {
                    fw.close();
                }
            }
            results.put("file",file);
        }
        return results;
    }

    /**
     * 获取keytab数据
     * @param tenantId
     * @param username
     * @return keytab data
     */
    public static Map getKeytabData(String tenantId,String username){
        String resource = DFDataQuery.GetTenantData(tenantId);
        logger.info("call DF tenant instance resource: \r\n"+resource);
        Map results = new HashMap<>();
        results.put("result","");
        String result = "";
        //定义用户集合，用于后续判断用户是否存在
        List<String> userlist = new ArrayList<>();
        try {
            JSONObject resourceJson = new JSONObject(resource);
            String items = resourceJson.getString("items");
            //判断items信息是否可用
            if(items.equals("[]")){
                return results;
            }
            JSONArray itemsJson = new JSONArray(items);
            for(int i = 0;i<itemsJson.length();i++){
                JSONObject json = new JSONObject(itemsJson.getString(i));
                String status = json.getString("status");
                JSONObject statusJson = new JSONObject(status);
                String phase = statusJson.getString("phase");
                String spec = json.getString("spec");
                JSONObject specJson = new JSONObject(spec);
                String binding = specJson.getString("binding");
                //判断binding信息是否可用
                if(binding==null||binding.equals("")||binding.equals("null")){
                    logger.info("Failed to obtain binding information for this user,The information is empty");
                    continue;
                }
                //捕捉bound状态
                if(phase.equals("Bound")){
                    JSONArray bindingJson = new JSONArray(binding);
                    for(int j = 0;j<bindingJson.length();j++){
                        JSONObject jsonn = new JSONObject(bindingJson.getString(j));
                        String credentials = jsonn.getString("credentials");
                        //判断credentials信息是否可用
                        if(credentials==null||credentials.equals("")||credentials.equals("null")){
                            logger.info("Failed to obtain credentials information for this user,The information is empty");
                            continue;
                        }
                        JSONObject credentialsJson = new JSONObject(credentials);
                        Iterator keys = credentialsJson.keys();
                        while (keys.hasNext()){
                            String principal = credentialsJson.getString("username");
                            String[] usernames = principal.split("@");
                            String splitusername = usernames[0];
                            userlist.add(splitusername);
                            //判断是否存在keytab
                            if(keys.next().equals("keytab")){
                                //判断有无匹配的用户
                                if(username.equals(splitusername)){
                                    String keytab = credentialsJson.getString("keytab");
                                    //判断keytab信息是否可用
                                    if(keytab==null||keytab.equals("")||keytab.equals("null")){
                                        logger.info("Failed to obtain keytab information for this user,The information is empty");
                                        continue;
                                    }else{
                                        result = keytab;
                                        results.put("result",result);
                                        results.put("userlist",userlist);
                                        return results;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            logger.error(e.getMessage());
        }
        results.put("userlist",userlist);
        return  results;
    }

    /**
     * 判断本地是否已存在该用户的keytab
     * @param username
     * @return
     */
    public static boolean judgeFileExists(String username) {

        File file = new File(prop.getProperty("keytab.path") + username + prop.getProperty("keytab.type"));
        if (file.exists()) {
            logger.info("The user is not download for the first time");
            return true;
        } else{
            logger.info("The user download for the first time");
            return false;
        }
    }
}
