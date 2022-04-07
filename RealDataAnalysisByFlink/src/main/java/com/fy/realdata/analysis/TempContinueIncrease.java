package com.fy.realdata.analysis;

import com.fy.realdata.beans.GlobalConfig;
import com.fy.realdata.beans.TempAndHumiBean;
import com.fy.realdata.beans.TempAndHumiMaxBean;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;
import org.apache.flink.util.Collector;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Random;

/**
 * @Classname TempContinueIncrease
 * @Description TODO
 * @Date 2022/4/1 9:39
 * @Created by fy
 */


public class TempContinueIncrease {
    public static void main(String[] args) throws Exception {
        //创建环境并配置相关参数
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
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
                .filter(data -> "temperature_data".equals(data.getDatasetId()))
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

        SingleOutputStreamOperator<Tuple3<String, Long, Long>> resultStream = pojoStream.keyBy("deviceId")
                .process(new TempIncreaseCount(3L));
        resultStream.print();
        resultStream.addSink(new TempIncreaseMysqlSink());


        env.execute();
    }


    public static class TempIncreaseCount extends KeyedProcessFunction<Tuple,TempAndHumiBean, Tuple3<String,Long,Long>> {

        private ValueState<Long> startTsState;//记录温度开始上升时的时间
        private ValueState<Double> prevalueState;//记录前一条记录的温度值
        private ValueState<Long> countState;//记录当前温度已经连续上升了几次
        private Long limit;

        public TempIncreaseCount(Long limit) {
            this.limit = limit;
        }



        @Override
        public void open(Configuration parameters) throws Exception {
            startTsState = getRuntimeContext().getState(new ValueStateDescriptor<Long>("startTs", Long.class));
            prevalueState = getRuntimeContext().getState(new ValueStateDescriptor<Double>("prevalue", Double.class));
            countState = getRuntimeContext().getState(new ValueStateDescriptor<Long>("count", Long.class));

        }

        @Override
        public void processElement(TempAndHumiBean value, Context ctx, Collector<Tuple3<String,Long,Long>> out) throws Exception {
            Double prevalue = prevalueState.value();
            Long count = countState.value();
            if (prevalue == null || prevalue >= value.getValue()){
                //来一条数据时，或者当前记录的温度值小于
                startTsState.update(value.getTimestamp());
                prevalueState.update(value.getValue());
                countState.update(1L);
            }
            else{
                prevalueState.update(value.getValue());
                countState.update(count + 1);
            }
            if(countState.value() >= limit){
                out.collect(new Tuple3<>(value.getDeviceId(),startTsState.value(),countState.value()));
            }
        }
    }

    public static class TempIncreaseMysqlSink extends RichSinkFunction<Tuple3<String, Long, Long>> {
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
        public void invoke(Tuple3<String, Long, Long> value, Context context) throws Exception {
            try {
                String deviceId = value.f0;
                String date = sf.format(new Date(value.f1));
                String replaceSQL = "replace into tempincrease(deviceid,starttime,counts) values('"+deviceId+"'" + ", '"+date+"'" + "," + value.f2 + ")";
                //System.out.println(replaceSQL);
                preparedStatement = conn.prepareStatement(replaceSQL);
                preparedStatement.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
