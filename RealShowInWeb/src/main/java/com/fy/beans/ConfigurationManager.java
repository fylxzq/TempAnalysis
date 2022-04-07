package com.fy.beans;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @Classname ConfigurationManager
 * @Description TODO
 * @Date 2022/3/14 9:40
 * @Created by fy
 */
public class ConfigurationManager {
    private static Properties prop = new Properties();
    static {
        InputStream in = ConfigurationManager.class.getClassLoader().getResourceAsStream("my.properties");
        try {
            prop.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static String getProperty(String key){
        return prop.getProperty(key);
    }
    public static int getInteger(String key){
        return Integer.parseInt(prop.getProperty(key));
    }
    public static Long getLong(String key){
        return Long.parseLong(getProperty(key));
    }
}
