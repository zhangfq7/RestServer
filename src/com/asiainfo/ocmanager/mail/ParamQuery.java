package com.asiainfo.ocmanager.mail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

/**
 * Created by Allen on 2017/7/3.
 * 获取Token认证参数
 */
public class ParamQuery {

    public final static String SEND_MAIL_USERNAME = "send.mail.username";
    public final static String SEND_MAIL_PASSWORD = "send.mail.password";
    public final static String GREENPLUM_HOST_PORT = "greenplum.host.port";
    public final static String MONGO_HOST = "mongodb.host";
    public final static String MONGO_PORT = "mongodb.port";
    public final static String MONGO_AUTH_USER = "mongodb.auth.user";
    public final static String MONGO_AUTH_PASSWD = "mongodb.auth.passwd";
    public final static String MONGO_AUTH_DBNAME = "mongodb.auth.dbname";
    public final static String DACP_JAVA_SECURITY_KRB5_REALM = "dacp.java.security.krb5.realm";
    public final static String DACP_JAVA_SECURITY_KRB5_KDC = "dacp.java.security.krb5.kdc";
    public final static String DACP_HADOOP_SECURITY_AUTHENTICATION = "dacp.hadoop.security.authentication";
    public final static String DACP_KEYTAB_FILE = "dacp.keytab.file";
    public final static String DACP_KERBEROS_PRINCIPAL = "dacp.kerberos.principal";
    public final static String BOOTSTRAP_SERVERS = "bootstrap.servers";



    private static HashMap<String, String> map = new HashMap<String, String>();

    public static HashMap<String, String> getCFProperties() throws IOException {
        if (map.size() == 0) {
            synchronized (ParamQuery.class) {
                if (map.size() == 0) {
                    String classPath = new ParamQuery().getClass().getResource("/").getPath();
                    String currentClassesPath = classPath.substring(0, classPath.length() - 8)+ "conf/config.properties";
                    System.out.println(currentClassesPath);
                    InputStream inputStream = null;
                    try {
                        inputStream = new FileInputStream(new File(currentClassesPath ));
                    } catch (IOException e) {
                        System.out.println("loadProperties IOException:" + e.getMessage());
                    } finally {
                        if (inputStream != null) {
                            try {
                                Properties prop = new Properties();
                                prop.load(inputStream);
                                map.put(SEND_MAIL_USERNAME, prop.getProperty(SEND_MAIL_USERNAME));
                                map.put(SEND_MAIL_PASSWORD, prop.getProperty(SEND_MAIL_PASSWORD));
                                map.put(MONGO_HOST,prop.getProperty(MONGO_HOST));
                                map.put(MONGO_PORT,prop.getProperty(MONGO_PORT));
                                map.put(MONGO_AUTH_USER,prop.getProperty(MONGO_AUTH_USER));
                                map.put(MONGO_AUTH_PASSWD,prop.getProperty(MONGO_AUTH_PASSWD));
                                map.put(MONGO_AUTH_DBNAME,prop.getProperty(MONGO_AUTH_DBNAME));
                                map.put(GREENPLUM_HOST_PORT,prop.getProperty(GREENPLUM_HOST_PORT));
                                map.put(BOOTSTRAP_SERVERS,prop.getProperty(BOOTSTRAP_SERVERS));
                                map.put(DACP_HADOOP_SECURITY_AUTHENTICATION,DACP_HADOOP_SECURITY_AUTHENTICATION+"="+prop.getProperty(DACP_HADOOP_SECURITY_AUTHENTICATION));
                                map.put(DACP_JAVA_SECURITY_KRB5_KDC,DACP_JAVA_SECURITY_KRB5_KDC+"="+prop.getProperty(DACP_JAVA_SECURITY_KRB5_KDC));
                                map.put(DACP_JAVA_SECURITY_KRB5_REALM,DACP_JAVA_SECURITY_KRB5_REALM+"="+prop.getProperty(DACP_JAVA_SECURITY_KRB5_REALM));
                                map.put(DACP_KERBEROS_PRINCIPAL,DACP_KERBEROS_PRINCIPAL+"="+prop.getProperty(DACP_KERBEROS_PRINCIPAL));
                                map.put(DACP_KEYTAB_FILE,DACP_KEYTAB_FILE+"="+prop.getProperty(DACP_KEYTAB_FILE));
                                prop.load(inputStream);
                                inputStream.close(); // 关闭流
                            } catch (IOException e) {
                                System.out.println("inputStream close IOException:" + e.getMessage());
                            }
                        }
                    }
                }



            }
        }
//        System.out.println(map);
        return map;
    }
}
