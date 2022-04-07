package com.fy.CTwingData;

import com.ctg.ag.sdk.biz.AepDeviceStatusClient;
import com.ctg.ag.sdk.biz.aep_device_status.QueryDeviceStatusListRequest;
import com.ctg.ag.sdk.biz.aep_device_status.QueryDeviceStatusListResponse;
import com.fy.DataCollectionsUtils.ContentInfo;
import com.fy.DataCollectionsUtils.DeviceConfig;
import com.fy.DataCollectionsUtils.ParsedRespond;
import com.fy.DataCollectionsUtils.RespondParser;
import org.apache.flume.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;


/**
 * @Classname ColletData
 * @Description TODO
 * @Date 2022/3/27 20:04
 * @Created by fy
 */
public class DataCollector {
    //定义天翼平台的连接和请求
    private AepDeviceStatusClient client = null;
    private QueryDeviceStatusListRequest request;
    private BufferedWriter bw = null;
    private static final Logger logger = LoggerFactory.getLogger(CTwingSource.class);

    public static void main(String[] args) throws Exception {
        DataCollector collector = new DataCollector();
        collector.CreateConnectAndRequest();
        collector.CreateIoBW(args[0]);
        while (true){
           collector.WriteData(collector);
           Thread.sleep(2000L);
        }
    }

    private void WriteData(DataCollector collector) throws Exception {
        //写入数据到指定文件
        List<ContentInfo> infos = collector.getInfos();
        for (ContentInfo info : infos){
            //数据中添加设备ID
            info.setDeviceID(DeviceConfig.deviceID);
            System.out.println(info.toString());
            bw.write(info.toString());
            bw.newLine();
        }
        bw.flush();
    }

    //创建输出流
    private void CreateIoBW(String path) throws FileNotFoundException {
        if(path == null){
            path = "/home/hadoop/data/Ctwing/test.txt";
        }
        FileOutputStream fos = new FileOutputStream(path);
        bw = new BufferedWriter(new OutputStreamWriter(fos));
    }

    public DataCollector() {
    }

    public DataCollector(Context context) throws UnsupportedEncodingException {
        DeviceConfig.productID = context.getString("productID");
        DeviceConfig.deviceID = context.getString("deviceID");
        DeviceConfig.appKey = context.getString("appKey");
        DeviceConfig.appSecret = context.getString("appSecret");
    }

    private void CreateConnectAndRequest() throws UnsupportedEncodingException {
        //创建连接天翼客户端的连接和请求
        client = AepDeviceStatusClient.newClient().appKey(DeviceConfig.appKey).appSecret(DeviceConfig.appSecret).build();
        request = new QueryDeviceStatusListRequest();
        String json_string = "{\"productId\":\""+DeviceConfig.productID+"\",\"deviceId\":\"" + DeviceConfig.deviceID + "\"}";
        System.out.println(json_string);
        byte[] json_byte = json_string.getBytes("UTF-8");
        request.setBody(json_byte);
    }

    public List<ContentInfo> getInfos() throws Exception {
        List<ContentInfo> statusList = null;
        QueryDeviceStatusListResponse response = client.QueryDeviceStatusList(request);
        if(response.getStatusCode() == 200) {
            //将响应对象转化为字符串，然后用Json转化为Java对象
            String respond_str = response.toString();
            ParsedRespond parsed_respond = RespondParser.parse(respond_str);
            statusList = parsed_respond.getDeviceStatusList();
        }
        return statusList;
    }


}
