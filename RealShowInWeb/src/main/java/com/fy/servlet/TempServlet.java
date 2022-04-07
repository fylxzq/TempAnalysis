package com.fy.servlet;

import com.fy.beans.JDBCHelper;
import net.sf.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @Classname TempServlet
 * @Description TODO
 * @Date 2022/3/30 21:33
 * @Created by fy
 */
public class TempServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    JDBCHelper jdbcHelper = JDBCHelper.getInstance();
    ResultSet rs = null;
    public TempServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String,Object> tempIntervalCount = getIntervalTemp();
        Map<String,Object> tempMax = getTHmaxWithTime("temperature_data");
        Map<String,Object> humiMax = getTHmaxWithTime("humidity_data");
        Map<String,Object> tempIncrease = getTempIncrease();
        Map<String,Object> map = new HashMap<>();

        for(Map.Entry<String,Object> entry: tempIncrease.entrySet()){
            map.put("starttime",entry.getKey());
            map.put("counts",entry.getValue());
        }
        map.put("tempinterval",tempIntervalCount.get("tempinterval"));
        map.put("intervalcount",tempIntervalCount.get("intervalcount"));
        map.put("tempwinstart", tempMax.get("winstart"));
        map.put("tempvalues", tempMax.get("values"));
        map.put("humiwinstart", humiMax.get("winstart"));
        map.put("humivalues", humiMax.get("values"));
        map.put("newssum", 3);
        resp.setContentType("text/html;charset=utf-8");
        PrintWriter pw = resp.getWriter();
        pw.write(JSONObject.fromObject(map).toString());
        pw.flush();;
        pw.close();
    }

    public Map<String, Object> getTHmaxWithTime(String datasetid) {
        Map<String ,Object> map = new HashMap<>();
        String[] winstarts = new String[5];
        Double[] values = new Double[5];
        String sql = "select winstart,value from temphumimax where datasetid = '"+datasetid+"'" +" order by winstart desc limit 5";
        try {
            ResultSet rs = getResultSet(sql);
            int i = 0;
            while (rs.next()){
                String winstart = rs.getString(1);
                System.out.println(winstart);
                Double value = rs.getDouble(2);
                winstarts[i] = winstart;
                values[i] = value;
                i++;
            }
            map.put("winstart", winstarts);
            map.put("values", values);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public  Map<String,Object> getIntervalTemp(){
        Map<String,Object> map = new HashMap<>();
        Double[] intervals = new Double[20];
        Integer[] counts = new Integer[20];
        String sql = "select tempinterval,intervalcount from tempintervalcount order by tempinterval";
        try {
            ResultSet rs = getResultSet(sql);
            int i = 0;
            System.out.println(rs);
            while (rs.next()){
                Double interval = rs.getDouble("tempinterval");
                Integer count = rs.getInt("intervalcount");
                intervals[i] = interval;
                counts[i] = count;
                i++;
            }
            map.put("tempinterval", intervals);
            map.put("intervalcount",counts);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public Map<String,Object> getTempIncrease(){
        Map<String,Object> map = new HashMap<>();
        String sql = "select starttime,counts from tempincrease order by starttime desc limit 1";
        String key = null;
        Integer val = null;
        try {
            ResultSet rs = getResultSet(sql);
            int i = 0;
            System.out.println(rs);
            while (rs.next()){
              key =  rs.getString(1);
              val = rs.getInt(2);
            }
            map.put(key, val);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public ResultSet getResultSet(String sql) throws Exception{
        jdbcHelper.excuteQuery(sql, null, new JDBCHelper.QueryCallback() {
            @Override
            public void process(ResultSet rs1) throws Exception {
                rs = rs1;
            }
        });
        return rs;
    }
}
