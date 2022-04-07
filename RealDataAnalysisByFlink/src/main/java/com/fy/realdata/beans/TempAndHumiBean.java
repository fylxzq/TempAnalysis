package com.fy.realdata.beans;

/**
 * @Classname TempAndHumiBean
 * @Description TODO
 * @Date 2022/3/30 10:02
 * @Created by fy
 */
public class TempAndHumiBean {
    private String deviceId;
    private String datasetId;
    private Long timestamp;
    private double value;

    public TempAndHumiBean() {
    }

    public TempAndHumiBean(String deviceId, String datasetId, Long timestamp, double value) {
        this.deviceId = deviceId;
        this.datasetId = datasetId;
        this.timestamp = timestamp;
        this.value = value;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "TempAndHumiBean{" +
                "deviceId='" + deviceId + '\'' +
                ", datasetId='" + datasetId + '\'' +
                ", timestamp=" + timestamp +
                ", value=" + value +
                '}';
    }
}
