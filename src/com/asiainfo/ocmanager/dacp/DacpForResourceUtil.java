package com.asiainfo.ocmanager.dacp;

import com.asiainfo.ocmanager.dacp.model.DBDistribution;
import com.asiainfo.ocmanager.dacp.model.DBRegister;

import com.asiainfo.ocmanager.dacp.model.Team;
import com.asiainfo.ocmanager.dacp.service.TeamWrapper;
import com.asiainfo.ocmanager.dacp.utils.DBUrlEnum;
import com.asiainfo.ocmanager.dacp.utils.DbTypeEnum;
import com.asiainfo.ocmanager.dacp.utils.DriverTypeEnum;
import com.asiainfo.ocmanager.dacp.utils.KeyTabClient;
import com.asiainfo.ocmanager.rest.resource.downloadUtils.GetKeytabFile;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.*;
import java.util.*;

/**
 * Created by YANLSH
 * Created on 2017/7/3
 * Get the tenant's resources and synchronize to DACP
 * Sync succeeds and returns userInfoMap,else return false information
 * Parameter:tenantId
 */
public class DacpForResourceUtil {

    public static Log logger = LogFactory.getLog(DacpForResourceUtil.class);

    private static Properties propdacp = new Properties();

    static {
        String classPath = new DacpForResourceUtil().getClass().getResource("/").getPath();
        String currentClassesPath = classPath.substring(0, classPath.length() - 8) + "conf/config.properties";
        try {
            InputStream inStream = new FileInputStream(new File(currentClassesPath));
            //            prop = new Properties();
            propdacp.load(inStream);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private static Map<String, List> userInfoMap;
    private static List dbRegisterList;
    private static List dbDistributionList;
    private static String databasename = "";
    private static String username = "";
    private static String password = "";
    private static String uri = "";
    private static String url = "";
    private static String team_code = "";
    private static String thriftUri = "";
    private static String thriftUrl = "";

    /**
     * get database and database trans data from df using tenant id
     */
    public static Map<String, List> getDatabaseData(String tenantId) {
        Team team = TeamWrapper.getTeamFromTenant(tenantId);
        team_code = team.getteam_code();
        try {
            // get df backing service data using tenant id
            String resourceJson = DFDataQuery.GetTenantData(tenantId);
            logger.info("call DF tenant instance resource: \r\n" + resourceJson);
            JsonParser parser = new JsonParser();
            JsonObject object = (JsonObject) parser.parse(resourceJson);
            // get items
            JsonArray itemsArray = object.get("items").getAsJsonArray();
            userInfoMap = new HashMap<>();
            dbRegisterList = new ArrayList<>();
            dbDistributionList = new ArrayList<>();
            for (int i = 0; i < itemsArray.size(); i++) {
                JsonObject subObject = itemsArray.get(i).getAsJsonObject();
                JsonObject specJsonObj = subObject.get("spec").getAsJsonObject();
                JsonObject statusJsonObj = subObject.get("status").getAsJsonObject();
                String phase = statusJsonObj.get("phase").getAsString();
                String instance_id = specJsonObj.get("instance_id").getAsString();
                // if Failure do nothing
                if (!"Failure".equals(phase)) {
                    JsonObject provisioningJsonObj = specJsonObj.get("provisioning").getAsJsonObject();
                    String backingservice_name = provisioningJsonObj.get("backingservice_name").getAsString();//dbname
                    String driveTypeStr = provisioningJsonObj.get("backingservice_name").getAsString().toLowerCase();
                    String driverclassname = DriverTypeEnum.getDriverTypeEnum(driveTypeStr);

                    boolean hadoopflag = isHadoopflag(backingservice_name.toLowerCase());
                    // check if item is hadoop ecosystem,if not ,do nothing
                    if (hadoopflag) {//is hadoop ecosystem,get binding
                        // if unbound ,do nothing
                        if (!"Unbound".equals(phase)) {
                            // only backing_service is hive ,then send to dacp ,or do nothing
                            if (!backingservice_name.toLowerCase().equals("hive")) continue;
//                            hadoopDBEntityAssign(backingservice_name, specJsonObj);
                            hadoopDBEntityAssign(backingservice_name, specJsonObj,tenantId);
                            DBEntityAssign(instance_id, backingservice_name, driverclassname);
                        }
                    } else {// if backing service is not hadoop ecosytem,get credentials;including gp
                        boolean flag = provisioningJsonObj.get("credentials").isJsonObject();
                        if (flag) {
                            if (backingservice_name.toLowerCase().equals("neo4j") ||
                                backingservice_name.toLowerCase().equals("mongodb") ||
                                backingservice_name.toLowerCase().equals("rabbitmq") ||
                                backingservice_name.toLowerCase().equals("redis"))
                                continue;
                            JsonObject credentialsJsonObj = provisioningJsonObj.get("credentials").getAsJsonObject();
//                            assignForDBInfo(credentialsJsonObj, backingservice_name);
                            assignForDBInfo(credentialsJsonObj, backingservice_name,tenantId);
                        }
                        DBEntityAssign(instance_id, backingservice_name, driverclassname);
                    }
                }
            }
            userInfoMap.put("database", dbRegisterList);
            userInfoMap.put("transdatabase", dbDistributionList);

        } catch (Exception e) {
            e.printStackTrace();
            logger.info("DacpforResourceUtil Exception " + e.getMessage());
        }
        return userInfoMap;
    }

    /**
     * sort users of resource and get DBInfo
     * @Parameter backingservice_name is resource name ;  specJsonObj is resource link info
     * @returns void
     */
    private static void hadoopDBEntityAssign(String backingservice_name, JsonObject specJsonObj,String tenantid) {
        JsonParser parser = new JsonParser();
        if (specJsonObj.get("binding").isJsonArray()) {
            JsonArray bindingJsonArray = specJsonObj.get("binding").getAsJsonArray();

            String sortByUserBindingStr = sortByUsername(bindingJsonArray.toString());

            JsonObject bindObj = (JsonObject) parser.parse(sortByUserBindingStr);//have sort

            //JsonObject bindObj = bindingJsonArray.get(0).getAsJsonObject();//no sort

            if (bindObj != null) {
                JsonObject credentialJsonObj = bindObj.get("credentials").getAsJsonObject();
//                assignForDBInfo(credentialJsonObj, backingservice_name);
                assignForDBInfo(credentialJsonObj, backingservice_name,tenantid);
            }

        }
    }

    /**
     * Initializes data registration and allocation instances, and joins queues separately
     * @Parameter instance_id is instance_id  ;  backingservice_name is resource name ; driverclassname is driver class name
     * @returns void
     */
    private static void DBEntityAssign(String instance_id, String backingservice_name, String driverclassname) {
        /*数据库分配*/
        String state = "on";
        String remark = "";//remark
        if (backingservice_name.toLowerCase().equals("hive")) {
            //hive_
            DBRegister dbRegister_hive = new DBRegister();
            dbRegister_hive.setXmlid("hive_" + instance_id);
            dbRegister_hive.setDbname("hive_" + databasename);
            dbRegister_hive.setCnname("hive_" + databasename);
            dbRegister_hive.setDriverclassname(driverclassname);
            dbRegister_hive.setUrl(url);
            dbRegister_hive.setUsername(username);
            dbRegister_hive.setPassword(password);
            dbRegister_hive.setRemark(remark);
            dbRegister_hive.setAlias(backingservice_name.toLowerCase());
            DBDistribution dbDistribution_hive = new DBDistribution();
            dbDistribution_hive.setDbname("hive_" + databasename);
            dbDistribution_hive.setCnname("hive_" + databasename);
            dbDistribution_hive.setDriverclassname(driverclassname);
            dbDistribution_hive.setUrl(url);
            dbDistribution_hive.setUsername(username);
            dbDistribution_hive.setPassword(password);
            dbDistribution_hive.setState(state);
            dbDistribution_hive.setTeam_code(team_code);
            dbDistribution_hive.setDbtype(backingservice_name.toLowerCase());
            dbRegisterList.add(dbRegister_hive);
            dbDistributionList.add(dbDistribution_hive);
            //spark_
            DBRegister dbRegister_sparksql = new DBRegister();
            dbRegister_sparksql.setXmlid("spark_" + instance_id);
            dbRegister_sparksql.setDbname("spark_" + databasename);
            dbRegister_sparksql.setCnname("spark_" + databasename);
            dbRegister_sparksql.setDriverclassname(driverclassname);
            dbRegister_sparksql.setUrl(thriftUrl);
            dbRegister_sparksql.setUsername(username);
            dbRegister_sparksql.setPassword(password);
            dbRegister_sparksql.setRemark(remark);
            dbRegister_sparksql.setAlias(backingservice_name.toLowerCase());

            DBDistribution dbDistribution_sparksql = new DBDistribution();
            dbDistribution_sparksql.setDbname("spark_" + databasename);
            dbDistribution_sparksql.setCnname("spark_" + databasename);
            dbDistribution_sparksql.setDriverclassname(driverclassname);
            dbDistribution_sparksql.setUrl(thriftUrl);
            dbDistribution_sparksql.setUsername(username);
            dbDistribution_sparksql.setPassword(password);
            dbDistribution_sparksql.setState(state);
            dbDistribution_sparksql.setTeam_code(team_code);
            dbDistribution_sparksql.setDbtype(backingservice_name.toLowerCase());

            dbRegisterList.add(dbRegister_sparksql);
            dbDistributionList.add(dbDistribution_sparksql);
        } else {
            DBRegister dbRegister = new DBRegister();
            dbRegister.setXmlid(instance_id);
            dbRegister.setDbname(databasename);
            dbRegister.setCnname(databasename);
            dbRegister.setDriverclassname(driverclassname);
            dbRegister.setUrl(url);
            dbRegister.setUsername(username);
            dbRegister.setPassword(password);
            dbRegister.setRemark(remark);
            dbRegister.setAlias(backingservice_name.toLowerCase());

            DBDistribution dbDistribution = new DBDistribution();
            dbDistribution.setDbname(databasename);
            dbDistribution.setCnname(databasename);
            dbDistribution.setDriverclassname(driverclassname);
            dbDistribution.setUrl(url);
            dbDistribution.setUsername(username);
            dbDistribution.setPassword(password);
            dbDistribution.setState(state);
            dbDistribution.setTeam_code(team_code);
            dbDistribution.setDbtype(backingservice_name.toLowerCase());

            dbRegisterList.add(dbRegister);
            dbDistributionList.add(dbDistribution);
        }

    }

    /**
     * Data is registered and assigned instances, and queues are added
     * @Parameter credentialsJsonObj is resource credential JsonObject  ;  backingservice_name is resource name ;
     * @returns void
     */
//    private static void assignForDBInfo(JsonObject credentialsJsonObj, String backingservice_name,String tenantid)
    private static void assignForDBInfo(JsonObject credentialsJsonObj, String backingservice_name,String tenantid) {
        if (credentialsJsonObj.get("username") != null) {
            username = credentialsJsonObj.get("username").getAsString();//username
        }
        if (credentialsJsonObj.get("password") != null) {
            password = credentialsJsonObj.get("password").getAsString();//password
        }
        if (credentialsJsonObj.has("uri")) {
            uri = credentialsJsonObj.get("uri").getAsString();//uri
        }
        String host = credentialsJsonObj.get("host").getAsString();//host
        String port = credentialsJsonObj.get("port").getAsString();//port
        if (DbTypeEnum.getDbFlagEnum(backingservice_name.toLowerCase()).equals("true")) {
            if (credentialsJsonObj.get("Hive database") != null) {
                String hiveDatabase = credentialsJsonObj.get("Hive database").getAsString();
                databasename = hiveDatabase;
                keyTabFileCreateAndDeploy(tenantid);
            }
            //now only hive do this operation
//            keyTabFileCreateAndDeploy(credentialsJsonObj);
//            keyTabFileCreateAndDeploy(tenantid);
        } else {
            if (credentialsJsonObj.get("name") != null) {
                databasename = credentialsJsonObj.get("name").getAsString();
            }
        }
        if (credentialsJsonObj.has("thriftUri")) {
            thriftUri = credentialsJsonObj.get("thriftUri").getAsString();
        }
        thriftUrl = DBUrlEnum.getDBUrlEnum(backingservice_name.toLowerCase(), thriftUri, host, port, databasename, username);//thriftUrl
        url = DBUrlEnum.getDBUrlEnum(backingservice_name.toLowerCase(), uri, host, port, databasename, username);//url
    }


    /**
     * hive of hadoop resource keyTab file create and deploy
     * @Parameter credentialsJsonObj is resource credential JsonObject
     * @returns void
     */
//    private static void keyTabFileCreateAndDeploy(JsonObject credentialsJsonObj) {
    private static void keyTabFileCreateAndDeploy(String tenantid) {
        Process process = null;
        List<String> processList = new ArrayList<String>();
        BufferedReader input = null;
        Boolean flag = true;
        try {
            //begin to create Keytab file
//            String keyTabStr = credentialsJsonObj.get("keytab").getAsString();
            String[] usernames = username.split("@");
            String user = usernames[0];
            Map result = GetKeytabFile.returnResult(tenantid,user);
            logger.info("result:" +result);
            String src_file = (String)result.get("filepaths");
            logger.info("src_file"+src_file);
            flag = (boolean)result.get("flag");
            logger.info("flag"+flag);
            String keyTabFilePath = propdacp.getProperty("keytab.path");
            String keyTabFileDesPath = propdacp.getProperty("keytab.destpath");
            String keyTabFileType = propdacp.getProperty("keytab.type");
            String keyTabFileName = username + keyTabFileType;
            String dest_file = keyTabFileDesPath + keyTabFileName;
            String tag = "dacp";
//            if (credentialsJsonObj.get("keytab") != null) {
//                flag = KeyTabClient.CreatKeyTab(keyTabStr, keyTabFilePath, keyTabFileName);
//                logger.info("keyTabStr: " + keyTabStr + "\nkeyTabFilePath: " + keyTabFilePath + " \ncreateKeyTabFlag:"+flag);
//            }
            if (flag) {
//                begin to deploy Keytab file
                String shellClassPath = new DacpForResourceUtil().getClass().getResource("/").getPath();
                String shellPath = shellClassPath.substring(0, shellClassPath.length() - 8) + "conf/deployKeytab/";
                String execStr = "sh " + shellPath + propdacp.getProperty("deploy.sh.name") + " " + src_file + " " + dest_file + " " + tag + "\n";
                logger.info("keyTabFileCreateAndDeploy execStr: " + execStr);
                process = Runtime.getRuntime().exec(execStr);
                input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            } else {
                throw new Exception("Create KeyTab File Failed!");
            }
        } catch (Exception e) {
            logger.error("dacpForResourceUtil keyTabFileCreateAndDeploy Exception:" + e.getMessage());
        } finally {
            try {
                String line = "";
                while ((line = input.readLine()) != null) {
                    processList.add(line);
                }
                if ("success".equals(processList.get(processList.size()-1))) {
                    logger.info("keyTabFileCreateAndDeploy success");
                } else if ("failed".equals(processList.get(processList.size()-1))) {
                    logger.info("keyTabFileCreateAndDeploy failed");
                }
                for(String pro:processList){
                    logger.info(pro+"\r\n");
                }
                input.close();
            } catch (Exception e) {
                logger.info("KafkaUtils getKafkaSpaceQuota Exception:" + e.getMessage());
            }

        }
    }



    /**
     * Determines whether the component is of the Hadoop type
     * @Parameter backingservice_name is resource name
     * @returns true is hadoop; else is not
     */
    private static boolean isHadoopflag(String backingservice_name) {
        String dbflag = DbTypeEnum.getDbFlagEnum(backingservice_name);
        if (dbflag.equals("true")) {
            return true;
        } else {
            return false;
        }
    }



    /**
     * The user list is sorted under the resource component
     * @Parameter bindingJsonArray is bingding resource contents
     * @returns Sorted list
     */
    private static String sortByUsername(String bindingJsonArray) {
        JSONArray jsonArr = null;
        JSONArray sortedJsonArray = new JSONArray();
        List<JSONObject> jsonValues = new ArrayList<JSONObject>();
        String bindObjStr = "";
        try {
            jsonArr = new JSONArray(bindingJsonArray);
            for (int i = 0; i < jsonArr.length(); i++) {
                jsonValues.add(jsonArr.getJSONObject(i));
            }
            Collections.sort(jsonValues, new Comparator<JSONObject>() {
                private static final String KEY_NAME = "bind_hadoop_user";

                public int compare(JSONObject a, JSONObject b) {
                    String valA = new String();
                    String valB = new String();
                    try {
                        valA = (String) a.get(KEY_NAME);
                        valB = (String) b.get(KEY_NAME);
                    } catch (JSONException e) {
                        logger.info("DacpforResourceUtil Collections sort compare" + e.getMessage());
                    }
                    return valA.compareTo(valB);
                }
            });
            for (int i = 0; i < jsonArr.length(); i++) {
                sortedJsonArray.put(jsonValues.get(i));
            }
            bindObjStr = sortedJsonArray.get(0).toString();
        } catch (JSONException e) {
            logger.info("DacpforResourceUtil sortByUsername" + e.getMessage());
        }
        return bindObjStr;
    }
}
