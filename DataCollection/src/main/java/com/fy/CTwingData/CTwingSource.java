package com.fy.CTwingData;

import com.ctg.ag.sdk.biz.AepDeviceStatusClient;
import com.ctg.ag.sdk.biz.aep_device_status.QueryDeviceStatusListRequest;
import com.ctg.ag.sdk.biz.aep_device_status.QueryDeviceStatusListResponse;
import com.fy.DataCollectionsUtils.ContentInfo;
import com.fy.DataCollectionsUtils.DeviceConfig;
import com.fy.DataCollectionsUtils.ParsedRespond;
import com.fy.DataCollectionsUtils.RespondParser;
import org.apache.flume.Context;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.PollableSource;
import org.apache.flume.conf.Configurable;
import org.apache.flume.event.SimpleEvent;
import org.apache.flume.source.AbstractSource;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @Classname CTwingSource
 * @Description TODO
 * @Date 2022/3/27 21:49
 * @Created by fy
 */
public class CTwingSource extends AbstractSource implements Configurable, PollableSource {
    private static final Logger logger = LoggerFactory.getLogger(CTwingSource.class);

    private DataCollector collector;
    @Override
    public void configure(Context context) {
        try {
            collector = new DataCollector(context);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    @Override
    public Status process() throws EventDeliveryException {
        HashMap<String,String> header = new HashMap<>();
        SimpleEvent event = new SimpleEvent();
        while (true){
            try {
                List<ContentInfo> statusList = collector.getInfos();
                if(statusList != null){
                    //将响应对象转化为字符串，然后用Json转化为Java对象
                    for(ContentInfo e : statusList){
                        //设置头信息
                        e.setDeviceID(DeviceConfig.deviceID);
                        header.put("datasetId", e.getDatasetId());
                        event.setHeaders(header);
                        event.setBody(e.toString().getBytes(StandardCharsets.UTF_8));
                        getChannelProcessor().processEvent(event);
                        //清空头信息
                        header.clear();
                    }
                }
                Thread.sleep(1000);
        } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public long getBackOffSleepIncrement() {
        return 0;
    }

    @Override
    public long getMaxBackOffSleepInterval() {
        return 0;
    }


}
