package com.asiainfo.ocmanager.rest.resource.downloadUtils;

import com.asiainfo.bdx.ldp.datafoundry.servicebroker.ocdp.exception.KerberosOperationException;
import com.asiainfo.ocmanager.dacp.DFDataQuery;
import org.apache.directory.server.kerberos.shared.keytab.Keytab;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Created by zhangfq on 2017/8/28.
 */
public class GetKeytabFile {

    private static Properties prop = new Properties();
    private static Logger logger = Logger.getLogger(GetKeytabFile.class);

    static {
        String classPath = new GetKeytabFile().getClass().getResource("/").getPath();
        String currentClassesPath = classPath.substring(0, classPath.length() - 8)+ "conf/config.properties";
        try{
            InputStream inStream = new FileInputStream(new File(currentClassesPath ));
            prop.load(inStream);
        }catch(IOException e){
            logger.error(e.getMessage());
        }
    }

    /**
     * 判断用户是否首次下载，返回结果
     * @param tanantid
     * @param username
     * @return
     */
    public static Map returnResult(String tanantid,String username){
        boolean isexists = GetKeytabFile.judgeFileExists(username);
        String filepaths = prop.getProperty("keytab.path") + username + prop.getProperty("keytab.type");
        Map results = new HashMap<>();
        if(isexists==true){
            //用户非首次下载，将本地保存的该用户keytab文件返回给用户
            File file = new File(filepaths);
            results.put("file",file);
            results.put("filepaths",filepaths);
            DownloadResult dr = new DownloadResult();
            dr.setResult("success");
            dr.setCode("200");
            dr.setMessage("");
            results.put("res",dr);
            results.put("flag",true);
            return results;
        }
        results = getKeytabFile(tanantid,username);
        if(results.containsKey("itemsnull")){
            DownloadResult dr = new DownloadResult();
            dr.setResult("failed");
            dr.setCode("404");
            dr.setMessage("此基线下不存在该用户，请选择正确的基线！");
            results.put("res",dr);
            results.put("flag",false);
            return results;
        }
        List userlist = (List)results.get("userlist");
        DownloadResult dr = new DownloadResult();
        if(userlist==null){
            userlist=new ArrayList<>();
        }
        if(results.get("file").equals("")||results.get("file")==null){
            //当keytab信息为""时，判断错误原因
            if(!userlist.contains(username)) {
                dr.setResult("failed");
                dr.setCode("404");
                dr.setMessage("用户未绑定到当前服务列表。");
                results.put("res", dr);
                return results;
            }else{
                dr.setResult("failed");
                dr.setCode("404");
                dr.setMessage("文件创建失败！");
                results.put("res", dr);
                return results;
            }
        }
        dr.setResult("success");
        dr.setCode("200");
        dr.setMessage("");
        results.put("res",dr);
        results.put("filepaths",filepaths);
        return results;
    }

    /**
     * 获取keytab文件
     * @param tenantId
     * @param username
     * @return keytab dataile
     */
    public static Map getKeytabFile(String tenantId,String username){
        String resource = DFDataQuery.GetTenantData(tenantId);
        logger.info("call DF tenant instance resource: \r\n"+resource);
        Map results = new HashMap<>();
        String result = "";
        //定义用户集合，用于后续判断用户是否存在
        List<String> userlist = new ArrayList<>();
        try {
            JSONObject resourceJson = new JSONObject(resource);
            String items = resourceJson.getString("items");
            //判断items信息是否可用
            if(items.equals("[]")){
                results.put("itemsnull","The tenant does not exist for this user, please choose the correct baseline");
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
//                    logger.info("Failed to obtain binding information for this user,The information is empty");
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
//                            logger.info("Failed to obtain credentials information for this user,The information is empty");
                            continue;
                        }
                        JSONObject credentialsJson = new JSONObject(credentials);
                            String principal = credentialsJson.getString("username");
                            String password = credentialsJson.getString("password");
                            String keytabFilePath = createKeyTabFilePath(username);
                            String adminPwd = prop.getProperty("adminpwd");
                            String kdcHost = prop.getProperty("dacp.java.security.krb5.kdc");
                            String realm = prop.getProperty("dacp.java.security.krb5.realm");
                            String[] usernames = principal.split("@");
                            String splitusername = usernames[0];
                            userlist.add(splitusername);
                            //判断有无匹配的用户
                            if(username.equals(splitusername)){
                                File res = createKeyTab(principal,password,keytabFilePath,adminPwd,kdcHost,realm);
                                if(res!=null){
                                    results.put("file",res);
                                    results.put("userlist",userlist);
                                    results.put("flag",true);
                                    return results;
                                }else{
                                    results.put("file",res);
                                    results.put("userlist",userlist);
                                    results.put("flag",false);
                                    return results;
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            logger.error(e.getMessage());
            results.put("flag",false);
        }
        results.put("file","");
        results.put("userlist",userlist);
        return  results;
    }


    /**
     * 生成新的keytab文件
     * @param principal
     * @param password
     * @param keytabFilePath
     * @param adminPwd
     * @param kdcHost
     * @param realm
     * @return File
     */
    public static File createKeyTab(String principal,String password,String keytabFilePath,String adminPwd,String kdcHost,String realm){
        Keytab keytab = new Keytab();
        File result = null;
        try{
            Map<String,String> config = new HashMap<>();
            config.put("userPrincipal",principal);
            config.put("keytabLocation",keytabFilePath);
            config.put("adminPwd",adminPwd);
            config.put("kdcHost",kdcHost);
            config.put("realm",realm);
            KerberosClient krb = new KerberosClient(config);
            keytab = krb.createKeyTab(principal,password, 0);
            result =  krb.createKeyTabFile(keytab,keytabFilePath);
            System.out.println("result is :" +String.valueOf(result));
        }catch (Exception e){
            System.out.println(e);
        }
        return result;

    }

    /**
     * 创建keytab文件，获取keytabpath
     * @param
     */
    public static String createKeyTabFilePath(String username){
        String filepaths = prop.getProperty("keytab.path") + username + prop.getProperty("keytab.type");
        File dir = new File(prop.getProperty("keytab.path"));
        dir.mkdirs();
        return filepaths;
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
