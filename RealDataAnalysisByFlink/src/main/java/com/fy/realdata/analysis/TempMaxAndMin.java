package com.fy.realdata.analysis;

import com.fy.realdata.beans.GlobalConfig;
import com.fy.realdata.beans.TempAndHumiBean;
import com.fy.realdata.beans.TempAndHumiMaxBean;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.java.tuple.Tuple2;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;


import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * @Classname TempMaxAndMin
 * @Description TODO
 * @Date 2022/3/31 11:25
 * @Created by fy
 */
public class TempMaxAndMin {
    public static void main(String[] args) throws Exception {
        //创建环境并配置相关参数
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        Random random = new Random();
        //读取数据源
        URL url = TempMaxAndMin.class.getResource("/test.txt");
        DataStreamSource<String> inputStream = env.readTextFile(url.getPath());
        //转化为POJO
        SingleOutputStreamOperator<TempAndHumiBean> pojoStream = inputStream.filter(line -> (!line.contains("AAIA") && !line.contains("AQQA")))
                .filter(line -> (line.contains("temperature_data") || line.contains("humidity_data")))//过滤掉非温湿度的数据
                .map(line -> {
                    String[] split = line.split(",");
                    return new TempAndHumiBean(split[0], split[1], new Long(split[2]), new Double(split[3]));
                })
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

        //分组开窗统计温湿度数据的最大值
        DataStream<TempAndHumiMaxBean> resultStream = pojoStream.keyBy(TempAndHumiBean::getDatasetId)
                .timeWindow(Time.minutes(1),Time.seconds(10))
                .aggregate(new TempAndHumiMaxAgg(),new TempAndHumiMaxWindow());//采用增量聚合的方式
        resultStream.addSink(new TempAndHumiMaxMysqlSink());
        resultStream.print();
        env.execute();
    }

    public static class TempAndHumiMaxAgg implements AggregateFunction<TempAndHumiBean,Double, Tuple2<String,Double>> {
        private String dataSetId = "unknown";
        @Override
        public Double createAccumulator() {
            return Double.MIN_VALUE;
        }

        @Override
        public Double add(TempAndHumiBean tempAndHumiBean, Double aDouble) {
            dataSetId = tempAndHumiBean.getDatasetId();
            return Math.max(aDouble, tempAndHumiBean.getValue());
        }

        @Override
        public Tuple2<String, Double> getResult(Double aDouble) {
            return new Tuple2<>(dataSetId,aDouble);
        }

        @Override
        public Double merge(Double aDouble, Double acc1) {
            return Math.max(aDouble, acc1);
        }
    }

    public static class TempAndHumiMaxWindow implements WindowFunction<Tuple2<String,Double>, TempAndHumiMaxBean, String, TimeWindow> {
        @Override
        public void apply(String s, TimeWindow timeWindow, Iterable<Tuple2<String,Double>> iterable, Collector<TempAndHumiMaxBean> collector) throws Exception {
            Tuple2<String,Double> value = iterable.iterator().next();
            collector.collect(new TempAndHumiMaxBean(timeWindow.getStart(), timeWindow.getEnd(), value.f0, value.f1));
        }
    }

    public static class TempAndHumiMaxMysqlSink extends RichSinkFunction<TempAndHumiMaxBean> {
        private Connection conn = null;
        private PreparedStatement preparedStatement;
        private SimpleDateFormat sf;
        @Override
        public void open(Configuration parameters) throws Exception {
            sf = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss");
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
        public void invoke(TempAndHumiMaxBean value, Context context) throws Exception {
            try {
                String datasetid = value.getDataSetId();
                String date = sf.format(new Date(value.getWinStart()));
                String insertSQL = "insert into temphumimax(datasetid,winstart,value) values('"+datasetid+"'" + ", '"+date+"'" + "," + value.getMaxValue() + ")";
                System.out.println(insertSQL);
                preparedStatement = conn.prepareStatement(insertSQL);
                preparedStatement.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
