package com.fy.DataCollectionsUtils;


public class ContentInfo {
	
	private String value;
	private String timestamp;
	private String datasetId;
	private String deviceID;
	
	public ContentInfo() {}
	public ContentInfo(String value, String timestamp, String datasetId,String deviceID) {
		this.value = value;
		this.timestamp = timestamp;
		this.datasetId = datasetId;
		this.deviceID = deviceID;
	}

	public String getDeviceID() {
		return deviceID;
	}

	public void setDeviceID(String deviceID) {
		this.deviceID = deviceID;
	}

	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	
	public String getDatasetId() {
		return datasetId;
	}
	
	public void setDatasetId(String datasetId) {
		this.datasetId = datasetId;
	}
	
	@Override
	public String toString() {
		return deviceID + "," + datasetId + "," + timestamp + "," + value;
	}
}
