package com.fy.realdata.beans;

/**
 * @Classname TempAndHumiMaxBean
 * @Description TODO
 * @Date 2022/3/31 15:22
 * @Created by fy
 */
public class TempAndHumiMaxBean {
    private long winStart;
    private long winEnd;
    private String dataSetId;
    private Double maxValue;
    public TempAndHumiMaxBean() {
    }

    public TempAndHumiMaxBean(long winStart, long winEnd, String dataSetId, Double maxValue) {
        this.winStart = winStart;
        this.winEnd = winEnd;
        this.dataSetId = dataSetId;
        this.maxValue = maxValue;
    }

    public long getWinStart() {
        return winStart;
    }

    public void setWinStart(long winStart) {
        this.winStart = winStart;
    }

    public long getWinEnd() {
        return winEnd;
    }

    public void setWinEnd(long winEnd) {
        this.winEnd = winEnd;
    }

    public String getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(String dataSetId) {
        this.dataSetId = dataSetId;
    }

    public Double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Double maxValue) {
        this.maxValue = maxValue;
    }

    @Override
    public String toString() {
        return "TempAndHumiMaxBean{" +
                "winStart=" + winStart +
                ", winEnd=" + winEnd +
                ", dataSetId='" + dataSetId + '\'' +
                ", maxValue=" + maxValue +
                '}';
    }
}
