package com.fy.realdata.analysis;

import com.fy.realdata.beans.TempAndHumiBean;
import com.fy.realdata.beans.TempAndHumiMaxBean;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.util.Properties;
import java.util.Random;

/**
 * @Classname ProjectOneStream
 * @Description TODO
 * @Date 2022/3/31 19:29
 * @Created by fy
 */
public class ProjectOneStream {
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
                .assignTimestampsAndWatermarks(new AscendingTimestampExtractor<TempAndHumiBean>() {
                    @Override
                    public long extractAscendingTimestamp(TempAndHumiBean tempAndHumiBean) {
                        return tempAndHumiBean.getTimestamp();
                    }
                });
        SingleOutputStreamOperator<TempAndHumiBean> distributedStream = pojoStream.filter(data -> "temperature_data".equals(data.getDatasetId()))
                .map(obj -> {
                    double random_value = random.nextGaussian() ;
                    obj.setValue(random_value+ obj.getValue());
                    return obj;
                });//过滤湿度数据
        //统计温度区间分布情况
        SingleOutputStreamOperator<Tuple3<String, Double, Long>> distributedResuleStream = distributedStream.flatMap(new TempAndHumiDistributed.TempAndHumiFlatMap()).keyBy(1).sum(2);
        distributedResuleStream.addSink(new TempAndHumiDistributed.MysqlSink());


        //分组开窗统计温湿度数据的最大值
        DataStream<TempAndHumiMaxBean> THMaxResultStream = pojoStream
                .map(obj -> {
                    double random_value = random.nextDouble();
                    obj.setValue(random_value+ obj.getValue());
                    return obj;
                }).keyBy(TempAndHumiBean::getDatasetId)
                .timeWindow(Time.minutes(1),Time.seconds(10))
                .aggregate(new TempMaxAndMin.TempAndHumiMaxAgg(),new TempMaxAndMin.TempAndHumiMaxWindow());//采用增量聚合的方式
        THMaxResultStream.addSink(new TempMaxAndMin.TempAndHumiMaxMysqlSink());



        //使用状态编程检测温度持续上升的次数大于阈值的开始上升时间
        SingleOutputStreamOperator<Tuple3<String, Long, Long>> TCotinueIncrease = pojoStream.filter(data -> "temperature_data".equals(data.getDatasetId()))
                .map(obj -> {
                    double random_value = random.nextDouble();
                    obj.setValue(random_value + obj.getValue());
                    return obj;
                }).keyBy("deviceId")
                .process(new TempContinueIncrease.TempIncreaseCount(3L));
        TCotinueIncrease.addSink(new TempContinueIncrease.TempIncreaseMysqlSink());

        env.execute();
    }
}
