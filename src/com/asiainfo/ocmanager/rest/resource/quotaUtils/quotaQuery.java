package com.asiainfo.ocmanager.rest.resource.quotaUtils;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import com.asiainfo.ocmanager.mail.ParamQuery;
import com.asiainfo.ocmanager.persistence.model.Quota;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.*;
import org.apache.log4j.Logger;
import org.bson.Document;

/**
 *
 * @author yujing2
 *
 */
public class quotaQuery {


    private static Logger logger = Logger.getLogger(quotaQuery.class);


    /**
     *
     * @param path
     * @return Hdfs Quota
     */
    public static Map<String,List<Quota>> getHdfsQuota(String path) {
        Map<String,List<Quota>> result = new HashMap<String,List<Quota>>();
        List<Quota> dfsquota = HdfsUtil.getHDFSData(path);
        result.put("items", dfsquota);
        return result;
    }

    /**
     *
     * @param queuename
     * @return Yarn Quota
     */
    public static Map<String,List<Quota>> getMrQuota(String queuename) {
        Map<String,List<Quota>> result = new HashMap<String,List<Quota>>();
        List<Quota> mrquota = YarnUtil.getYarnData(queuename);
        result.put("items", mrquota);
        return result;
    }

    /**
     *
     * @param queuename
     * @return Spark Quota
     */
    public static Map getSparkQuota(String queuename) {
        Map result = new HashMap();
        List<Quota> sparkQuota = YarnUtil.getYarnData(queuename);
        List<Quota> items = new ArrayList<Quota>();
        Iterator<Quota> iterator = sparkQuota.iterator();
        while(iterator.hasNext()){
            items.add(iterator.next());
        }
        result.put("items",items);
        return result;
    }


    /**
     *
     * @param namespace
     * @return Hbase Quota
     */
    public static Map<String,List<Quota>> getHbaseQuota(String namespace) {
        Map<String,List<Quota>> result = new HashMap<String,List<Quota>>();
        List<Quota> hbasequota = HbaseUtil.getHbaseData(namespace);
        result.put("items", hbasequota);
        return result;
    }


    /**
     *
     * @param dbname
     * @param queuename
     * @return hive Quota
     */
    public static Map getHiveQuota(String dbname,String queuename) {
        Map result = new HashMap();
        /*
        todo :move hive quota code to here
         */
        String path = "/apps/hive/warehouse/"+dbname+".db";
        List<Quota> hdfsQuota = HdfsUtil.getHDFSData(path);
        List<Quota> queueQuota = YarnUtil.getYarnData(queuename);
        //yarn quota
        Iterator<Quota> iterator = queueQuota.iterator();
        List<Quota> items = new ArrayList<Quota>();
        while(iterator.hasNext()){
            items.add(iterator.next());
        }
        // hdfs quota
        Iterator<Quota> hdfsIter = hdfsQuota.iterator();
        while(hdfsIter.hasNext()){
            items.add(hdfsIter.next());
        }
        result.put("items",items);
        return result;
    }
    /**
     * get the Quota of the kafka based on the topicName
     * @Parameter :topicName
     * @returns KafkaQuota Quota,If there is a network exception or topicName not exist, returns the default kafka Quota
     */
    public static Map getKafkaQuota(String topicName) {
        Map result = new HashMap();
        List<Quota> items = new ArrayList<>();

        Quota topicPartitionQuota = kafkaUtils.getKafkaPartitionNumQuota(topicName);
        Quota kafkaSpaceQuota = kafkaUtils.getKafkaSpaceQuota(topicName);
        items.add(topicPartitionQuota);
        items.add(kafkaSpaceQuota);
        result.put("items",items);
        return result;
    }

