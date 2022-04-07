package com.fy.realdata.analysis;

import com.fy.realdata.beans.GlobalConfig;
import com.fy.realdata.beans.TempAndHumiBean;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerConfig;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.Random;

/**
 * @Classname TempAndHumiDistributed
 * @Description TODO
 * @Date 2022/3/30 10:05
 * @Created by fy
 */
public class TempAndHumiDistributed {
    public static void main(String[] args) throws Exception {
        //创建环境并配置相关参数
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        //设置Kafka Consumer配置信息
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.1.101:9092,192.168.1.102:9092,192.168.1.103:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "mytemp");

        //读取Kafka数据源
        FlinkKafkaConsumer<String> myConsumer = new FlinkKafkaConsumer<String>("tmpctwing", new SimpleStringSchema(), props);
        DataStreamSource<String> inputStream = env.addSource(myConsumer);

        Random random = new Random();
        //转化为POJO
        SingleOutputStreamOperator<TempAndHumiBean> pojoStream = inputStream.filter(line -> (!line.contains("AAIA") && !line.contains("AQQA")))
                .filter(line -> (line.contains("temperature_data") || line.contains("humidity_data")))//过滤掉非温湿度的数据
                .map(line -> {
                    String[] split = line.split(",");
                    return new TempAndHumiBean(split[0], split[1], new Long(split[2]), new Double(split[3]));
                })
                .filter(data -> "temperature_data".equals(data.getDatasetId()))//过滤湿度数据
                .map(obj -> {
                    double random_value = random.nextGaussian() ;
                    obj.setValue(random_value+ obj.getValue());
                    return obj;
                }).assignTimestampsAndWatermarks(new AscendingTimestampExtractor<TempAndHumiBean>() {
                    @Override
                    public long extractAscendingTimestamp(TempAndHumiBean tempAndHumiBean) {
                        return tempAndHumiBean.getTimestamp();
                    }
                });

        SingleOutputStreamOperator<Tuple3<String, Double, Long>> resuleStream = pojoStream.flatMap(new TempAndHumiFlatMap()).keyBy(1).sum(2);
        resuleStream.addSink(new MysqlSink());
        resuleStream.print();
        env.execute();

    }


    public static class TempAndHumiFlatMap implements FlatMapFunction<TempAndHumiBean,Tuple3<String,Double,Long>>{
        private final double[] arr = new double[20];
        public TempAndHumiFlatMap() {
            //初始化温度区间
            for(int i = 0;i < 20; ++i){
                arr[i] = 25.5 + i * 0.5;
            }
        }

        @Override
        public void flatMap(TempAndHumiBean tempAndHumiBean, Collector<Tuple3<String, Double, Long>> collector) throws Exception {
            double value = tempAndHumiBean.getValue();
            double interval = calculateInterval(value);
            collector.collect(new Tuple3<>(tempAndHumiBean.getDatasetId(),interval,1L));
        }

        //计算温度所属的区间
        private double calculateInterval(double value){
            for(int i = 0; i < arr.length; i++){
                if(arr[i] >= value){
                    return arr[i];
                }
            }
            //前面没有返回说明温度值大于最大值，那么返回最大值的区间
            return arr[arr.length - 1];
        }
    }

    public static class MysqlSink extends RichSinkFunction<Tuple3<String,Double,Long>> {
        private Connection conn = null;
        private PreparedStatement preparedStatement;
        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            Class.forName(GlobalConfig.DRIVER_CLASS);
            conn = DriverManager.getConnection(GlobalConfig.DB_URL, GlobalConfig.USER_NAME, GlobalConfig.PASSWORD);
        }

        @Override
        public void close() throws Exception {
            super.close();
            if(preparedStatement != null){
                preparedStatement.close();
            }
            if(conn != null){
                conn.close();
            }
            super.close();
        }

        @Override
        public void invoke(Tuple3<String, Double,Long> value, Context context) throws Exception {
            try {
                double interval = value.f1;
                long count = value.f2;
                String querySQL = "SELECT intervalcount FROM tempintervalcount where tempinterval = " + interval;
                System.out.println(querySQL);
                String updateSQL = "update tempintervalcount set intervalcount = " + count + " where tempinterval = " + interval;
                System.out.println(updateSQL);
                String insertSQL = "insert into tempintervalcount(tempinterval,intervalcount) values(" + interval +"," + count +")";
                System.out.println(insertSQL);
                preparedStatement = conn.prepareStatement(querySQL);
                ResultSet resultSet = preparedStatement.executeQuery();
                if(resultSet.next()){
                    preparedStatement.executeUpdate(updateSQL);
                }
                else{
                    preparedStatement.execute(insertSQL);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
