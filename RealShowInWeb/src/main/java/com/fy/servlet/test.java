package com.fy.servlet;

/**
 * @Classname test
 * @Description TODO
 * @Date 2022/3/31 20:35
 * @Created by fy
 */
public class test {
    public static void main(String[] args) {
        String s = "123";
        String sql = "select winstart,value from temphumimax where datasetid = '"+s+"'" +" order by winstart desc limit 10";
        System.out.println(sql);
    }
}