    /**
     * get the size of the GP based on the databasename
     * @Parameter :database
     * @returns GP Quota,If there is a network exception or database not exist, returns the default GP Quota
     */
    public static Map getGpQuota(String database) {
        Map result = new HashMap();
        List<Quota> items = new ArrayList<Quota>();
        long useplace = 0;
        Quota volumeSize= new Quota("volumeSize","","0","","greenplum database used Size");
        try {
            Class.forName("org.postgresql.Driver");
            String hostport = ParamQuery.getCFProperties().get(ParamQuery.GREENPLUM_HOST_PORT);
            Connection db1 = DriverManager.getConnection("jdbc:postgresql://"+hostport+"/" + database, "gpadmin", "asiainfoldp");
            Statement st = db1.createStatement();
            ResultSet rs = st.executeQuery("select  sum((pg_relation_size(relid)))from pg_stat_user_tables");
            while (rs.next()) {
                useplace = Long.valueOf(rs.getString(1));
            }
            //String used = String.valueOf(useplace);
            String used = UnitConversion.unitConversion(useplace);
            volumeSize= new Quota("volumeSize","",used,"","greenplum database used Size");
            rs.close();
            st.close();
        } catch (ClassNotFoundException e) {
            logger.info("quotaQuery getGpQuota ClassNotFoundException" + e.getMessage());
            volumeSize= new Quota("volumeSize","","-1","","greenplum database used Size");
            items.add(volumeSize);
            result.put("items", items);
            return result;
        } catch (SQLException e) {
            logger.info("quotaQuery getGpQuota SQLException" + e.getMessage());
            volumeSize= new Quota("volumeSize","","-1","","greenplum database used Size");
            items.add(volumeSize);
            result.put("items", items);
            return result;
        } catch (IOException e) {
            logger.info("quotaQuery getGpQuota IOException" + e.getMessage());
            volumeSize= new Quota("volumeSize","","-1","","greenplum database used Size");
            items.add(volumeSize);
            result.put("items", items);
            return result;
        }
        items.add(volumeSize);
        result.put("items", items);
        return result;
    }


    /**
     * get the size of the mongo based on the databasename
     * @Parameter :databasename
     * @returns mongo Quota,If there is a network exception or databasename not exist, returns the default mongo Quota
     */
    public static Map getMongoQuota(String databasename) {
        Map result = new HashMap();
        List<Quota> items = new ArrayList<Quota>();
        Quota volumSize= new Quota("volumeSize","","0","","mongodb database used size");
        long useplace = 0;
        try {
            String host = ParamQuery.getCFProperties().get(ParamQuery.MONGO_HOST);
            String port = ParamQuery.getCFProperties().get(ParamQuery.MONGO_PORT);
            String authUser = ParamQuery.getCFProperties().get(ParamQuery.MONGO_AUTH_USER);
            String authPassWd = ParamQuery.getCFProperties().get(ParamQuery.MONGO_AUTH_PASSWD);
            String authDbname = ParamQuery.getCFProperties().get(ParamQuery.MONGO_AUTH_DBNAME);
            MongoClient mongoClient = null;
            //IP port
            ServerAddress addr = new ServerAddress(host, Integer.valueOf(port));
            List<ServerAddress> seeds = new ArrayList<ServerAddress>();
            seeds.add(addr);

            // 用户名 数据库 密码
            MongoCredential credential = MongoCredential.createCredential(authUser, authDbname, authPassWd.toCharArray());
            List<MongoCredential> credentialList = new ArrayList<MongoCredential>();
            credentialList.add(credential);

            mongoClient = new MongoClient(seeds,credentialList);

            MongoDatabase database = mongoClient.getDatabase(databasename);

            if(database!=null && database.listCollections()!=null){
                for(Document colloctio:database.listCollections()){
                    useplace+=colloctio.toJson().getBytes().length;
                }
            }
            String used = String.valueOf(useplace);
            volumSize= new Quota("volumeSize","",used+"(B)","","mongodb database used size");
        }catch (Exception e){
            logger.info("quotaQuery getMongoQuota Exception "+e.getMessage());
            volumSize= new Quota("volumeSize","","-1","","mongodb database used size");
            items.add(volumSize);
            result.put("items", items);
            return result;
        }
        items.add(volumSize);
        result.put("items",items);
        return result;
    }
}
