package com.fy.beans;


import java.sql.*;
import java.util.LinkedList;

/**
 * @Classname JDBCHelper
 * @Description TODO
 * @Date 2022/3/14 9:47
 * @Created by fy
 */
public class JDBCHelper {
    static {
        try {
            ConfigurationManager.getProperty(ConfigurationManager.getProperty(Constants.JDBC_DRIVER));
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    private static JDBCHelper instance = null;

    //懒汉式实现单例
    public static JDBCHelper getInstance(){
        if(instance == null){
            synchronized (JDBCHelper.class){
                if(instance == null){
                    instance = new JDBCHelper();
                }
            }
        }
        return instance;
    }
    //创建数据库连接池
    private LinkedList<Connection> datasources = new LinkedList<>();
    private JDBCHelper(){
        int size = ConfigurationManager.getInteger(Constants.JDBC_DATASOURCE_SIZE);
        for(int i = 0; i < size; i++){
            String url = ConfigurationManager.getProperty(Constants.JDBC_URL);
            String user = ConfigurationManager.getProperty(Constants.JDBC_USER);
            String password = ConfigurationManager.getProperty(Constants.JDBC_PASSWORD);
            try {
                Connection conn = DriverManager.getConnection(url, user, password);
                datasources.add(conn);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    };
    //提供获取数据库连接
    public synchronized Connection getConnection(){
        while (datasources.size() == 0){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return datasources.poll();
    }
    //执行sql查询语句
    public synchronized void excuteQuery(String sql,Object[] params,QueryCallback callback) throws Exception{
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            if(params != null){
                for(int i = 0; i < params.length; i++){
                    pstmt.setObject(i + 1, params[i]);
                }
            }
            rs = pstmt.executeQuery();
            callback.process(rs);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(conn != null){
                datasources.push(conn);
            }
        }
    }
    public static interface QueryCallback{
        void process(ResultSet rs) throws Exception;
    }
}
